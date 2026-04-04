package com.example.rag;

import java.io.InputStream;
import java.util.List;

import com.example.rag.model.DocumentSection;

public class extractor {
    public static interface ExtractionStrategy {
        List<DocumentSection> extract(InputStream pdfStream);
    }
}
