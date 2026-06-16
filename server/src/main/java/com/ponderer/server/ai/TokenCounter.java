package com.ponderer.server.ai;

/**
 * Estimates token count using character-based heuristics.
 * English: ~4 chars/token, CJK: ~1.5 chars/token.
 */
public final class TokenCounter {

    private TokenCounter() {}

    public static long estimate(String text) {
        if (text == null || text.isEmpty()) return 0;
        long cjk = text.codePoints().filter(TokenCounter::isCjk).count();
        long other = text.length() - cjk;
        return (long) Math.ceil(cjk / 1.5 + other / 4.0);
    }

    private static boolean isCjk(int cp) {
        return (cp >= 0x4E00 && cp <= 0x9FFF)
                || (cp >= 0x3400 && cp <= 0x4DBF)
                || (cp >= 0x20000 && cp <= 0x2A6DF)
                || (cp >= 0xAC00 && cp <= 0xD7AF)
                || (cp >= 0x3040 && cp <= 0x30FF);
    }
}