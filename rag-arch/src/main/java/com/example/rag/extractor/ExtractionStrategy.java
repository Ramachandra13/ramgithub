package com.example.rag.extractor;

import java.io.InputStream;
import java.util.List;

import com.example.rag.model.DocumentSection;

public interface ExtractionStrategy {
    public List<DocumentSection> extract(InputStream inputStream);
}
