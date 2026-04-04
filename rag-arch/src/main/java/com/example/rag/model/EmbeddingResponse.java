package com.example.rag.model;

import java.util.List;

public class EmbeddingResponse {

    private List<float[]> embeddings;
    private int dimensions;

    public List<float[]> getEmbeddings() {
        return embeddings;
    }

    public void setEmbeddings(List<float[]> embeddings) {
        this.embeddings = embeddings;
    }

    public int getDimensions() {
        return dimensions;
    }

    public void setDimensions(int dimensions) {
        this.dimensions = dimensions;
    }
}

