package com.example.rag.controller;

import com.example.rag.model.ChatRequest;
import com.example.rag.model.ChatResponse;
import com.example.rag.orchestrator.ChatOrchestratorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
}
