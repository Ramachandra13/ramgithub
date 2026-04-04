package com.example.rag.model;

import java.util.Collection;

public class EmbeddingRequest {

    private Collection<String> texts;

    public EmbeddingRequest(Collection<String> texts) {
        this.texts = texts;
    }

    public Collection<String> getTexts() {
        return texts;
    }

    public void setTexts(Collection<String> texts) {
        this.texts = texts;
    }
}
