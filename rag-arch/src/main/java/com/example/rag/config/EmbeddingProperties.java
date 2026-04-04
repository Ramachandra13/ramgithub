package com.example.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingProperties {

    private String endpoint;
    private int maxChunkTokens;
    private int chunkOverlapTokens;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public int getMaxChunkTokens() {
        return maxChunkTokens;
    }

    public void setMaxChunkTokens(int maxChunkTokens) {
        this.maxChunkTokens = maxChunkTokens;
    }

    public int getChunkOverlapTokens() {
        return chunkOverlapTokens;
    }

    public void setChunkOverlapTokens(int chunkOverlapTokens) {
        this.chunkOverlapTokens = chunkOverlapTokens;
    }
}
