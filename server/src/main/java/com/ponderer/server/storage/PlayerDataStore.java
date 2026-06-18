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

public final class PlayerDataStore {

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, PlayerData>>() {}.getType();

    private final Path dataFile;
    private final Logger logger;
    private Map<String, PlayerData> data;

    public PlayerDataStore(Path worldRoot, Logger logger) {
        this.dataFile = worldRoot.resolve("ponderer").resolve("player_data.json");
        this.logger = logger;
        this.data = load();
    }

    public int getUploadCount(UUID uuid) {
        PlayerData d = data.get(uuid.toString());
        return d == null ? 0 : d.uploadCount;
    }

    public void incrementUploadCount(UUID uuid) {
        data.computeIfAbsent(uuid.toString(), k -> new PlayerData()).uploadCount++;
        save();
    }

    public void setUploadCount(UUID uuid, int count) {
        data.computeIfAbsent(uuid.toString(), k -> new PlayerData()).uploadCount = count;
        save();
    }

    public long getRemainingTokens(UUID uuid) {
        PlayerData d = data.get(uuid.toString());
        return d == null ? 0L : d.remainingTokens;
    }

    public void setRemainingTokens(UUID uuid, long tokens) {
        data.computeIfAbsent(uuid.toString(), k -> new PlayerData()).remainingTokens = tokens;
        save();
    }

    public void deductTokens(UUID uuid, long amount) {
        PlayerData d = data.computeIfAbsent(uuid.toString(), k -> new PlayerData());
        d.remainingTokens = Math.max(0, d.remainingTokens - amount);
        save();
    }

    public void addTokens(UUID uuid, long amount) {
        data.computeIfAbsent(uuid.toString(), k -> new PlayerData()).remainingTokens += amount;
        save();
    }

    public int getTotalAiCalls(UUID uuid) {
        PlayerData d = data.get(uuid.toString());
        return d == null ? 0 : d.totalAiCalls;
    }

    public void incrementAiCalls(UUID uuid) {
        data.computeIfAbsent(uuid.toString(), k -> new PlayerData()).totalAiCalls++;
        save();
    }

    public PlayerData getData(UUID uuid) {
        return data.getOrDefault(uuid.toString(), new PlayerData());
    }

    public Map<String, PlayerData> getAllData() {
        return Map.copyOf(data);
    }

    private Map<String, PlayerData> load() {
        if (!Files.exists(dataFile)) return new HashMap<>();
        try {
            String json = Files.readString(dataFile);
            Map<String, PlayerData> map = GSON.fromJson(json, MAP_TYPE);
            return map != null ? map : new HashMap<>();
        } catch (IOException e) {
            logger.warning(MessageConfig.global("log_player_data_load_failed", e.getMessage()));
            return new HashMap<>();
        }
    }

    private void save() {
        try {
            Files.createDirectories(dataFile.getParent());
            Files.writeString(dataFile, GSON.toJson(data));
        } catch (IOException e) {
            logger.warning(MessageConfig.global("log_player_data_save_failed", e.getMessage()));
        }
    }

    public static class PlayerData {
        public int uploadCount = 0;
        public long remainingTokens = 0L;
        public int totalAiCalls = 0;
    }
}
