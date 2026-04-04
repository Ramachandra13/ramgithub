package com.example.rag.controller;

import java.util.concurrent.CompletableFuture;

import com.example.rag.model.ChatRequest;
import com.example.rag.model.ChatResponse;
import com.example.rag.orchestrator.ChatOrchestratorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/rag")
public class RagController {

    private static final Logger log =
            LoggerFactory.getLogger(RagController.class);

    private final ChatOrchestratorService orchestrator;

    public RagController(ChatOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        log.info("Initiated chat : ", request.toString());

        String question = request.latestUserQuestion();

        return orchestrator.handleUserQuery(question);
    }

    @PostMapping(
            value = "/chatstream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter chatStream(@RequestBody ChatRequest request) {

        SseEmitter emitter = new SseEmitter(0L); // no timeout

        CompletableFuture.runAsync(() -> {
            try {
                String question = request.latestUserQuestion();
                orchestrator.streamUserQuery(question, emitter);
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
