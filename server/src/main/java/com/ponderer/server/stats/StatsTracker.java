package com.ponderer.server.stats;

import com.ponderer.server.config.MessageConfig;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.logging.Logger;

public final class StatsTracker {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path logFile;
    private final Logger logger;

    public StatsTracker(Path worldRoot, Logger logger) {
        this.logFile = worldRoot.resolve("ponderer").resolve("activity.log");
        this.logger = logger;
    }

    public void recordUpload(UUID uuid, String sceneId) {
        append("UPLOAD uuid=" + uuid + " scene=" + sceneId);
    }

    public void recordAiCall(UUID uuid, String playerName, long inputTokens, long outputTokens, String provider) {
        append("AI_CALL uuid=" + uuid + " player=" + playerName
                + " provider=" + provider
                + " input_tokens=" + inputTokens
                + " output_tokens=" + outputTokens
                + " total=" + (inputTokens + outputTokens));
    }

    public void recordTopup(UUID target, UUID admin, long amount) {
        append("TOPUP target=" + target + " admin=" + admin + " amount=" + amount);
    }

    private void append(String message) {
        String line = "[" + LocalDateTime.now().format(FMT) + "] " + message + System.lineSeparator();
        try {
            Files.createDirectories(logFile.getParent());
            Files.writeString(logFile, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            logger.warning(MessageConfig.global("log_activity_write_failed", e.getMessage()));
        }
    }
}
