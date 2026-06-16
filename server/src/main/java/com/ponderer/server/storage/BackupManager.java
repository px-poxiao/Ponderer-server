package com.ponderer.server.storage;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

public final class BackupManager {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Path backupRoot;
    private final int maxBackups;
    private final Logger logger;

    public BackupManager(Path worldRoot, int maxBackups, Logger logger) {
        this.backupRoot = worldRoot.resolve("ponderer").resolve(".backups");
        this.maxBackups = maxBackups;
        this.logger = logger;
    }

    public void backupScene(String sceneId, byte[] currentBytes) {
        if (maxBackups <= 0 || currentBytes == null || currentBytes.length == 0) return;
        String safeName = sceneId.replace(':', '_').replace('/', '_');
        Path dir = backupRoot.resolve(safeName);
        try {
            Files.createDirectories(dir);
            String timestamp = LocalDateTime.now().format(FMT);
            Path backup = dir.resolve(timestamp + ".json");
            Files.write(backup, currentBytes);
            pruneOldBackups(dir);
        } catch (IOException e) {
            logger.warning("Failed to backup scene " + sceneId + ": " + e.getMessage());
        }
    }

    public void backupStructure(String structureId, byte[] currentBytes) {
        if (maxBackups <= 0 || currentBytes == null || currentBytes.length == 0) return;
        String safeName = structureId.replace(':', '_').replace('/', '_');
        Path dir = backupRoot.resolve("structures").resolve(safeName);
        try {
            Files.createDirectories(dir);
            String timestamp = LocalDateTime.now().format(FMT);
            Path backup = dir.resolve(timestamp + ".nbt");
            Files.write(backup, currentBytes);
            pruneOldBackups(dir);
        } catch (IOException e) {
            logger.warning("Failed to backup structure " + structureId + ": " + e.getMessage());
        }
    }

    private void pruneOldBackups(Path dir) throws IOException {
        List<Path> backups = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile).forEach(backups::add);
        }
        backups.sort(Comparator.comparing(Path::toString));
        while (backups.size() > maxBackups) {
            Files.deleteIfExists(backups.remove(0));
        }
    }
}