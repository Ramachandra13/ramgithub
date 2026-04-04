package com.example.rag.service;

import com.example.rag.config.EmbeddingProperties;
import com.example.rag.extractor;
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
import java.util.stream.Collectors;

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
            throw new IllegalArgumentException("Chunk overlap tokens must be between 0 and max chunk tokens");
        }
    }

    public List<DocumentChunk> processDocument(
            Path filePath,
            extractor.ExtractionStrategy extractionStrategy,
            String documentId,
            String checksum
    ) {

        if (!Files.exists(filePath)) {
            throw new RuntimeException("PDF file not found: " + filePath);
        }

        try (InputStream stream = new FileInputStream(filePath.toFile())) {
            List<DocumentSection> sections = extractionStrategy.extract(stream);
            return createChunksFromSections(sections, documentId, checksum);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process document: " + filePath, e);
        }
    }

    private List<DocumentChunk> createChunksFromSections(
            List<DocumentSection> sections,
            String documentId,
            String checksum
    ) {
        List<DocumentChunk> chunks = new ArrayList<>();

        int globalIndex = 0;

//        for (DocumentSection section : sections) {
//            chunks.addAll(createChunksFromSection(section, documentId, checksum));
//        }

        for (DocumentSection section : sections) {
            List<DocumentChunk> sectionChunks =
                    createChunksFromSection(section, documentId, checksum);

            for (DocumentChunk chunk : sectionChunks) {
                chunk.setChunkIndex(globalIndex++);
                chunks.add(chunk);
            }
        }

        // Now we know total
        int total = chunks.size();
        for (DocumentChunk chunk : chunks) {
            chunk.setChunkTotal(total);
            chunk.getMetadata().put("chunk_total", total);
        }

        return chunks;
    }

    private List<DocumentChunk> createChunksFromSection(
            DocumentSection section,
            String documentId,
            String checksum
    ) {

        if (section.getPageTexts().isEmpty() || section.getText().isBlank()) {
            return List.of();
        }

        int tokenCount = tokenizer.countTokens(section.getText());
        if (tokenCount <= maxChunkTokens) {
            List<Integer> pages = section.getPageTexts()
                    .stream()
                    .map(PageText::getPageNumber)
                    .toList();

            return List.of(createChunk(
                    section.getText(),
                    section,
                    pages,
                    documentId,
                    checksum
            ));
        }

        List<ChunkWithPages> subChunks = splitLargeSection(section);
        List<DocumentChunk> results = new ArrayList<>();

        for (int i = 0; i < subChunks.size(); i++) {
            ChunkWithPages sc = subChunks.get(i);
            results.add(createChunk(
                    sc.text(),
                    section,
                    sc.pages(),
                    documentId,
                    checksum
            ));
        }

        return results;
    }

    private List<ChunkWithPages> splitLargeSection(DocumentSection section) {

        List<ChunkWithPages> result = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        Set<Integer> pages = new HashSet<>();
        int tokenCount = 0;

        for (PageText page : section.getPageTexts()) {
            String content = page.getText();
            int index = 0;

            while (index < content.length()) {

                int available = maxChunkTokens - tokenCount;
                if (available <= 0) {
                    flushChunk(result, buffer, pages);
                    tokenCount = applyOverlap(buffer, pages);
                    continue;
                }

                String remaining = content.substring(index);
                int remainingTokens = tokenizer.countTokens(remaining);

                if (remainingTokens <= available) {
                    buffer.append(" ").append(remaining);
                    pages.add(page.getPageNumber());
                    tokenCount += remainingTokens;
                    break;
                }

                String fitted = getTextWithinTokenLimit(remaining, available);
                buffer.append(" ").append(fitted);
                pages.add(page.getPageNumber());

                flushChunk(result, buffer, pages);
                tokenCount = applyOverlap(buffer, pages);

                index += fitted.length();
            }
        }

        if (!buffer.isEmpty()) {
            flushChunk(result, buffer, pages);
        }

        return result;
    }

    private void flushChunk(
            List<ChunkWithPages> result,
            StringBuilder buffer,
            Set<Integer> pages
    ) {
        result.add(new ChunkWithPages(
                buffer.toString().trim(),
                pages.stream().sorted().toList()
        ));
        buffer.setLength(0);
        pages.clear();
    }

    private int applyOverlap(StringBuilder buffer, Set<Integer> pages) {
        if (chunkOverlapTokens <= 0) return 0;

        String overlap = getOverlapText(buffer.toString());
        buffer.setLength(0);
        buffer.append(overlap);
        return tokenizer.countTokens(overlap);
    }

    private String getTextWithinTokenLimit(String text, int maxTokens) {

        int low = 0, high = text.length(), best = 0;

        while (low <= high) {
            int mid = (low + high) / 2;
            if (tokenizer.countTokens(text.substring(0, mid)) <= maxTokens) {
                best = mid;
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        String candidate = text.substring(0, best);
        for (int i = candidate.length() - 1; i >= 0; i--) {
            if (Arrays.binarySearch(SENTENCE_ENDINGS, candidate.charAt(i)) >= 0) {
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
            if (tokenizer.countTokens(text.substring(i)) <= chunkOverlapTokens) {
                start = i;
                break;
            }
        }
        return text.substring(start);
    }

    private DocumentChunk createChunk(
            String text,
            DocumentSection section,
            List<Integer> pages,
            String documentId,
            String checksum
    ) {

        String sectionHash = section.getFullHeadingPath() == null
                ? "nosection"
                : String.valueOf(Math.abs(section.getFullHeadingPath().hashCode()));

        DocumentChunk chunk = new DocumentChunk();
        chunk.setId( UUID.nameUUIDFromBytes((documentId + ":" + chunk.getChunkIndex()).getBytes()).toString());
        chunk.setContent(text);
        chunk.setText(text);
        chunk.setSourceDocument(documentId);

        chunk.setSection(section.getHeadingText());
        chunk.setStartPage(Collections.min(pages));
        chunk.setEndPage(Collections.max(pages));
        chunk.setSectionPath(section.getFullHeadingPath());

        Map<String, Object> metadata = chunk.getMetadata();

        metadata.put("document_id", documentId);
        metadata.put("checksum", checksum);
        metadata.put("indexed_at", Instant.now().toString());
        metadata.put("pages", pages);
        metadata.put("section_path", section.getFullHeadingPath());


//        metadata.put("chunk_type", "bookmark");
//        metadata.put("bookmark_level", section.getLevel());
//        metadata.put("pages", pages);
//        metadata.put("source", documentId);
//        metadata.put("section_path", section.getFullHeadingPath());


        return chunk;
    }

    private record ChunkWithPages(String text, List<Integer> pages) {}
}
