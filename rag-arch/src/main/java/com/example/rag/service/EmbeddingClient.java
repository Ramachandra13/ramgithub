package com.example.rag.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.example.rag.model.EmbeddingRequest;
import com.example.rag.model.EmbeddingResponse;
import com.example.rag.vector.VectorStoreClient;

@Component
public class EmbeddingClient {

    private static final Logger log =
            LoggerFactory.getLogger(EmbeddingClient.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${embedding.service.url}")
    private String endpoint;


    public float[] embed(String text) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Embedding text must not be null or empty");
        }
        log.debug("Embedding text (length={} chars)", text.length());

        EmbeddingRequest request =
                new EmbeddingRequest(List.of(text));
        // Pending to configure a real embedding server (OpenAI / Azure / custom)
        EmbeddingResponse response =
                restTemplate.postForObject(
                        endpoint + "/embed",
                        request,
                        EmbeddingResponse.class
                );

        if (response == null || response.getEmbeddings().isEmpty()) {
            throw new IllegalStateException("Empty embedding response");
        }

        return response.getEmbeddings().get(0);
    }


}
