package com.example.rag.model;

import java.util.List;

public record ChatResponse(
        String answer,
        List<Source> sources
) {
    public record Source(
            String documentId,
            Integer startPage,
            Integer endPage,
            String section
    ) {}
}
