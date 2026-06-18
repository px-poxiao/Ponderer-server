package com.ponderer.server.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ponderer.server.config.MessageConfig;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

public final class SceneAccessStore {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Long>>() {}.getType();

    private final Path dataFile;
    private final Logger logger;
    private Map<String, Long> access; // sceneId -> lastPulledEpochMillis

    public SceneAccessStore(Path worldRoot, Logger logger) {
        this.dataFile = worldRoot.resolve("ponderer").resolve("scene_access.json");
        this.logger = logger;
        this.access = load();
    }

    public void recordAccess(String sceneId) { access.put(sceneId, System.currentTimeMillis()); save(); }

    public long getLastAccess(String sceneId) { return access.getOrDefault(sceneId, 0L); }

    public List<String> getExpired(int days) {
        long cutoff = System.currentTimeMillis() - (long) days * 86400_000L;
        return access.entrySet().stream()
                .filter(e -> e.getValue() < cutoff)
                .map(Map.Entry::getKey).toList();
    }

    public void remove(String sceneId) { access.remove(sceneId); save(); }

    private Map<String, Long> load() {
        if (!Files.exists(dataFile)) return new HashMap<>();
        try { Map<String, Long> m = GSON.fromJson(Files.readString(dataFile), MAP_TYPE); return m != null ? m : new HashMap<>(); }
        catch (IOException e) { logger.warning(MessageConfig.global("log_scene_access_load_failed", e.getMessage())); return new HashMap<>(); }
    }

    private void save() {
        try { Files.createDirectories(dataFile.getParent()); Files.writeString(dataFile, GSON.toJson(access)); }
        catch (IOException e) { logger.warning(MessageConfig.global("log_scene_access_save_failed", e.getMessage())); }
    }
}
