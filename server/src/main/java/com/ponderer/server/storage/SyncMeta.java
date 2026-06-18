package com.ponderer.server.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ponderer.server.config.MessageConfig;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class SyncMeta {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private final Path metaFile;
    private final Logger logger;
    private Map<String, String> hashes;

    public SyncMeta(Path worldRoot, Logger logger) {
        this.metaFile = worldRoot.resolve("ponderer").resolve(".sync_hashes.json");
        this.logger = logger;
        this.hashes = load();
    }

    public String getHash(String key) {
        return hashes.getOrDefault(key, "");
    }

    public void putHash(String key, String hash) {
        hashes.put(key, hash);
        save();
    }

    public void removeHash(String key) {
        hashes.remove(key);
        save();
    }

    public static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private Map<String, String> load() {
        if (!Files.exists(metaFile)) return new HashMap<>();
        try {
            String json = Files.readString(metaFile);
            Map<String, String> map = GSON.fromJson(json, MAP_TYPE);
            return map != null ? map : new HashMap<>();
        } catch (IOException e) {
            logger.warning(MessageConfig.global("log_sync_hashes_load_failed", e.getMessage()));
            return new HashMap<>();
        }
    }

    private void save() {
        try {
            Files.createDirectories(metaFile.getParent());
            Files.writeString(metaFile, GSON.toJson(hashes));
        } catch (IOException e) {
            logger.warning(MessageConfig.global("log_sync_hashes_save_failed", e.getMessage()));
        }
    }
}
