package com.ponderer.server.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ponderer.server.config.MessageConfig;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

public final class LockStore {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private final Path dataFile;
    private final Logger logger;
    private Map<String, String> locks; // sceneId -> ownerUuid

    public LockStore(Path worldRoot, Logger logger) {
        this.dataFile = worldRoot.resolve("ponderer").resolve("scene_locks.json");
        this.logger = logger;
        this.locks = load();
    }

    public boolean isLocked(String sceneId) { return locks.containsKey(sceneId); }

    public void lock(String sceneId, UUID owner) { locks.put(sceneId, owner.toString()); save(); }

    public void unlock(String sceneId) { locks.remove(sceneId); save(); }

    public UUID getLockOwner(String sceneId) {
        String raw = locks.get(sceneId);
        if (raw == null) return null;
        try { return UUID.fromString(raw); } catch (IllegalArgumentException e) { return null; }
    }

    private Map<String, String> load() {
        if (!Files.exists(dataFile)) return new HashMap<>();
        try { Map<String, String> m = GSON.fromJson(Files.readString(dataFile), MAP_TYPE); return m != null ? m : new HashMap<>(); }
        catch (IOException e) { logger.warning(MessageConfig.global("log_locks_load_failed", e.getMessage())); return new HashMap<>(); }
    }

    private void save() {
        try { Files.createDirectories(dataFile.getParent()); Files.writeString(dataFile, GSON.toJson(locks)); }
        catch (IOException e) { logger.warning(MessageConfig.global("log_locks_save_failed", e.getMessage())); }
    }
}
