package com.example.rag.tokenizer.impl;

import com.example.rag.tokenizer.ITokenizer;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.ModelType;

import org.springframework.stereotype.Component;

@Component
public class OpenAiTokenizer implements ITokenizer {

    private final Encoding encoding;

    public OpenAiTokenizer() {
        this.encoding = Encodings.newLazyEncodingRegistry()
                .getEncoding("cl100k_base")
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Encoding cl100k_base not found. " +
                                        "Check that jtokkit is on the classpath."
                        )
                );

    }

    @Override
    public int countTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
//        return encoding.encode(text).size();
        return encoding.countTokens(text);
    }
}
