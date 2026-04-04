package com.example.rag.service;

import com.example.rag.model.PageText;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class PdfDocumentExtractor {

    /**
     * Extracts text per page from a PDF file.
     */
    public List<PageText> extractPages(Path pdfPath) {

        List<PageText> pages = new ArrayList<>();

        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {

            PDFTextStripper stripper = new PDFTextStripper();

            int pageCount = document.getNumberOfPages();

            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String text = stripper.getText(document).trim();

                if (!text.isEmpty()) {
                    PageText pageText = new PageText();
                    pageText.setPageNumber(page);
                    pageText.setText(text);
                    pages.add(pageText);
                }
            }

            return pages;

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to extract pages from PDF: " + pdfPath.getFileName(),
                    e
            );
        }
    }
}
