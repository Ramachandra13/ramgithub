package com.example.rag.service;

import com.example.rag.config.EmbeddingProperties;
import com.example.rag.extractor.ExtractionStrategy;
import com.example.rag.model.DocumentChunk;
import com.example.rag.model.DocumentSection;
import com.example.rag.model.PageText;
import com.example.rag.tokenizer.ITokenizer;

import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

@Service
public class DocumentExtractor {

    private final int maxChunkTokens;
    private final int chunkOverlapTokens;
    private final ITokenizer tokenizer;

    private static final char[] SENTENCE_ENDINGS = {'.', '!', '?'};

    public DocumentExtractor(EmbeddingProperties properties, ITokenizer tokenizer) {
        this.maxChunkTokens = properties.getMaxChunkTokens();
        this.chunkOverlapTokens = properties.getChunkOverlapTokens();
        this.tokenizer = tokenizer;

        if (maxChunkTokens <= 0) {
            throw new IllegalArgumentException("Max chunk tokens must be greater than 0");
        }
        if (chunkOverlapTokens < 0 || chunkOverlapTokens >= maxChunkTokens) {
            throw new IllegalArgumentException(
                    "Chunk overlap tokens must be between 0 and max chunk tokens");
        }
    }

    /* ===================== ENTRY POINT ===================== */

    public List<DocumentChunk> processDocument(
            Path filePath,
            ExtractionStrategy extractionStrategy,
            String documentId,
            String checksum
    ) {

        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found: " + filePath);
        }

