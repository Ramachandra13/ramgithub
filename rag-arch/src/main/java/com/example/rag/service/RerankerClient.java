package com.example.rag.service;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.example.rag.model.DocumentChunk;

@Component
public class RerankerClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public List<DocumentChunk> rerank(
            String query,
            List<DocumentChunk> chunks) {

        // Call cross-encoder reranker
        return chunks;
    }
}
