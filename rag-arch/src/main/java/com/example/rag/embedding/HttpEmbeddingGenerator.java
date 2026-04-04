package com.example.rag.embedding;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.rag.config.EmbeddingProperties;
import com.example.rag.model.EmbeddingRequest;
import com.example.rag.model.EmbeddingResponse;

@Service
public class HttpEmbeddingGenerator implements IEmbeddingGenerator {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public HttpEmbeddingGenerator(EmbeddingProperties properties) {
        this.baseUrl = properties.getEndpoint();
        this.restTemplate = new RestTemplate();
    }

    @Override
    public float[] generateEmbedding(String text) {

        EmbeddingRequest request = new EmbeddingRequest(List.of(text));

        EmbeddingResponse response =
                restTemplate.postForObject(
                        baseUrl + "/embed",
                        request,
                        EmbeddingResponse.class
                );

        if (response == null || response.getEmbeddings().isEmpty()) {
            throw new IllegalStateException("Empty embedding response");
        }

        return response.getEmbeddings().get(0);
    }
}
