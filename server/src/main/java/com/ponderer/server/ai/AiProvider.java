package com.ponderer.server.ai;

import java.util.concurrent.CompletableFuture;

public interface AiProvider {
    CompletableFuture<String> generate(String systemPrompt, String userContent,
                                       String baseUrl, String apiKey, String model, int maxTokens);
}