package com.example.rag.service;

import com.example.rag.model.DocumentChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.BreakIterator;
import java.util.*;

@Component
public class TextChunker {

    private static final Logger log =
            LoggerFactory.getLogger(TextChunker.class);

    /**
     * Chunk text into DocumentChunk objects for RAG.
     *
     * @param documentId     unique document id
     * @param sourceDocument filename / source
     * @param text           full document text
     * @param chunkSize      max characters per chunk
     * @param overlap        overlapping characters
     */
    public List<DocumentChunk> chunk(
            String documentId,
            String sourceDocument,
            String text,
            int chunkSize,
            int overlap
    ) {

        if (text == null || text.isBlank()) {
            log.warn("Empty text received for chunking: {}", sourceDocument);
            return List.of();
        }

        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be > 0");
        }

        if (overlap < 0 || overlap >= chunkSize) {
            throw new IllegalArgumentException(
                    "overlap must be >= 0 and < chunkSize");
        }

        List<String> sentences = splitIntoSentences(text);
        List<String> chunkTexts = new ArrayList<>();

        StringBuilder current = new StringBuilder();

        for (String sentence : sentences) {
            if (current.length() + sentence.length() > chunkSize) {
                chunkTexts.add(current.toString().trim());

                int overlapChars = Math.min(overlap, current.length());
                current = new StringBuilder(
                        current.substring(current.length() - overlapChars)
                );
            }
            current.append(sentence).append(" ");
        }

        String lastChunk = current.toString().trim();
        if (!lastChunk.isEmpty()) {
            chunkTexts.add(lastChunk);
        }


        int totalChunks = chunkTexts.size();
        List<DocumentChunk> result = new ArrayList<>(totalChunks);

        for (int i = 0; i < totalChunks; i++) {
            String chunkText = chunkTexts.get(i);

            DocumentChunk chunk = new DocumentChunk();


// -------- core identity --------
            chunk.setId(UUID.randomUUID().toString());
            chunk.setSource(sourceDocument);
            chunk.setSourceDocument(sourceDocument);

            // -------- content --------
            chunk.setContent(chunkText);   // used for embeddings
            chunk.setText(chunkText);      // UI / debug

            // -------- chunk info --------
            chunk.setChunkIndex(i);
            chunk.setChunkTotal(totalChunks);

            // -------- optional defaults --------
            chunk.setSection("body");
            chunk.setStartPage(-1);
            chunk.setEndPage(-1);

            // -------- IMPORTANT: metadata --------
            Map<String, Object> metadata = chunk.getMetadata();

            metadata.put("document_id", documentId);
            metadata.put("source", sourceDocument);
            metadata.put("chunk_index", i);
            metadata.put("chunk_total", totalChunks);
            metadata.put("section", "body");

            result.add(chunk);

        }

        log.info(
                "Chunked document '{}' into {} chunks",
                sourceDocument, totalChunks
        );

        return result;
    }

    private List<String> splitIntoSentences(String text) {
        BreakIterator iterator =
                BreakIterator.getSentenceInstance(Locale.US);

        iterator.setText(text);
        List<String> sentences = new ArrayList<>();

        int start = iterator.first();
        for (int end = iterator.next();
             end != BreakIterator.DONE;
             start = end, end = iterator.next()) {

            sentences.add(text.substring(start, end));
        }
        return sentences;
    }
}
