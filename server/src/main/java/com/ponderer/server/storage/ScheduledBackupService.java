package com.ponderer.server.storage;

import com.ponderer.server.config.MessageConfig;
import com.ponderer.server.config.PluginConfig;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ScheduledBackupService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Path worldRoot;
    private final Plugin plugin;
    private final PluginConfig config;
    private final Logger logger;

    public ScheduledBackupService(Path worldRoot, Plugin plugin, PluginConfig config, Logger logger) {
        this.worldRoot = worldRoot;
        this.plugin = plugin;
        this.config = config;
        this.logger = logger;
    }

    public void start() {
        if (!config.isScheduledBackupEnabled()) return;
        long intervalTicks = (long) config.getScheduledBackupIntervalHours() * 72000L;
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::runBackup, intervalTicks, intervalTicks);
        logger.info(MessageConfig.global("log_scheduled_backup_enabled", config.getScheduledBackupIntervalHours()));
    }

    private void runBackup() {
        Path pondererDir = worldRoot.resolve("ponderer");
        Path outDir = pondererDir.resolve(".backups").resolve("full");
        String timestamp = LocalDateTime.now().format(FMT);
        Path zipPath = outDir.resolve(timestamp + ".zip");

        try {
            Files.createDirectories(outDir);
            try (OutputStream fos = Files.newOutputStream(zipPath);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {
                zipDirectory(pondererDir, pondererDir, outDir, zos);
            }
            logger.info(MessageConfig.global("log_full_backup_created", zipPath.getFileName()));
        } catch (IOException e) {
            logger.warning(MessageConfig.global("log_scheduled_backup_failed", e.getMessage()));
        }
    }

    private void zipDirectory(Path base, Path current, Path excludeDir, ZipOutputStream zos) throws IOException {
        try (var stream = Files.list(current)) {
            for (Path path : stream.toList()) {
                if (path.startsWith(excludeDir)) continue;
                if (Files.isDirectory(path)) {
                    zipDirectory(base, path, excludeDir, zos);
                } else {
                    String entryName = base.relativize(path).toString().replace('\\', '/');
                    zos.putNextEntry(new ZipEntry(entryName));
                    Files.copy(path, zos);
                    zos.closeEntry();
                }
            }
        }
    }
}
