package com.ponderer.server.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ponderer.server.config.MessageConfig;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

public final class ReviewStore {

    public static final class ReviewEntry {
        public String sceneId;
        public String ownerUuid;
        public String json;
        public List<StructureEntry> structures;
        public String status; // "pending" | "approved" | "rejected"
        public String rejectionReason;
        public long submittedAt;
    }

    public static final class StructureEntry {
        public String id;
        public byte[] bytes;
        public StructureEntry(String id, byte[] bytes) { this.id = id; this.bytes = bytes; }
    }

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, ReviewEntry>>() {}.getType();

    private final Path dataFile;
    private final Logger logger;
    private Map<String, ReviewEntry> queue;

    public ReviewStore(Path worldRoot, Logger logger) {
        this.dataFile = worldRoot.resolve("ponderer").resolve("review_queue.json");
        this.logger = logger;
        this.queue = load();
    }

    public void enqueue(String sceneId, String ownerUuid, String json, List<StructureEntry> structures) {
        ReviewEntry e = new ReviewEntry();
        e.sceneId = sceneId; e.ownerUuid = ownerUuid; e.json = json;
        e.structures = structures; e.status = "pending"; e.submittedAt = System.currentTimeMillis();
        queue.put(sceneId, e);
        save();
    }

    public void approve(String sceneId) {
        ReviewEntry e = queue.get(sceneId);
        if (e != null) { e.status = "approved"; save(); }
    }

    public void reject(String sceneId, String reason) {
        ReviewEntry e = queue.get(sceneId);
        if (e != null) { e.status = "rejected"; e.rejectionReason = reason; save(); }
    }

    public List<ReviewEntry> getPending() {
        return queue.values().stream().filter(e -> "pending".equals(e.status)).toList();
    }

    public ReviewEntry getEntry(String sceneId) { return queue.get(sceneId); }

    public boolean isApproved(String sceneId) {
        ReviewEntry e = queue.get(sceneId);
        return e == null || "approved".equals(e.status); // not in queue = auto-approved
    }

    public void remove(String sceneId) { queue.remove(sceneId); save(); }

    private Map<String, ReviewEntry> load() {
        if (!Files.exists(dataFile)) return new HashMap<>();
        try {
            Map<String, ReviewEntry> map = GSON.fromJson(Files.readString(dataFile), MAP_TYPE);
            return map != null ? map : new HashMap<>();
        } catch (IOException e) { logger.warning(MessageConfig.global("log_review_queue_load_failed", e.getMessage())); return new HashMap<>(); }
    }

    private void save() {
        try { Files.createDirectories(dataFile.getParent()); Files.writeString(dataFile, GSON.toJson(queue)); }
        catch (IOException e) { logger.warning(MessageConfig.global("log_review_queue_save_failed", e.getMessage())); }
    }
}
