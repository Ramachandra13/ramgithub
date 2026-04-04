package com.example.rag.model;

import java.util.List;
import java.util.Map;

public class ChatCompletionRequest {

    private String model;
    private List<Map<String, String>> messages;
    private double temperature;

    public ChatCompletionRequest(
            String model,
            List<Map<String, String>> messages,
            double temperature
    ) {
        this.model = model;
        this.messages = messages;
        this.temperature = temperature;
    }

    public String getModel() {
        return model;
    }

    public List<Map<String, String>> getMessages() {
        return messages;
    }

    public double getTemperature() {
        return temperature;
    }
}
