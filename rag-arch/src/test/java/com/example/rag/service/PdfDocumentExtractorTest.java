package com.example.rag.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import com.example.rag.model.PageText;

@SpringBootTest
class PdfDocumentExtractorTest {

    @Autowired
    PdfDocumentExtractor pdfDocumentExtractor;

//    private final PdfDocumentExtractor pdfDocumentExtractor = new PdfDocumentExtractor();

    @Test
    void shouldExtractTextFromPdf() {

        // Place a small test PDF under src/test/resources
        Path pdfPath = Path.of("src/test/resources/sample.pdf");

        List<PageText > pages = pdfDocumentExtractor.extractPages(pdfPath);

        assertNotNull(pages);
        assertFalse(pages.isEmpty());
    }
}
