package com.example.rag.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.rag.service.RagIndexingService;
import com.example.rag.service.TextChunker;

@RestController
@RequestMapping("/admin")
public class IndexingController {
    private static final Logger log =
            LoggerFactory.getLogger(IndexingController.class);

    private static final String FILES_PATH =
            "D://From-C-Drive//Weekly-Architecture-India-Tasks-Status//Tech-Task//RAG-Architecture//Data-For-POC//rag-arch//src//main//resources//rag-data//kubernetes//";

    private final RagIndexingService indexingService;

    public IndexingController(RagIndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @PostMapping("/reindex")
    public void reindex() throws IOException {
        log.info("Triggered reindexing documents:");
        indexingService.indexDocuments(FILES_PATH);
    }
}
