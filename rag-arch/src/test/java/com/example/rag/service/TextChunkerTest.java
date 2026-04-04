package com.example.rag.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TextChunkerTest {

    private final TextChunker chunker = new TextChunker();

    @Test
    void shouldSplitTextIntoChunksWithOverlap() {

        String text = "A".repeat(1000);

        List<String> chunks = chunker.chunk(text, 300, 50, 10, 5);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() > 1);
        assertEquals(300, chunks.get(0).length());
    }
}
