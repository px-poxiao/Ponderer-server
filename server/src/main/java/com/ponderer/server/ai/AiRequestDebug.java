package com.ponderer.server.ai;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class AiRequestDebug {

    private AiRequestDebug() {}

    public static String keyFingerprint(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "empty";
        }
        String trimmed = apiKey.trim();
        String start = trimmed.length() <= 6 ? trimmed : trimmed.substring(0, 6);
        String end = trimmed.length() <= 4 ? trimmed : trimmed.substring(trimmed.length() - 4);
        return start + "..." + end + " len=" + trimmed.length() + " sha256=" + sha256(trimmed).substring(0, 12);
    }

    public static String providerUrl(String provider, String baseUrl) {
        if ("openai".equalsIgnoreCase(provider)) {
            return OpenAiProvider.chatCompletionsUrl(baseUrl);
        }
        if ("anthropic".equalsIgnoreCase(provider)) {
            return AnthropicProvider.messagesUrl(baseUrl);
        }
        return baseUrl == null || baseUrl.isBlank() ? "<default>" : baseUrl.trim();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ignored) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
