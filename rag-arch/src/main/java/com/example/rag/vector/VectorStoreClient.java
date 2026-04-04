package com.example.rag.vector;

import com.example.rag.model.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class VectorStoreClient {

    private static final Logger log =
            LoggerFactory.getLogger(VectorStoreClient.class);

    private static final String COLLECTION = "rag-documents";
    private static final int BATCH_SIZE = 256;

    private final RestTemplate restTemplate = new RestTemplate();

    /** ✅ MUST match your embedding vector size */
    @Value("${embedding.vector-size}")
    private int embeddingVectorSize;

    @Value("${qdrant.url:http://localhost:6333}")
    private String qdrantUrl;

    /* ===================== COLLECTION INIT ===================== */

    private final Object collectionLock = new Object();
    private volatile boolean collectionReady = false;

    public void ensureCollectionExists() {

        if (collectionReady) return;

        synchronized (collectionLock) {

            if (collectionReady) return;

            String url = qdrantUrl + "/collections/" + COLLECTION;

            try {
                restTemplate.getForEntity(url, String.class);
                log.info("Qdrant collection '{}' already exists", COLLECTION);
                collectionReady = true;
                return;
            } catch (HttpClientErrorException.NotFound e) {
                log.info("Qdrant collection '{}' not found, creating it", COLLECTION);
            }

            Map<String, Object> body = Map.of(
                    "vectors", Map.of(
                            "size", embeddingVectorSize,
                            "distance", "Cosine"
                    )
            );

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(body, defaultHeaders());

            restTemplate.put(url, request);

            waitForCollection(url);

            collectionReady = true;
            log.info("Qdrant collection '{}' is ready", COLLECTION);
        }
    }

    /* ===================== UPSERT ===================== */

    public void upsert(List<DocumentChunk> chunks) {

        if (chunks == null || chunks.isEmpty()) {
            log.warn("No chunks to upsert");
            return;
        }

        log.info("Upserting {} chunks into Qdrant", chunks.size());

        for (int i = 0; i < chunks.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, chunks.size());
            List<DocumentChunk> batch = chunks.subList(i, end);
            upsertBatch(batch);
        }
    }

    private void upsertBatch(List<DocumentChunk> batch) {

        List<Map<String, Object>> points =
                batch.stream()
                        .map(this::toQdrantPoint)
                        .toList();

        Map<String, Object> body = Map.of("points", points);

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(body, defaultHeaders());

        String url = qdrantUrl +
                "/collections/" + COLLECTION +
                "/points?wait=true";

        restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
    }

    /* ===================== METADATA LOOKUP ===================== */

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

            Map<?, ?> result =
                    (Map<?, ?>) response.getBody().get("result");
            List<?> points =
                    (List<?>) result.get("points");

            if (points == null || points.isEmpty()) {
                return Optional.empty();
            }

            Map<?, ?> payload =
                    (Map<?, ?>) ((Map<?, ?>) points.get(0)).get("payload");

            Object value = payload.get(payloadField);
            return value == null ? Optional.empty() : Optional.of(value.toString());

        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty(); // ✅ safe bootstrap behavior
        } catch (Exception e) {
            log.warn("Payload lookup failed", e);
            return Optional.empty();
        }
    }

    /* ===================== SEARCH ===================== */

    public List<DocumentChunk> vectorSearch(float[] embedding) {

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
            log.error("Vector search failed", e);
            return List.of();
        }
    }

    public List<DocumentChunk> hybridSearch(
            float[] embedding,
            String queryText
    ) {
        List<DocumentChunk> vectorResults = vectorSearch(embedding);
//        List<DocumentChunk> bm25Results = bm25Search(queryText);
//        return mergeAndRerank(vectorResults, bm25Results);
        return null;
    }

    /* ===================== HELPERS ===================== */

    private Map<String, Object> toQdrantPoint(DocumentChunk chunk) {

        validateChunkId(chunk);

        Map<String, Object> payload = new HashMap<>();

        payload.put(
                "preview",
                chunk.getText().length() > 200
                        ? chunk.getText().substring(0, 200)
                        : chunk.getText()
        );

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
        UUID.fromString(chunk.getId());
    }

    private List<DocumentChunk> extractSearchResults(Map<String, Object> response) {

        if (response == null || !response.containsKey("result")) {
            return List.of();
        }

        List<Map<String, Object>> results =
                (List<Map<String, Object>>) response.get("result");

        List<DocumentChunk> chunks = new ArrayList<>();

        for (Map<String, Object> r : results) {

            Map<String, Object> payload =
                    (Map<String, Object>) r.get("payload");

            DocumentChunk chunk = new DocumentChunk();
            chunk.setId(String.valueOf(r.get("id")));
            chunk.setText((String) payload.get("text"));
            chunk.setContent((String) payload.get("text"));
            chunk.setMetadata(payload);

            chunks.add(chunk);
        }

        return chunks;
    }

    /* ===================== DELETE ===================== */

    public void deleteByFilter(String documentId) {

        Map<String, Object> body = Map.of(
                "filter", Map.of(
                        "must", List.of(
                                Map.of(
                                        "key", "document_id",
                                        "match", Map.of(
                                                "value", documentId
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
    }

    /* ===================== CORE UTIL ===================== */

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private void waitForCollection(String url) {

        for (int i = 0; i < 10; i++) {
            try {
                restTemplate.getForEntity(url, String.class);
                return;
            } catch (HttpClientErrorException.NotFound e) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ignored) {}
            }
        }

        throw new IllegalStateException(
                "Qdrant collection failed to become ready"
        );
    }
}
