package com.example.rag.service;

import com.example.rag.model.DocumentChunk;
import com.example.rag.vector.VectorStoreClient;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class RagServiceTest {

    private final EmbeddingClient embeddingClient = Mockito.mock(EmbeddingClient.class);
    private final VectorStoreClient vectorStoreClient = Mockito.mock(VectorStoreClient.class);
    private final RerankerClient rerankerClient = Mockito.mock(RerankerClient.class);

    private final RagService ragService =
            new RagService(embeddingClient, vectorStoreClient, rerankerClient);

    @Test
    void shouldRetrieveAndRerankContext() {

        Mockito.when(embeddingClient.embed(any()))
                .thenReturn(new float[]{0.1f, 0.2f});

        DocumentChunk chunk =
                new DocumentChunk("1", "test.pdf", "test text", new float[]{0.1f});

        Mockito.when(vectorStoreClient.hybridSearch(any(), any()))
                .thenReturn(List.of(chunk));

        Mockito.when(rerankerClient.rerank(any(), any()))
                .thenReturn(List.of(chunk));

        List<DocumentChunk> result = ragService.retrieveContext("test");

        assertEquals(1, result.size());
        assertEquals("test.pdf", result.get(0).getSource());
    }
}
