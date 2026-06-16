package com.ponderer.server.handler;

import com.ponderer.server.config.MessageConfig;
import com.ponderer.server.config.PluginConfig;
import com.ponderer.server.network.packets.DownloadStructureResultPacket;
import com.ponderer.server.network.packets.SyncResponsePacket;
import com.ponderer.server.permissions.PermissionManager;
import com.ponderer.server.storage.SceneStore;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DownloadStructureHandler {

    private final SceneStore sceneStore;
    private final PermissionManager permissions;
    private final MessageConfig messages;
    private final PluginConfig config;
    private final Plugin plugin;

    public DownloadStructureHandler(SceneStore sceneStore, PermissionManager permissions,
                                    MessageConfig messages, PluginConfig config, Plugin plugin) {
        this.sceneStore = sceneStore;
        this.permissions = permissions;
        this.messages = messages;
        this.config = config;
        this.plugin = plugin;
    }

    public void handle(Player player, String sourceId) {
        if (!config.isStructureImportEnabled()) {
            sendResult(player, new DownloadStructureResultPacket(sourceId, "", false, "Feature disabled"));
            player.sendMessage(messages.get("feature_disabled", "structure_import"));
            return;
        }
        if (!permissions.canUpload(player)) {
            sendResult(player, new DownloadStructureResultPacket(sourceId, "", false, "No permission"));
            return;
        }

        if (sourceId == null || !sourceId.contains(":")) {
            sendResult(player, new DownloadStructureResultPacket(sourceId, "", false, "Invalid structure id"));
            return;
        }

        Path sourcePath = resolveSourcePath(sourceId);
        if (sourcePath == null || !Files.exists(sourcePath)) {
            sendResult(player, new DownloadStructureResultPacket(sourceId, "", false, "Structure not found"));
            player.sendMessage(messages.get("structure_not_found", sourceId));
            return;
        }

        int colon = sourceId.indexOf(':');
        String targetId = "ponderer:" + sourceId.substring(colon + 1);

        try {
            byte[] bytes = Files.readAllBytes(sourcePath);
            boolean ok = sceneStore.saveStructure(targetId, bytes);
            if (!ok) {
                sendResult(player, new DownloadStructureResultPacket(sourceId, targetId, false, "Import failed"));
                return;
            }

            List<SyncResponsePacket.FileEntry> scripts = sceneStore.collectScripts();
            List<SyncResponsePacket.FileEntry> structures = sceneStore.collectStructures();
            player.sendPluginMessage(plugin, SyncResponsePacket.CHANNEL,
                    new SyncResponsePacket(scripts, structures).encode());

            sendResult(player, new DownloadStructureResultPacket(sourceId, targetId, true, "OK"));
            player.sendMessage(messages.get("structure_import_success", sourceId, targetId));
        } catch (Exception e) {
            sendResult(player, new DownloadStructureResultPacket(sourceId, targetId, false, "Read failed"));
        }
    }

    private Path resolveSourcePath(String sourceId) {
        Path serverPath = sceneStore.resolveStructurePath(sourceId);
        if (serverPath != null && Files.exists(serverPath)) return serverPath;

        int colon = sourceId.indexOf(':');
        String namespace = sourceId.substring(0, colon);
        String path = sourceId.substring(colon + 1);
        Path generated = sceneStore.getStructureDir().getParent().getParent()
                .resolve("generated").resolve(namespace).resolve("structures").resolve(path + ".nbt");
        if (Files.exists(generated)) return generated;

        return null;
    }

    private void sendResult(Player player, DownloadStructureResultPacket packet) {
        player.sendPluginMessage(plugin, DownloadStructureResultPacket.CHANNEL, packet.encode());
    }
}
