package com.example.rag.service;

import java.util.List;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.example.rag.model.DocumentChunk;
import com.example.rag.vector.VectorStoreClient;

@Service
public class RagService {
    Logger log = Logger.getLogger(RagService.class.getName());

    private final EmbeddingClient embeddingClient;
    private final VectorStoreClient vectorStore;
    private final RerankerClient reranker;

    public RagService(
            EmbeddingClient embeddingClient,
            VectorStoreClient vectorStore,
            RerankerClient reranker) {
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.reranker = reranker;
    }

    public List<DocumentChunk> retrieveContext(String question) {


        if (question == null || question.isBlank()) {
            log.info("Empty query received, skipping retrieval");
            return List.of();
        }


        float[] queryEmbedding = embeddingClient.embed(question);

        List<DocumentChunk> retrieved =
                vectorStore.hybridSearch(queryEmbedding, question);

        return reranker.rerank(question, retrieved);
    }

}
