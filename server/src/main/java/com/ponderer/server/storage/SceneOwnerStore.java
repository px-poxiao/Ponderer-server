package com.ponderer.server.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ponderer.server.config.MessageConfig;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public final class SceneOwnerStore {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private final Path dataFile;
    private final Logger logger;
    private Map<String, String> owners; // sceneId -> ownerUUID

    public SceneOwnerStore(Path worldRoot, Logger logger) {
        this.dataFile = worldRoot.resolve("ponderer").resolve("scene_owners.json");
        this.logger = logger;
        this.owners = load();
    }

    /** Returns the owner UUID of a scene, or null if not recorded. */
    public UUID getOwner(String sceneId) {
        String raw = owners.get(sceneId);
        if (raw == null) return null;
        try { return UUID.fromString(raw); } catch (IllegalArgumentException e) { return null; }
    }

    public void setOwner(String sceneId, UUID owner) {
        owners.put(sceneId, owner.toString());
        save();
    }

    public void removeOwner(String sceneId) {
        owners.remove(sceneId);
        save();
    }

    public boolean isOwner(String sceneId, UUID uuid) {
        UUID owner = getOwner(sceneId);
        return owner != null && owner.equals(uuid);
    }

    /** Returns true if the scene has no recorded owner (e.g. manually placed by admin). */
    public boolean hasNoOwner(String sceneId) {
        return !owners.containsKey(sceneId);
    }

    private Map<String, String> load() {
        if (!Files.exists(dataFile)) return new HashMap<>();
        try {
            Map<String, String> map = GSON.fromJson(Files.readString(dataFile), MAP_TYPE);
            return map != null ? map : new HashMap<>();
        } catch (IOException e) {
            logger.warning(MessageConfig.global("log_scene_owners_load_failed", e.getMessage()));
            return new HashMap<>();
        }
    }

    private void save() {
        try {
            Files.createDirectories(dataFile.getParent());
            Files.writeString(dataFile, GSON.toJson(owners));
        } catch (IOException e) {
            logger.warning(MessageConfig.global("log_scene_owners_save_failed", e.getMessage()));
        }
    }
}
