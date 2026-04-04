package com.example.rag.service;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import com.example.rag.config.RagIndexingProperties;

@Component
public class RagIndexingRunner {

    private static final Logger log = LoggerFactory.getLogger(RagIndexingRunner.class);

    private final RagIndexingService indexingService;
    private final RagIndexingProperties props;
    private final AtomicBoolean started = new AtomicBoolean(false);


    private static final String FILES_PATH =
            "D://From-C-Drive//Weekly-Architecture-India-Tasks-Status//Tech-Task//RAG-Architecture//Data-For-POC//rag-arch//src//main//resources//rag-data//kubernetes//";


    public RagIndexingRunner(
            RagIndexingService indexingService,
            RagIndexingProperties props
    ) {
        this.indexingService = indexingService;
        this.props = props;
    }


    @Async("indexingExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void startIndexingAsync() {

        // Prevent double execution
        if (!started.compareAndSet(false, true)) {
            log.warn("Indexing already started, skipping");
            return;
        }

        try {
            indexingService.indexDocuments(FILES_PATH);
        } catch (IOException ex) {
            log.error("Background indexing failed", ex);
        }
    }
}
