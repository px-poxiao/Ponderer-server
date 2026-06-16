package com.ponderer.server.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ponderer.server.config.MessageConfig;
import com.ponderer.server.config.PluginConfig;
import com.ponderer.server.network.packets.UploadResponsePacket;
import com.ponderer.server.network.packets.UploadScenePacket;
import com.ponderer.server.permissions.PermissionManager;
import com.ponderer.server.ratelimit.RateLimiter;
import com.ponderer.server.stats.StatsTracker;
import com.ponderer.server.storage.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.nio.charset.StandardCharsets;
import java.util.List;

public final class UploadHandler {

    private final SceneStore sceneStore;
    private final SyncMeta syncMeta;
    private final BackupManager backupManager;
    private final PermissionManager permissions;
    private final RateLimiter rateLimiter;
    private final PlayerDataStore playerData;
    private final StatsTracker stats;
    private final PluginConfig config;
    private final SceneOwnerStore ownerStore;
    private final MessageConfig messages;
    private final Plugin plugin;
    private final CollabStore collabStore;
    private final ReviewStore reviewStore;
    private final LockStore lockStore;
    private final PushCooldownStore pushCooldown;
    private final SubscriptionStore subscriptions;

    // Set by PondererPlugin after construction to avoid circular dependency
    private ReviewAiService reviewAi;

    public UploadHandler(SceneStore sceneStore, SyncMeta syncMeta, BackupManager backupManager,
                         PermissionManager permissions, RateLimiter rateLimiter,
                         PlayerDataStore playerData, StatsTracker stats,
                         PluginConfig config, SceneOwnerStore ownerStore,
                         MessageConfig messages, Plugin plugin,
                         CollabStore collabStore, ReviewStore reviewStore,
                         LockStore lockStore, PushCooldownStore pushCooldown,
                         SubscriptionStore subscriptions) {
        this.sceneStore = sceneStore;
        this.syncMeta = syncMeta;
        this.backupManager = backupManager;
        this.permissions = permissions;
        this.rateLimiter = rateLimiter;
        this.playerData = playerData;
        this.stats = stats;
        this.config = config;
        this.ownerStore = ownerStore;
        this.messages = messages;
        this.plugin = plugin;
        this.collabStore = collabStore;
        this.reviewStore = reviewStore;
        this.lockStore = lockStore;
        this.pushCooldown = pushCooldown;
        this.subscriptions = subscriptions;
    }

    public void setReviewAi(ReviewAiService reviewAi) { this.reviewAi = reviewAi; }

