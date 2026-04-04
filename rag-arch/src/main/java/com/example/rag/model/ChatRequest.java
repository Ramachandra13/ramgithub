package com.example.rag.model;

import java.util.List;

public class ChatRequest {

    private List<Message> messages;

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public String latestUserQuestion() {
        return messages.stream()
                .filter(m -> "user".equalsIgnoreCase(m.role()))
                .reduce((first, second) -> second)
                .map(Message::content)
                .orElseThrow(() ->
                        new IllegalArgumentException("No user message found"));
    }

    public record Message(String role, String content) {}
}
