package com.ponderer.server.storage;

import com.ponderer.server.config.MessageConfig;
import com.ponderer.server.config.PluginConfig;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

public final class ExpiryService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final SceneAccessStore accessStore;
    private final SceneStore sceneStore;
    private final PluginConfig config;
    private final Path worldRoot;
    private final Plugin plugin;
    private final Logger logger;

    public ExpiryService(SceneAccessStore accessStore, SceneStore sceneStore,
                         PluginConfig config, Path worldRoot, Plugin plugin, Logger logger) {
        this.accessStore = accessStore;
        this.sceneStore = sceneStore;
        this.config = config;
        this.worldRoot = worldRoot;
        this.plugin = plugin;
        this.logger = logger;
    }

    public void start() {
        if (!config.isExpiryEnabled()) return;
        long dayTicks = 24L * 72000L;
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::runExpiry, dayTicks, dayTicks);
        logger.info(MessageConfig.global("log_expiry_enabled", config.getExpiryDays()));
    }

    private void runExpiry() {
        List<String> expired = accessStore.getExpired(config.getExpiryDays());
        if (expired.isEmpty()) return;

        Path archiveDir = worldRoot.resolve("ponderer").resolve(".archive");
        String timestamp = LocalDateTime.now().format(FMT);

        for (String sceneId : expired) {
            byte[] bytes = sceneStore.readScene(sceneId);
            if (bytes == null) { accessStore.remove(sceneId); continue; }

            String safeName = sceneId.replace(':', '_').replace('/', '_');
            Path dest = archiveDir.resolve(safeName + "_" + timestamp + ".json");
            try {
                Files.createDirectories(archiveDir);
                Files.write(dest, bytes);
                sceneStore.deleteScene(sceneId);
                accessStore.remove(sceneId);
                logger.info(MessageConfig.global("log_expiry_archived", sceneId));
            } catch (IOException e) {
                logger.warning(MessageConfig.global("log_expiry_archive_failed", sceneId, e.getMessage()));
            }
        }
    }
}
