package com.example.rag.service;

import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.rag.model.DocumentChecksumUtil;
import com.example.rag.model.DocumentChunk;
import com.example.rag.model.PageText;
import com.example.rag.vector.VectorStoreClient;


@Service
public class RagIndexingService {

    private static final Logger log = LoggerFactory.getLogger(RagIndexingService.class);


    private final RagDataLoader loader;
    private final PdfDocumentExtractor extractor;
    private final TextChunker chunker;
    private final EmbeddingClient embeddingClient;
    private final VectorStoreClient vectorStore;

    private final QdrantMetadataService metadataService;
    private final DocumentChecksumUtil checksumUtil;

    public RagIndexingService(
            RagDataLoader loader,
            PdfDocumentExtractor extractor,
            TextChunker chunker,
            EmbeddingClient embeddingClient,
            VectorStoreClient vectorStore,
            QdrantMetadataService metadataService,
            DocumentChecksumUtil checksumUtil
    ) {
        this.loader = loader;
        this.extractor = extractor;
        this.chunker = chunker;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.metadataService = metadataService;
        this.checksumUtil = checksumUtil;

    }

//    @PostConstruct
    public void indexDocuments(String filesPath) throws IOException {

//       String filesPath = "D://From-C-Drive//Weekly-Architecture-India-Tasks-Status//Tech-Task//RAG-Architecture//Data-For-POC//rag-arch//src//main//resources//rag-data//kubernetes//";

        log.info("Starting document indexing");

        List<Path> pdfs = loader.loadDocuments(filesPath);

        int total = pdfs.size();
        int processed = 0;


        List<DocumentChunk> allChunks = new ArrayList<>();

        // after chunking
        log.info("Number of chunks created: {}", allChunks.size());

        for (Path pdf : pdfs) {

            processed++;
            indexSingleDocument(pdf, processed, total);


            String fileName = pdf.getFileName().toString();
            String documentId = UUID.randomUUID().toString();

            log.info("Processing document: {}", fileName);

            // 1. Extract page-level text
            List<PageText> pages = extractor.extractPages(pdf);

            if (pages.isEmpty()) {
                log.warn("No text extracted from {}", pdf.getFileName());
                continue;
            }

            // 2. Combine pages into a single string
            String fullText = pages.stream()
                    .map(PageText::getText)
                    .collect(Collectors.joining("\n"));


            // 3. Chunk text → DocumentChunks
                List<DocumentChunk> chunks = chunker.chunk(
                        documentId,
                        fileName,
                        fullText,
                        1000,   // chunk size
                        150     // overlap
                );



            // 4. Generate embeddings
                for (DocumentChunk chunk : chunks) {
                    float[] embedding =
                            embeddingClient.embed(chunk.generateEmbeddingText());

                        chunk.setEmbedding(embedding);
                    }

            allChunks.addAll(chunks);

        }

        // after embeddings
        log.info("Total chunks created: {}", allChunks.size());

        vectorStore.upsert(allChunks);

        log.info("Document indexing completed successfully");

    }

    private void indexSingleDocument(
            Path pdf,
            int current,
            int total
    ) {
        String fileName = pdf.getFileName().toString();
        String documentId = fileName; // ✅ stable ID

        log.info("[{}/{}] Processing {}", current, total, fileName);

        try {
            byte[] bytes = Files.readAllBytes(pdf);
            String checksum = checksumUtil.sha256(bytes);

            Optional<String> existing =
                    Optional.ofNullable(
                            metadataService.fetchChecksum(documentId)
                    ).orElse(Optional.empty());


            if (existing.isPresent()
                    && existing.get().equals(checksum)) {

                log.info("Skipping unchanged document: {}", fileName);
                return;
            }

            if (existing.isPresent()) {
                log.info("Checksum changed, deleting old vectors: {}", fileName);
                metadataService.deleteDocumentVectors(documentId);
            }

            List<PageText> pages = extractor.extractPages(pdf);

            if (pages.isEmpty()) {
                log.warn("No text extracted from {}", fileName);
                return;
            }

            String fullText = pages.stream()
                    .map(PageText::getText)
                    .collect(Collectors.joining("\n"));

            List<DocumentChunk> chunks = chunker.chunk(
                    documentId,
                    fileName,
                    fullText,
                    1000,
                    150
            );

            for (DocumentChunk chunk : chunks) {

                chunk.getMetadata().put("checksum", checksum);
                chunk.getMetadata().put("document_id", documentId);

                chunk.setEmbedding(
                        embeddingClient.embed(chunk.generateEmbeddingText())
                );
            }

            vectorStore.upsert(chunks);

            log.info("Indexed document {} ({} chunks)",
                    fileName, chunks.size());

        } catch (Exception ex) {
            log.error("Failed processing {}", fileName, ex);
        }
    }
}

