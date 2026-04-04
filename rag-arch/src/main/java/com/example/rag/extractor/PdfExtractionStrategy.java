package com.example.rag.extractor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.rag.model.DocumentSection;
import com.example.rag.model.PageText;

@Component
public class PdfExtractionStrategy implements ExtractionStrategy {

    private static final Logger log =
            LoggerFactory.getLogger(PdfExtractionStrategy.class);

    @Override
    public List<DocumentSection> extract(InputStream inputStream) {

        List<DocumentSection> sections = new ArrayList<>();

        try (PDDocument document = PDDocument.load(inputStream)) {

            int totalPages = document.getNumberOfPages();
            log.info("Extracting PDF with {} pages", totalPages);

            PDFTextStripper stripper = new PDFTextStripper();

            List<PageText> pageTexts = new ArrayList<>();

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);

                String text = stripper.getText(document).trim();

                if (!text.isBlank()) {
                    pageTexts.add(
                            new PageText(page, text)
                    );
                }
            }

            if (pageTexts.isEmpty()) {
                log.warn("No readable text extracted from PDF");
                return List.of();
            }

            String fullText = pageTexts.stream()
                    .map(PageText::getText)
                    .collect(Collectors.joining("\n"));

            // ✅ Single logical section (safe default)
            DocumentSection bodySection = new DocumentSection();
            bodySection.setLevel(1);
            bodySection.setHeadingText("Document Body");
            bodySection.setFullHeadingPath("Document Body");
            bodySection.setPageTexts(pageTexts);
            bodySection.setText(fullText);

            sections.add(bodySection);


        } catch (IOException e) {
            throw new RuntimeException("Failed to extract PDF content", e);
        }

        return sections;
    }
}
