package com.example.rag.model;

import java.security.MessageDigest;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
public class DocumentChecksumUtil {

    public String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Checksum failed", e);
        }
    }
}
