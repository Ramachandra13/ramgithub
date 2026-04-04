package com.example.rag.orchestrator;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.rag.model.ChatResponse;
import com.example.rag.model.DocumentChunk;
import com.example.rag.service.LlmClient;
import com.example.rag.service.RagService;
import com.example.rag.service.TextChunker;

@Service
public class ChatOrchestratorService {

    private static final Logger log =
            LoggerFactory.getLogger(ChatOrchestratorService.class);

    private final RagService ragService;
    private final LlmClient llmClient;

    public ChatOrchestratorService(RagService ragService, LlmClient llmClient) {
        this.ragService = ragService;
        this.llmClient = llmClient;
    }


    public ChatResponse handleUserQuery(String question) {
        log.info("Question received: {}", question);

        List<DocumentChunk> context = ragService.retrieveContext(question);

        log.info("context: {}", context);

        String answer = llmClient.generateAnswer(question, context);

        List<ChatResponse.Source> sources = context.stream()
                .map(chunk -> new ChatResponse.Source(
                        (String) chunk.getMetadata().get("document_id"),
                        (Integer) chunk.getMetadata().get("start_page"),
                        (Integer) chunk.getMetadata().get("end_page"),
                        (String) chunk.getMetadata().get("section_path")
                ))
                .distinct()
                .toList();

        return new ChatResponse(answer, sources);

    }

}
