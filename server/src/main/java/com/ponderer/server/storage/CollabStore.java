package com.ponderer.server.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

public final class CollabStore {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, List<String>>>() {}.getType();

    private final Path dataFile;
    private final Logger logger;
    private Map<String, List<String>> data; // "scene:<id>" or "global:<ownerUuid>" -> [uuids]

    public CollabStore(Path worldRoot, Logger logger) {
        this.dataFile = worldRoot.resolve("ponderer").resolve("scene_collabs.json");
        this.logger = logger;
        this.data = load();
    }

    public boolean isCollaborator(String sceneId, UUID ownerUuid, UUID candidate) {
        String c = candidate.toString();
        List<String> sceneCollabs = data.getOrDefault("scene:" + sceneId, List.of());
        if (sceneCollabs.contains(c)) return true;
        if (ownerUuid != null) {
            List<String> globalCollabs = data.getOrDefault("global:" + ownerUuid, List.of());
            if (globalCollabs.contains(c)) return true;
        }
        return false;
    }

    public void addSceneCollab(String sceneId, UUID uuid) {
        data.computeIfAbsent("scene:" + sceneId, k -> new ArrayList<>()).add(uuid.toString()); save();
    }

    public void removeSceneCollab(String sceneId, UUID uuid) {
        List<String> list = data.get("scene:" + sceneId);
        if (list != null) { list.remove(uuid.toString()); save(); }
    }

    public void addGlobalCollab(UUID owner, UUID collab) {
        data.computeIfAbsent("global:" + owner, k -> new ArrayList<>()).add(collab.toString()); save();
    }

    public void removeGlobalCollab(UUID owner, UUID collab) {
        List<String> list = data.get("global:" + owner);
        if (list != null) { list.remove(collab.toString()); save(); }
    }

    public List<UUID> getSceneCollabs(String sceneId) {
        return data.getOrDefault("scene:" + sceneId, List.of()).stream()
                .map(s -> { try { return UUID.fromString(s); } catch (Exception e) { return null; } })
                .filter(Objects::nonNull).toList();
    }

    public List<UUID> getGlobalCollabs(UUID owner) {
        return data.getOrDefault("global:" + owner, List.of()).stream()
                .map(s -> { try { return UUID.fromString(s); } catch (Exception e) { return null; } })
                .filter(Objects::nonNull).toList();
    }

    private Map<String, List<String>> load() {
        if (!Files.exists(dataFile)) return new HashMap<>();
        try { Map<String, List<String>> m = GSON.fromJson(Files.readString(dataFile), MAP_TYPE); return m != null ? m : new HashMap<>(); }
        catch (IOException e) { logger.warning("Failed to load collabs: " + e.getMessage()); return new HashMap<>(); }
    }

    private void save() {
        try { Files.createDirectories(dataFile.getParent()); Files.writeString(dataFile, GSON.toJson(data)); }
        catch (IOException e) { logger.warning("Failed to save collabs: " + e.getMessage()); }
    }
}