    public void handle(Player player, UploadScenePacket packet) {
        if (!config.isUploadEnabled()) {
            send(player, response(packet, "disabled"));
            player.sendMessage(messages.get("feature_disabled", "upload"));
            return;
        }

        if (!permissions.canUpload(player)) {
            send(player, response(packet, "error"));
            player.sendMessage(messages.get("no_upload_permission"));
            return;
        }

        if (!rateLimiter.tryAcquire(player.getUniqueId())) {
            send(player, response(packet, "error"));
            player.sendMessage(messages.get("upload_rate_limited"));
            return;
        }

        int limit = permissions.getUploadLimit(player.getUniqueId());
        if (limit == 0) limit = config.getDefaultUploadLimit();
        if (limit > 0) {
            int current = playerData.getUploadCount(player.getUniqueId());
            if (current >= limit) {
                send(player, response(packet, "error"));
                player.sendMessage(messages.get("upload_limit_reached", limit));
                return;
            }
        }

        if (config.isRegionPermissionRequired() && isRegionScene(packet.json()) && !permissions.canCreateRegion(player)) {
            send(player, response(packet, "error"));
            player.sendMessage(messages.get("no_region_permission"));
            return;
        }

        // Size check
        long maxBytes = config.getMaxSceneSizeBytes();
        if (maxBytes > 0 && packet.json().getBytes(StandardCharsets.UTF_8).length > maxBytes) {
            send(player, response(packet, "error"));
            player.sendMessage(messages.get("upload_too_large", maxBytes));
            return;
        }

        // Push cooldown
        int cooldown = config.getPushCooldownSeconds();
        if (cooldown > 0 && !pushCooldown.tryAcquire(player.getUniqueId(), cooldown)) {
            long remaining = pushCooldown.getRemainingSeconds(player.getUniqueId(), cooldown);
            send(player, response(packet, "cooldown:" + remaining));
            player.sendMessage(messages.get("upload_cooldown", remaining));
            return;
        }

        // Item filter
        String carrierItem = extractCarrierItem(packet.json());
        String filterMode = config.getSceneFilterMode();
        if (!"off".equals(filterMode) && carrierItem != null) {
            List<String> filterItems = config.getSceneFilterItems();
            boolean inList = filterItems.contains(carrierItem);
            if ("whitelist".equals(filterMode) && !inList) {
                send(player, response(packet, "error"));
                player.sendMessage(messages.get("upload_item_not_allowed", carrierItem));
                return;
            }
            if ("blacklist".equals(filterMode) && inList) {
                send(player, response(packet, "error"));
                player.sendMessage(messages.get("upload_item_not_allowed", carrierItem));
                return;
            }
        }

        // NBT restriction
        if (config.isNbtRestrictionEnabled()) {
            boolean hasNbt = hasCustomNbt(packet.json());
            String nbtMode = config.getNbtRestrictionMode();
            if ("whitelist".equals(nbtMode) && !hasNbt) {
                send(player, response(packet, "error"));
                player.sendMessage(messages.get("upload_nbt_required"));
                return;
            }
            if ("blacklist".equals(nbtMode) && hasNbt) {
                send(player, response(packet, "error"));
                player.sendMessage(messages.get("upload_nbt_forbidden"));
                return;
            }
        }

        // Lock check
        if (config.isLocksEnabled() && lockStore.isLocked(packet.sceneId()) && !permissions.isAdmin(player)) {
            send(player, response(packet, "error"));
            player.sendMessage(messages.get("scene_locked", packet.sceneId()));
            return;
        }

        // Ownership + collab check
        if (sceneStore.sceneExists(packet.sceneId()) && !ownerStore.hasNoOwner(packet.sceneId())) {
            boolean canEdit = ownerStore.isOwner(packet.sceneId(), player.getUniqueId())
                    || (config.isCollaborationEnabled()
                    && collabStore.isCollaborator(packet.sceneId(), ownerStore.getOwner(packet.sceneId()), player.getUniqueId()))
                    || permissions.isAdmin(player);
            if (!canEdit) {
                send(player, response(packet, "error"));
                player.sendMessage(messages.get("no_edit_permission"));
                return;
            }
        }

        // Conflict detection + hash skip
        String mode = packet.mode() == null ? "check" : packet.mode();
        String serverHash = getServerHash(packet.sceneId());
        String clientHash = packet.lastSyncHash() == null ? "" : packet.lastSyncHash();

        if ("force".equals(mode) && !config.isForcePushAllowed()) {
            send(player, response(packet, "error"));
            player.sendMessage(messages.get("upload_force_disabled"));
            return;
        }

        if (config.isConflictCheckEnabled() && !"force".equals(mode)) {
            // Skip if unchanged
            if (!serverHash.isEmpty() && serverHash.equals(clientHash)) {
                send(player, response(packet, "ok:" + serverHash));
                return;
            }
            // Conflict
            if (!serverHash.isEmpty() && !clientHash.isEmpty() && !serverHash.equals(clientHash)) {
                send(player, response(packet, "conflict"));
                player.sendMessage(messages.get("upload_conflict", packet.sceneId()));
                return;
            }
        }

        // Review routing
        String reviewMode = config.getReviewMode();
        if ("manual".equals(reviewMode)) {
            reviewStore.enqueue(packet.sceneId(), player.getUniqueId().toString(), packet.json(),
                    toReviewStructures(packet.structures()));
            send(player, response(packet, "pending"));
            player.sendMessage(messages.get("upload_pending_review", packet.sceneId()));
            return;
        }
        if ("ai".equals(reviewMode) && reviewAi != null) {
            reviewAi.review(packet, player);
            return;
        }

        // Auto: save directly
        doSave(player, packet);
    }

