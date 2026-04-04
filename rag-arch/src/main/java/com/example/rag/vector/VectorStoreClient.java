package com.example.rag.vector;

import com.example.rag.model.DocumentChunk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class VectorStoreClient {

    private static final Logger log =
            LoggerFactory.getLogger(VectorStoreClient.class);

    private static final String COLLECTION = "rag-documents";
    private static final int BATCH_SIZE = 256;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${qdrant.url:http://localhost:6333}")
    private String qdrantUrl;

    /* ===================== UPSERT ===================== */

    public void upsert(List<DocumentChunk> chunks) {

        if (chunks == null || chunks.isEmpty()) {
            log.warn("No chunks to upsert");
            return;
        }

        log.info("Storing {} chunks into VectorDB..", chunks.size());

        for (int i = 0; i < chunks.size(); i += BATCH_SIZE) {

            int end = Math.min(i + BATCH_SIZE, chunks.size());
            List<DocumentChunk> batch = chunks.subList(i, end);

            try {
                upsertBatch(batch);
                log.debug("Upserted batch {} – {}", i, end);
            } catch (Exception e) {
                log.error("Failed to upsert batch {} – {}", i, end, e);
                throw new RuntimeException("Vector upsert failed", e);
            }
        }

        log.info("Successfully stored all {} chunks into VectorDB", chunks.size());
    }

    private void upsertBatch(List<DocumentChunk> batch) {

        List<Map<String, Object>> points = batch.stream()
                .map(this::toQdrantPoint)
                .toList();

        Map<String, Object> body = Map.of("points", points);

        HttpHeaders headers = defaultHeaders();
        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, headers);

        String url = qdrantUrl +
                "/collections/" + COLLECTION +
                "/points?wait=true";

        restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
    }

    /* ===================== CHECKSUM LOOKUP ===================== */

    /**
     * Finds any single payload field value for a document.
     * NEVER returns null.
     */
    public Optional<String> findAnyPayload(
            Map<String, Object> filter,
            String payloadField
    ) {
        try {
            Map<String, Object> body = Map.of(
                    "limit", 1,
                    "with_payload", true,
                    "filter", Map.of(
                            "must", List.of(
                                    Map.of(
                                            "key", "document_id",
                                            "match", Map.of(
                                                    "value", filter.get("document_id")
                                            )
                                    )
                            )
                    )
            );

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(body, defaultHeaders());

            String url = qdrantUrl +
                    "/collections/" + COLLECTION +
                    "/points/scroll";

            ResponseEntity<Map> response =
                    restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            Map<?, ?> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("result")) {
                return Optional.empty();
            }

            Map<?, ?> result = (Map<?, ?>) responseBody.get("result");
            List<?> points = (List<?>) result.get("points");

            if (points == null || points.isEmpty()) {
                return Optional.empty();
            }

            Map<?, ?> point = (Map<?, ?>) points.get(0);
            Map<?, ?> payload = (Map<?, ?>) point.get("payload");

            Object value = payload.get(payloadField);
            return value != null ? Optional.of(value.toString()) : Optional.empty();

        } catch (Exception ex) {
            log.warn("Payload lookup failed for filter {}", filter, ex);
            return Optional.empty();
        }
    }

    /* ===================== SEARCH ===================== */

    public List<DocumentChunk> hybridSearch(float[] embedding, String queryText) {

        try {
            Map<String, Object> body = Map.of(
                    "vector", embedding,
                    "limit", 5,
                    "with_payload", true
            );

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(body, defaultHeaders());

            String url = qdrantUrl +
                    "/collections/" + COLLECTION +
                    "/points/search";

            ResponseEntity<Map> response =
                    restTemplate.exchange(url, HttpMethod.POST, request, Map.class);

            return extractSearchResults(response.getBody());

        } catch (Exception e) {
            log.error("Hybrid search failed", e);
            return List.of();
        }
    }

    /* ===================== HELPERS ===================== */

    private Map<String, Object> toQdrantPoint(DocumentChunk chunk) {

        validateChunkId(chunk); // safety net

        Map<String, Object> payload = new HashMap<>();

        payload.put("text", chunk.getText());

        if (chunk.getMetadata() != null) {
            payload.putAll(chunk.getMetadata());
        }

        return Map.of(
                "id", chunk.getId(),
                "vector", chunk.getEmbedding(),
                "payload", payload
        );
    }

    private void validateChunkId(DocumentChunk chunk) {
        try {
            UUID.fromString(chunk.getId());
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid Qdrant point ID: " + chunk.getId() +
                            " (must be UUID or unsigned integer)"
            );
        }
    }

    private List<DocumentChunk> extractSearchResults(Map<String, Object> response) {

        if (response == null || !response.containsKey("result")) {
            return List.of();
        }

        List<Map<String, Object>> results =
                (List<Map<String, Object>>) response.get("result");

        return results.stream()
                .map(r -> {
                    Map<String, Object> payload =
                            (Map<String, Object>) r.get("payload");

                    DocumentChunk chunk = new DocumentChunk();
                    chunk.setId(String.valueOf(r.get("id")));
                    chunk.setContent((String) payload.get("text"));
                    chunk.setText((String) payload.get("text"));
                    chunk.setMetadata(payload);

                    return chunk;
                })
                .toList();
    }

    /* ===================== DELETE BY FILTER ===================== */

    public void deleteByFilter(Map<String, String> filter) {

        Map<String, Object> body = Map.of(
                "filter", Map.of(
                        "must", List.of(
                                Map.of(
                                        "key", "document_id",
                                        "match", Map.of(
                                                "value", filter.get("document_id")
                                        )
                                )
                        )
                )
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, defaultHeaders());

        String url = qdrantUrl +
                "/collections/" + COLLECTION +
                "/points/delete?wait=true";

        restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        log.info("Deleted vectors for document {}", filter.get("document_id"));
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}

