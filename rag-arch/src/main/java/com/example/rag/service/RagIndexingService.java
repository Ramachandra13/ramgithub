package com.example.rag.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.rag.extractor.ExtractionStrategy;
import com.example.rag.model.DocumentChunk;
import com.example.rag.model.DocumentChecksumUtil;
import com.example.rag.vector.VectorStoreClient;

@Service
public class RagIndexingService {

    private static final Logger log =
            LoggerFactory.getLogger(RagIndexingService.class);

    private final AtomicBoolean indexing = new AtomicBoolean(false);

    private final RagDataLoader loader;
    private final DocumentExtractor documentExtractor;
    private final EmbeddingClient embeddingClient;
    private final VectorStoreClient vectorStore;
    private final QdrantMetadataService metadataService;
    private final DocumentChecksumUtil checksumUtil;
    private final ExtractionStrategy extractionStrategy;

    public RagIndexingService(
            RagDataLoader loader,
            DocumentExtractor documentExtractor,
            EmbeddingClient embeddingClient,
            VectorStoreClient vectorStore,
            QdrantMetadataService metadataService,
            DocumentChecksumUtil checksumUtil,
            ExtractionStrategy extractionStrategy
    ) {
        this.loader = loader;
        this.documentExtractor = documentExtractor;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.metadataService = metadataService;
        this.checksumUtil = checksumUtil;
        this.extractionStrategy = extractionStrategy;
    }

    /**
     * Entry point for full indexing.
     */
    public void indexDocuments(String filesPath) throws IOException {


        if (!indexing.compareAndSet(false, true)) {
            log.warn("Indexing already running, skipping");
            return;
        }

        try {
            log.info("Starting document indexing from {}", filesPath);

            List<Path> documents = loader.loadDocuments(filesPath);
            int total = documents.size();

            for (int i = 0; i < total; i++) {
                indexSingleDocument(documents.get(i), i + 1, total);
            }

            log.info("Document indexing completed successfully");

        } finally {
            indexing.set(false);
        }

    }

    /**
     * Index exactly one document (idempotent).
     */
    private void indexSingleDocument(
            Path filePath,
            int current,
            int total
    ) {

        String documentId = filePath.getFileName().toString();
        log.info("[{}/{}] Processing {}", current, total, documentId);

        try {
            byte[] bytes = Files.readAllBytes(filePath);
            String checksum = checksumUtil.sha256(bytes);

            Optional<String> existing =
                    metadataService.fetchChecksum(documentId);

            if (existing.isPresent() && existing.get().equals(checksum)) {
                log.info("Skipping unchanged document {}", documentId);
                return;
            }

            if (existing.isPresent()) {
                log.info("Checksum changed, deleting old vectors for {}",
                        documentId);
                metadataService.deleteDocumentVectors(documentId);
            }

            // ✅ Chunking + metadata handled ONLY here
            List<DocumentChunk> chunks =
                    documentExtractor.processDocument(
                            filePath,
                            extractionStrategy,
                            documentId,
                            checksum
                    );

            if (chunks.isEmpty()) {
                log.warn("No chunks produced for {}", documentId);
                return;
            }

            // ✅ Embeddings
            for (DocumentChunk chunk : chunks) {
                chunk.setEmbedding(
                        embeddingClient.embed(
                                chunk.generateEmbeddingText()
                        )
                );
            }

            vectorStore.upsert(chunks);

            log.info("Indexed {} ({} chunks)",
                    documentId, chunks.size());

        } catch (Exception ex) {
            log.error("Failed processing {}", documentId, ex);
        }
    }
}
