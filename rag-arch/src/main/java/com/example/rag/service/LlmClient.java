package com.example.rag.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.example.rag.model.ChatCompletionRequest;
import com.example.rag.model.ChatCompletionResponse;
import com.example.rag.model.DocumentChunk;

@Component
public class LlmClient {

    private static final Logger log =
            LoggerFactory.getLogger(LlmClient.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${llm.service.url}")
    private String llmUrl;

    @Value("${llm.model:gpt-4o-mini}")
    private String model;

    @Value("${llm.api-key:}")
    private String apiKey;

    /**
     * Generate a grounded answer using retrieved context.
     */
    public String generateAnswer(
            String question,
            List<DocumentChunk> context
    ) {

        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question must not be empty");
        }

        if (context == null || context.isEmpty()) {
            log.warn("No context chunks provided, answering without retrieval");
        }

        log.info("Generating answer for question: {}", question);
        log.info("Using {} context chunks", context.size());

        String systemPrompt = buildSystemPrompt();
        String contextBlock = buildContextBlock(context);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content",
                contextBlock + "\n\nQuestion:\n" + question));

        ChatCompletionRequest request =
                new ChatCompletionRequest(
                        model,
                        messages,
                        0.2
                );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (!apiKey.isBlank()) {
            headers.setBearerAuth(apiKey);
        }

        HttpEntity<ChatCompletionRequest> entity =
                new HttpEntity<>(request, headers);

        ResponseEntity<ChatCompletionResponse> response =
                restTemplate.exchange(
                        llmUrl + "/v1/chat/completions",
                        HttpMethod.POST,
                        entity,
                        ChatCompletionResponse.class
                );

        ChatCompletionResponse body = response.getBody();

        if (body == null
                || body.choices() == null
                || body.choices().isEmpty()) {

            throw new IllegalStateException("Empty LLM response");
        }

        String answer = body.choices().get(0).message().content();

        log.debug("LLM answer length: {}", answer.length());
        return answer;
    }

    /* ===================== PROMPT HELPERS ===================== */

    private String buildSystemPrompt() {
        return """
        You are a helpful assistant answering questions using ONLY the provided context.
        Follow these rules strictly:
        - If the answer is not present in the context, say "I don't know based on the provided information."
        - Do NOT hallucinate.
        - Be concise and factual.
        - Cite relevant sections implicitly when possible.
        """;
    }

    private String buildContextBlock(List<DocumentChunk> chunks) {

        if (chunks == null || chunks.isEmpty()) {
            return "Context: [No context available]";
        }

        return "Context:\n" +
                chunks.stream()
                        .map(c -> "- " + c.getText())
                        .collect(Collectors.joining("\n"));
    }
}
