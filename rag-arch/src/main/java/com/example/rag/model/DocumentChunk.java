package com.example.rag.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentChunk {

    private String id;
    private String source;
    private String content;
    private float[] embedding;


    private String text;
    private String sourceDocument;

    private int chunkIndex;
    private int chunkTotal;

    private String section;
    private int startPage;
    private int endPage;
    private String sectionPath;
    private Map<String, Object> metadata = new HashMap<>();


    public DocumentChunk() {
        this.metadata = new HashMap<>();
    }

    public DocumentChunk(String id, String fileName, float[] embedding, Map<String, Object> metadata) {
        id = id.trim();
        source = fileName;
//        content = chunk;
        this.embedding = embedding;
        this.metadata = metadata;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSourceDocument() {
        return sourceDocument;
    }

    public void setSourceDocument(String sourceDocument) {
        this.sourceDocument = sourceDocument;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public int getChunkTotal() {
        return chunkTotal;
    }

    public void setChunkTotal(int chunkTotal) {
        this.chunkTotal = chunkTotal;
    }

    public int getStartPage() {
        return startPage;
    }

    public void setStartPage(int startPage) {
        this.startPage = startPage;
    }

    public int getEndPage() {
        return endPage;
    }

    public void setEndPage(int endPage) {
        this.endPage = endPage;
    }

    public String getSectionPath() {
        return sectionPath;
    }

    public void setSectionPath(String sectionPath) {
        this.sectionPath = sectionPath;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    /**
     * Generates the text used for embedding creation
     */
    public String generateEmbeddingText() {
        return content;
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getContent() {
        return content;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public Map<String, Object> getMetadata() {
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

}