        try (InputStream stream = new FileInputStream(filePath.toFile())) {
            List<DocumentSection> sections =
                    extractionStrategy.extract(stream);
            return createChunksFromSections(sections, documentId, checksum);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process document: " + filePath, e);
        }
    }

    /* ===================== SECTION → CHUNKS ===================== */

    private List<DocumentChunk> createChunksFromSections(
            List<DocumentSection> sections,
            String documentId,
            String checksum
    ) {

        List<TempChunk> tempChunks = new ArrayList<>();

        for (DocumentSection section : sections) {
            tempChunks.addAll(createTempChunksFromSection(section));
        }

        int totalChunks = tempChunks.size();
        List<DocumentChunk> result = new ArrayList<>(totalChunks);

        for (int i = 0; i < totalChunks; i++) {
            TempChunk tc = tempChunks.get(i);
            result.add(
                    createChunk(
                            tc.text,
                            tc.section,
                            tc.pages,
                            documentId,
                            checksum,
                            i,
                            totalChunks
                    )
            );
        }

        return result;
    }

    /* ===================== SECTION SPLITTING ===================== */

    private List<TempChunk> createTempChunksFromSection(
            DocumentSection section
    ) {

        if (section.getPageTexts().isEmpty()
                || section.getText().isBlank()) {
            return List.of();
        }

        int tokenCount =
                tokenizer.countTokens(section.getText());

        if (tokenCount <= maxChunkTokens) {
            List<Integer> pages =
                    section.getPageTexts()
                            .stream()
                            .map(PageText::getPageNumber)
                            .toList();

            return List.of(
                    new TempChunk(
                            section.getText(),
                            section,
                            pages
                    )
            );
        }

        return splitLargeSection(section);
    }

    /* ===================== TOKEN-AWARE SPLIT ===================== */

    private List<TempChunk> splitLargeSection(
            DocumentSection section
    ) {

        List<TempChunk> result = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        Set<Integer> pages = new HashSet<>();
        int tokenCount = 0;

        for (PageText page : section.getPageTexts()) {
            String content = page.getText();
            int index = 0;

            while (index < content.length()) {

                int available = maxChunkTokens - tokenCount;

                if (available <= 0) {
                    flushChunk(result, buffer, pages, section);
                    tokenCount = applyOverlap(buffer);
                    continue;
                }

                String remaining = content.substring(index);
                int remainingTokens =
                        tokenizer.countTokens(remaining);

                if (remainingTokens <= available) {
                    buffer.append(" ").append(remaining);
                    pages.add(page.getPageNumber());
                    tokenCount += remainingTokens;
                    break;
                }

                String fitted =
                        getTextWithinTokenLimit(
                                remaining,
                                available
                        );

                buffer.append(" ").append(fitted);
                pages.add(page.getPageNumber());

                flushChunk(result, buffer, pages, section);
                tokenCount = applyOverlap(buffer);

                index += fitted.length();
            }
        }

        if (!buffer.isEmpty()) {
            flushChunk(result, buffer, pages, section);
        }

        return result;
    }

    private void flushChunk(
            List<TempChunk> result,
            StringBuilder buffer,
            Set<Integer> pages,
            DocumentSection section
    ) {
        result.add(
                new TempChunk(
                        buffer.toString().trim(),
                        section,
                        pages.stream().sorted().toList()
                )
        );
        buffer.setLength(0);
        pages.clear();
    }

    /* ===================== HELPERS ===================== */

    private int applyOverlap(StringBuilder buffer) {
        if (chunkOverlapTokens <= 0) return 0;

        String overlap = getOverlapText(buffer.toString());
        buffer.setLength(0);
        buffer.append(overlap);

        return tokenizer.countTokens(overlap);
    }

    private String getTextWithinTokenLimit(
            String text,
            int maxTokens
    ) {

        int low = 0, high = text.length(), best = 0;

        while (low <= high) {
            int mid = (low + high) / 2;
            if (tokenizer.countTokens(
                    text.substring(0, mid)) <= maxTokens) {
                best = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        String candidate = text.substring(0, best);

        for (int i = candidate.length() - 1; i >= 0; i--) {
            if (Arrays.binarySearch(
                    SENTENCE_ENDINGS,
                    candidate.charAt(i)) >= 0) {
                return candidate.substring(0, i + 1);
            }
        }
        return candidate;
    }

    private String getOverlapText(String text) {
        int tokens = tokenizer.countTokens(text);
        if (tokens <= chunkOverlapTokens) return "";

        int start = text.length();
        for (int i = 0; i < text.length(); i++) {
            if (tokenizer.countTokens(
                    text.substring(i)) <= chunkOverlapTokens) {
                start = i;
                break;
            }
        }
        return text.substring(start);
    }

    /* ===================== FINAL CHUNK ===================== */


    private DocumentChunk createChunk(
            String text,
            DocumentSection section,
            List<Integer> pages,
            String documentId,
            String checksum,
            int chunkIndex,
            int chunkTotal
    ) {

        DocumentChunk chunk = new DocumentChunk();

        chunk.setChunkIndex(chunkIndex);
        chunk.setChunkTotal(chunkTotal);

        chunk.setId(
                UUID.nameUUIDFromBytes(
                        (documentId + ":" + chunkIndex)
                                .getBytes()
                ).toString()
        );

        chunk.setContent(text);
        chunk.setText(text);
        chunk.setSourceDocument(documentId);

        chunk.setSection(section.getHeadingText());
        chunk.setSectionPath(section.getFullHeadingPath());

        chunk.setStartPage(
                pages.isEmpty() ? null :
                        Collections.min(pages)
        );
        chunk.setEndPage(
                pages.isEmpty() ? null :
                        Collections.max(pages)
        );

        Map<String, Object> metadata =
                chunk.getMetadata();

        metadata.put("document_id", documentId);
        metadata.put("checksum", checksum);
        metadata.put("indexed_at",
                Instant.now().toString());

        metadata.put("chunk_index", chunkIndex);
        metadata.put("chunk_total", chunkTotal);

        metadata.put("start_page",
                chunk.getStartPage());
        metadata.put("end_page",
                chunk.getEndPage());
        metadata.put("pages", pages);
        metadata.put("section_path",
                section.getFullHeadingPath());

        return chunk;
    }

    /* ===================== INTERNAL DTO ===================== */

    private static class TempChunk {
        final String text;
        final DocumentSection section;
        final List<Integer> pages;

        TempChunk(
                String text,
                DocumentSection section,
                List<Integer> pages
        ) {
            this.text = text;
            this.section = section;
            this.pages = pages;
        }
    }

}
