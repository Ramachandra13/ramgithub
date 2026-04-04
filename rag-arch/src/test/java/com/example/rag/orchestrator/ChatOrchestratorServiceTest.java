package com.example.rag.orchestrator;

import com.example.rag.model.ChatResponse;
import com.example.rag.model.DocumentChunk;
import com.example.rag.service.LlmClient;
import com.example.rag.service.RagService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class ChatOrchestratorServiceTest {

    private final RagService ragService = Mockito.mock(RagService.class);
    private final LlmClient llmClient = Mockito.mock(LlmClient.class);

    private final ChatOrchestratorService orchestrator =
            new ChatOrchestratorService(ragService, llmClient);

    @Test
    void shouldReturnAnswerWithSources() {

        DocumentChunk chunk =
                new DocumentChunk("1", "doc.pdf", "content", new float[]{});

        Mockito.when(ragService.retrieveContext(any()))
                .thenReturn(List.of(chunk));

        Mockito.when(llmClient.generateAnswer(any(), any()))
                .thenReturn("final answer");

        ChatResponse response =
                orchestrator.handleUserQuery("What is Kubernetes?");

        assertEquals("final answer", response.answer());
        assertEquals(List.of("doc.pdf"), response.sources());
    }
}
