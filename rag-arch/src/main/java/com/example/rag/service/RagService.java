package com.example.rag.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.rag.model.DocumentChunk;
import com.example.rag.vector.VectorStoreClient;

@Service
public class RagService {

    private static final Logger log =
            LoggerFactory.getLogger(RagService.class);

    private final EmbeddingClient embeddingClient;
    private final VectorStoreClient vectorStore;
    private final RerankerClient reranker;

    public RagService(
            EmbeddingClient embeddingClient,
            VectorStoreClient vectorStore,
            RerankerClient reranker
    ) {
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.reranker = reranker;
    }

    public List<DocumentChunk> retrieveContext(String question) {

        if (question == null || question.isBlank()) {
            log.info("Empty query received, skipping retrieval");
            return List.of();
        }

        log.info("RagService:: Generating query embedding");

        float[] queryEmbedding =
                embeddingClient.embed(question);

        log.info("RagService:: Performing vector search");

        List<DocumentChunk> retrieved =
                vectorStore.vectorSearch(queryEmbedding);

        log.info("RagService:: Reranking {} retrieved chunks",
                retrieved.size());

        return reranker.rerank(question, retrieved);
    }
}
