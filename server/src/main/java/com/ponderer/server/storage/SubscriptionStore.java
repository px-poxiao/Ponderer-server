package com.ponderer.server.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

public final class SubscriptionStore {

    private static final Gson GSON = new Gson();
    private static final Type LIST_MAP_TYPE = new TypeToken<Map<String, List<String>>>() {}.getType();

    private final Path subFile;
    private final Path notifFile;
    private final Logger logger;
    private Map<String, List<String>> subscriptions;   // sceneId -> [uuids]
    private Map<String, List<String>> pendingNotifs;   // uuid -> [messages]

    public SubscriptionStore(Path worldRoot, Logger logger) {
        this.subFile   = worldRoot.resolve("ponderer").resolve("subscriptions.json");
        this.notifFile = worldRoot.resolve("ponderer").resolve("pending_notifications.json");
        this.logger = logger;
        this.subscriptions = loadFile(subFile);
        this.pendingNotifs = loadFile(notifFile);
    }

    public void subscribe(String sceneId, UUID uuid) {
        List<String> list = subscriptions.computeIfAbsent(sceneId, k -> new ArrayList<>());
        if (!list.contains(uuid.toString())) { list.add(uuid.toString()); saveSub(); }
    }

    public void unsubscribe(String sceneId, UUID uuid) {
        List<String> list = subscriptions.get(sceneId);
        if (list != null) { list.remove(uuid.toString()); saveSub(); }
    }

    public List<UUID> getSubscribers(String sceneId) {
        return subscriptions.getOrDefault(sceneId, List.of()).stream()
                .map(s -> { try { return UUID.fromString(s); } catch (Exception e) { return null; } })
                .filter(Objects::nonNull).toList();
    }

    public void queueNotification(UUID uuid, String message) {
        pendingNotifs.computeIfAbsent(uuid.toString(), k -> new ArrayList<>()).add(message); saveNotif();
    }

    public List<String> drainNotifications(UUID uuid) {
        List<String> msgs = pendingNotifs.remove(uuid.toString());
        if (msgs != null && !msgs.isEmpty()) saveNotif();
        return msgs != null ? msgs : List.of();
    }

    public boolean hasPending(UUID uuid) {
        List<String> list = pendingNotifs.get(uuid.toString());
        return list != null && !list.isEmpty();
    }

    private Map<String, List<String>> loadFile(Path path) {
        if (!Files.exists(path)) return new HashMap<>();
        try { Map<String, List<String>> m = GSON.fromJson(Files.readString(path), LIST_MAP_TYPE); return m != null ? m : new HashMap<>(); }
        catch (IOException e) { logger.warning("Failed to load " + path.getFileName() + ": " + e.getMessage()); return new HashMap<>(); }
    }

    private void saveSub() {
        try { Files.createDirectories(subFile.getParent()); Files.writeString(subFile, GSON.toJson(subscriptions)); }
        catch (IOException e) { logger.warning("Failed to save subscriptions: " + e.getMessage()); }
    }

    private void saveNotif() {
        try { Files.createDirectories(notifFile.getParent()); Files.writeString(notifFile, GSON.toJson(pendingNotifs)); }
        catch (IOException e) { logger.warning("Failed to save notifications: " + e.getMessage()); }
    }
}
