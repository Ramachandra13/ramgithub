package com.example.rag.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.rag.vector.VectorStoreClient;

@Service
public class QdrantMetadataService {

    private final VectorStoreClient vectorStore;

    public QdrantMetadataService(VectorStoreClient vectorStore) {
        this.vectorStore = vectorStore;
    }


    public Optional<String> fetchChecksum(String documentId) {

        Optional<String> checksum;

        try {
            checksum = vectorStore.findAnyPayload(
                    Map.of("document_id", documentId),
                    "checksum"
            );
        } catch (Exception ex) {
            // Defensive: never let exceptions propagate as nulls
            return Optional.empty();
        }

        // Enforce Optional contract
        return checksum == null ? Optional.empty() : checksum;

    }

    public void deleteDocumentVectors(String documentId) {
        vectorStore.deleteByFilter(
                Map.of("document_id", documentId)
        );
    }

}
