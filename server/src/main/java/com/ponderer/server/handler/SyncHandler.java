package com.ponderer.server.handler;

import com.ponderer.server.config.MessageConfig;
import com.ponderer.server.config.PluginConfig;
import com.ponderer.server.network.packets.SyncResponsePacket;
import com.ponderer.server.permissions.PermissionManager;
import com.ponderer.server.storage.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SyncHandler {

    private static final Pattern RESOURCE_ID =
            Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private final SceneStore sceneStore;
    private final PermissionManager permissions;
    private final MessageConfig messages;
    private final Plugin plugin;
    private final VisibilityStore visibilityStore;
    private final ReviewStore reviewStore;
    private final SceneAccessStore accessStore;
    private final SceneOwnerStore ownerStore;
    private final PluginConfig config;

    public SyncHandler(SceneStore sceneStore, PermissionManager permissions, MessageConfig messages,
                       Plugin plugin, VisibilityStore visibilityStore, ReviewStore reviewStore,
                       SceneAccessStore accessStore, SceneOwnerStore ownerStore, PluginConfig config) {
        this.sceneStore = sceneStore;
        this.permissions = permissions;
        this.messages = messages;
        this.plugin = plugin;
        this.visibilityStore = visibilityStore;
        this.reviewStore = reviewStore;
        this.accessStore = accessStore;
        this.ownerStore = ownerStore;
        this.config = config;
    }

    public void handle(Player player) {
        if (!config.isSyncEnabled()) {
            player.sendMessage(messages.get("feature_disabled", "sync"));
            return;
        }
        if (!permissions.canPull(player)) {
            player.sendMessage(messages.get("no_pull_permission"));
            return;
        }

        sendCurrentState(player);
    }

    public void sendCurrentState(Player player) {
        List<SyncResponsePacket.FileEntry> scripts = sceneStore.collectScripts();

        // Filter by visibility and review status
        boolean autoReview = "auto".equals(config.getReviewMode());
        List<SyncResponsePacket.FileEntry> filteredScripts = scripts.stream()
                .filter(e -> {
                    if (!autoReview && !reviewStore.isApproved(e.id())) return false;
                    if (!config.isVisibilityEnabled()) return true;
                    var owner = ownerStore.getOwner(e.id());
                    return visibilityStore.canSee(e.id(), player.getUniqueId(), owner != null ? owner : player.getUniqueId());
                })
                .peek(e -> accessStore.recordAccess(e.id()))
                .toList();

        Set<String> referencedStructures = referencedResourceIds(filteredScripts);
        List<SyncResponsePacket.FileEntry> filteredStructures = sceneStore.collectStructures().stream()
                .filter(e -> referencedStructures.contains(e.id()))
                .toList();

        player.sendPluginMessage(plugin, SyncResponsePacket.CHANNEL,
                new SyncResponsePacket(filteredScripts, filteredStructures).encode());
    }

    private Set<String> referencedResourceIds(List<SyncResponsePacket.FileEntry> scripts) {
        Set<String> ids = new HashSet<>();
        for (SyncResponsePacket.FileEntry entry : scripts) {
            String json = new String(entry.bytes(), StandardCharsets.UTF_8);
            Matcher matcher = RESOURCE_ID.matcher(json);
            while (matcher.find()) {
                ids.add(matcher.group());
            }
        }
        return ids;
    }
}