    /** Called by admin approve or ReviewAiService after approval. */
    public void commitApproved(ReviewStore.ReviewEntry entry, Player notifyPlayer) {
        UploadScenePacket packet = new UploadScenePacket(
                entry.sceneId, entry.json,
                entry.structures == null ? List.of() : entry.structures.stream()
                        .map(s -> new UploadScenePacket.StructureEntry(s.id, s.bytes)).toList(),
                "force", "");
        doSave(notifyPlayer, packet);
        reviewStore.remove(entry.sceneId);
    }

    private void doSave(Player player, UploadScenePacket packet) {
        byte[] existing = sceneStore.readScene(packet.sceneId());
        if (existing != null) {
            if (config.isBackupsEnabled()) backupManager.backupScene(packet.sceneId(), existing);
        } else {
            ownerStore.setOwner(packet.sceneId(), player.getUniqueId());
            playerData.incrementUploadCount(player.getUniqueId());
        }

        boolean ok = sceneStore.saveScene(packet.sceneId(), packet.json());

        if (ok && packet.structures() != null) {
            for (UploadScenePacket.StructureEntry entry : packet.structures()) {
                if (entry == null || entry.id() == null || entry.bytes() == null) continue;
                byte[] existingStruct = readStructure(entry.id());
                if (existingStruct != null && config.isBackupsEnabled()) backupManager.backupStructure(entry.id(), existingStruct);
                ok = sceneStore.saveStructure(entry.id(), entry.bytes()) && ok;
            }
        }

        if (ok) {
            String newHash = computeHash(packet.sceneId());
            syncMeta.putHash("scripts/" + packet.sceneId(), newHash);
            stats.recordUpload(player.getUniqueId(), packet.sceneId());
            send(player, response(packet, "ok:" + newHash));
            player.sendMessage(messages.get("upload_success", packet.sceneId()));

            // Notify subscribers
            if (config.isSubscriptionsEnabled()) {
                for (var sub : subscriptions.getSubscribers(packet.sceneId())) {
                    String msg = messages.get("scene_updated_notification", packet.sceneId(), player.getName());
                    var online = plugin.getServer().getPlayer(sub);
                    if (online != null) online.sendMessage(msg);
                    else subscriptions.queueNotification(sub, msg);
                }
            }
        } else {
            send(player, response(packet, "error"));
            player.sendMessage(messages.get("upload_failed", packet.sceneId()));
        }
    }

    private String getServerHash(String sceneId) {
        byte[] bytes = sceneStore.readScene(sceneId);
        if (bytes == null) return "";
        return SyncMeta.sha256(bytes);
    }

    private String computeHash(String sceneId) {
        byte[] bytes = sceneStore.readScene(sceneId);
        if (bytes == null) return "";
        return SyncMeta.sha256(bytes);
    }

    private byte[] readStructure(String structureId) {
        var path = sceneStore.resolveStructurePath(structureId);
        if (path == null) return null;
        try { return java.nio.file.Files.readAllBytes(path); } catch (Exception e) { return null; }
    }

    private boolean isRegionScene(String json) {
        return json != null && (json.contains("\"triggerMode\":\"coordinate\"") || json.contains("\"triggerMode\": \"coordinate\""));
    }

    private String extractCarrierItem(String json) {
        if (json == null) return null;
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("carrierItem")) return obj.get("carrierItem").getAsString();
            if (obj.has("item")) return obj.get("item").getAsString();
        } catch (Exception ignored) {}
        return null;
    }

    private boolean hasCustomNbt(String json) {
        if (json == null) return false;
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return obj.has("nbt") || obj.has("nbtFilter") || obj.has("tag");
        } catch (Exception ignored) {}
        return false;
    }

    private List<ReviewStore.StructureEntry> toReviewStructures(List<UploadScenePacket.StructureEntry> entries) {
        if (entries == null) return List.of();
        return entries.stream()
                .filter(e -> e != null && e.id() != null && e.bytes() != null)
                .map(e -> new ReviewStore.StructureEntry(e.id(), e.bytes()))
                .toList();
    }

    private void send(Player player, UploadResponsePacket packet) {
        player.sendPluginMessage(plugin, UploadResponsePacket.CHANNEL, packet.encode());
    }

    private UploadResponsePacket response(UploadScenePacket packet, String status) {
        return new UploadResponsePacket(packet.sceneId(), packet.pack(), status);
    }
}
