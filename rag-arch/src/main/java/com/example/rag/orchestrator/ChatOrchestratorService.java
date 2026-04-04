package com.example.rag.orchestrator;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.rag.model.ChatResponse;
import com.example.rag.model.DocumentChunk;
import com.example.rag.service.LlmClient;
import com.example.rag.service.RagService;

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


    /* ===================== NON-STREAMING ===================== */

    public ChatResponse handleUserQuery(String question) {

        log.info("Question received: {}", question);

        StopWatch watch = new StopWatch("RAG Pipeline");
        watch.start("retrieve-context");

        List<DocumentChunk> context =
                ragService.retrieveContext(question);

        watch.stop();

        log.info("Retrieved {} context chunks", context.size());

        if (context.isEmpty()) {
            log.warn("No context found for question");

            return new ChatResponse(
                    "I don't know based on the provided information.",
                    List.of()
            );
        }

        watch.start("llm-generation");

        String answer =
                llmClient.generateAnswer(question, context);

        watch.stop();

        List<ChatResponse.Source> sources =
                buildSources(context);

        log.info(watch.prettyPrint());

        return new ChatResponse(answer, sources);
    }
    /* ===================== STREAMING ===================== */
    public void streamUserQuery(
            String question,
            SseEmitter emitter
    ) {

        StopWatch watch = new StopWatch("RAG Pipeline (Streaming)");
        watch.start("retrieve-context");

        List<DocumentChunk> context =
                ragService.retrieveContext(question);

        watch.stop();

        log.info("Retrieved {} context chunks", context.size());

        if (context.isEmpty()) {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name("final")
                                .data(new ChatResponse(
                                        "I don't know based on the provided information.",
                                        List.of()
                                ))
                );
            } catch (Exception e) {
                emitter.completeWithError(e);
                return;
            }
            emitter.complete();
            return;
        }

        watch.start("llm-stream");

        llmClient.streamAnswer(question, context, emitter);

        watch.stop();

        try {
            emitter.send(
                    SseEmitter.event()
                            .name("sources")
                            .data(buildSources(context))
            );
        } catch (Exception e) {
            emitter.completeWithError(e);
            return;
        }

        log.info(watch.prettyPrint());
        emitter.complete();
    }

    private List<ChatResponse.Source> buildSources(
            List<DocumentChunk> context
    ) {
        return context.stream()
                .map(chunk -> new ChatResponse.Source(
                        // Required: document identity
                        (String) chunk.getMetadata().get("document_id"),

                        // Optional but important: page range
                        (Integer) chunk.getMetadata().get("start_page"),
                        (Integer) chunk.getMetadata().get("end_page"),

                        // Optional: section / heading info
                        (String) chunk.getMetadata().get("section_path")
                ))
                // Remove duplicate sources caused by multiple chunks
                .distinct()
                .toList();
    }
}
