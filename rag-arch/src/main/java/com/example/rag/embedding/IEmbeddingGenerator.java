package com.example.rag.embedding;

import com.example.rag.model.DocumentChunk;

import java.util.List;
import java.util.concurrent.CompletableFuture;


public interface IEmbeddingGenerator {

    float[] generateEmbedding(String text);

//    List<float[]> generateEmbeddings(List<DocumentChunk> chunks);

}
