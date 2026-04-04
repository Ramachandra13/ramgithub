package com.example.rag.service;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;


@Component
public class RagDataLoader {

    public List<Path> loadDocuments(String location) throws IOException {

        ClassPathResource resource = new ClassPathResource("rag-data");

        return Files.walk(resource.getFile().toPath())
                .filter(path -> path.toString().endsWith(".pdf"))
                .toList();
    }

}
