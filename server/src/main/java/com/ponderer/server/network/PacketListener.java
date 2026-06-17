package com.ponderer.server.network;

import com.ponderer.server.ai.AiProxyHandler;
import com.ponderer.server.handler.DownloadStructureHandler;
import com.ponderer.server.handler.SyncHandler;
import com.ponderer.server.handler.UploadHandler;
import com.ponderer.server.network.packets.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.logging.Logger;

public final class PacketListener implements PluginMessageListener {

    private final SyncHandler syncHandler;
    private final UploadHandler uploadHandler;
    private final DownloadStructureHandler downloadHandler;
    private final AiProxyHandler aiHandler;
    private final Logger logger;

    public PacketListener(SyncHandler syncHandler, UploadHandler uploadHandler,
                          DownloadStructureHandler downloadHandler, AiProxyHandler aiHandler,
                          Logger logger) {
        this.syncHandler = syncHandler;
        this.uploadHandler = uploadHandler;
        this.downloadHandler = downloadHandler;
        this.aiHandler = aiHandler;
        this.logger = logger;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        try {
            switch (channel) {
                case SyncRequestPacket.CHANNEL ->
                        syncHandler.handle(player);
                case UploadScenePacket.CHANNEL ->
                        uploadHandler.handle(player, UploadScenePacket.decode(message));
                case DownloadStructurePacket.CHANNEL ->
                        downloadHandler.handle(player, DownloadStructurePacket.decode(message).sourceId());
                case AiRequestPacket.CHANNEL ->
                        aiHandler.handle(player, AiRequestPacket.decode(message));
            }
        } catch (RuntimeException e) {
            logger.warning("[Ponderer] Ignored malformed plugin message on " + channel
                    + " from " + player.getName() + ": " + e.getMessage());
        }
    }
}
