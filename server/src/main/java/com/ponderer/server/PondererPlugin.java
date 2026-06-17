package com.ponderer.server;

import com.ponderer.server.ai.AiProxyHandler;
import com.ponderer.server.commands.PondererAdminCommand;
import com.ponderer.server.commands.PondererCommand;
import com.ponderer.server.config.MessageConfig;
import com.ponderer.server.config.PluginConfig;
import com.ponderer.server.handler.DownloadStructureHandler;
import com.ponderer.server.handler.ReviewAiService;
import com.ponderer.server.handler.SyncHandler;
import com.ponderer.server.handler.UploadHandler;
import com.ponderer.server.listener.PlayerJoinListener;
import com.ponderer.server.network.PacketListener;
import com.ponderer.server.network.packets.*;
import com.ponderer.server.permissions.PermissionManager;
import com.ponderer.server.ratelimit.RateLimiter;
import com.ponderer.server.stats.StatsTracker;
import com.ponderer.server.storage.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

public final class PondererPlugin extends JavaPlugin {

    private PluginConfig config;
    private MessageConfig messages;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
        config = new PluginConfig(getConfig());
        messages = new MessageConfig(this);

        Path worldRoot = getServer().getWorldContainer().toPath()
                .resolve(getServer().getWorlds().get(0).getName());

        // Core stores
        SceneStore sceneStore           = new SceneStore(worldRoot, getLogger());
        SyncMeta syncMeta               = new SyncMeta(worldRoot, getLogger());
        PlayerDataStore playerData      = new PlayerDataStore(worldRoot, getLogger());
        BackupManager backupManager     = new BackupManager(worldRoot, config.getMaxBackups(), getLogger());
        StatsTracker stats              = new StatsTracker(worldRoot, getLogger());
        SceneOwnerStore ownerStore      = new SceneOwnerStore(worldRoot, getLogger());

        // New stores
        ReviewStore reviewStore         = new ReviewStore(worldRoot, getLogger());
        VisibilityStore visibilityStore = new VisibilityStore(worldRoot, getLogger());
        CollabStore collabStore         = new CollabStore(worldRoot, getLogger());
        ReportStore reportStore         = new ReportStore(worldRoot, getLogger());
        SubscriptionStore subscriptions = new SubscriptionStore(worldRoot, getLogger());
        LockStore lockStore             = new LockStore(worldRoot, getLogger());
        SceneAccessStore accessStore    = new SceneAccessStore(worldRoot, getLogger());
        PushCooldownStore pushCooldown  = new PushCooldownStore();

        PermissionManager permissions;
        try {
            permissions = new PermissionManager(getServer().getServicesManager(), getLogger());
        } catch (IllegalStateException e) {
            getLogger().severe("LuckPerms not found! Disabling Ponderer plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        RateLimiter rateLimiter = new RateLimiter(config.getUploadsPerHour());

        // Handlers
        SyncHandler syncHandler = new SyncHandler(sceneStore, permissions, messages, this,
                visibilityStore, reviewStore, accessStore, ownerStore, config);

        UploadHandler uploadHandler = new UploadHandler(sceneStore, syncMeta, backupManager,
                permissions, rateLimiter, playerData, stats, config, ownerStore, messages, this,
                collabStore, reviewStore, lockStore, pushCooldown, subscriptions);

        // ReviewAiService — set via setter to avoid circular dependency
        ReviewAiService reviewAi = new ReviewAiService(config, reviewStore, uploadHandler, this, getLogger());
        uploadHandler.setReviewAi(reviewAi);

        DownloadStructureHandler downloadHandler = new DownloadStructureHandler(sceneStore, permissions, messages, config, this, syncHandler);
        AiProxyHandler aiHandler = new AiProxyHandler(config, permissions, playerData, stats, messages, this, getLogger());

        PacketListener packetListener = new PacketListener(syncHandler, uploadHandler, downloadHandler, aiHandler, getLogger());
        registerChannels(packetListener);

        // Admin command
        PondererAdminCommand adminCmd = new PondererAdminCommand(
                permissions, playerData, stats, sceneStore, messages,
                reviewStore, visibilityStore, collabStore, reportStore,
                uploadHandler, backupManager, config, this);
        var cmd = getCommand("ponderer-admin");
        if (cmd != null) {
            cmd.setExecutor(adminCmd);
            cmd.setTabCompleter(adminCmd);
        }

        // Player command
        PondererCommand pondererCmd = new PondererCommand(permissions, messages, config, this,
                lockStore, ownerStore, subscriptions, reportStore, visibilityStore);
        var pcmd = getCommand("ponderer");
        if (pcmd != null) {
            pcmd.setExecutor(pondererCmd);
            pcmd.setTabCompleter(pondererCmd);
        }

        // Listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(subscriptions, config), this);

        // Scheduled services
        new ScheduledBackupService(worldRoot, this, config, getLogger()).start();
        new ExpiryService(accessStore, sceneStore, config, worldRoot, this, getLogger()).start();

        getLogger().info("PondererServer enabled. World root: " + worldRoot);
    }

    private void registerChannels(PacketListener listener) {
        var messenger = getServer().getMessenger();
        messenger.registerIncomingPluginChannel(this, SyncRequestPacket.CHANNEL, listener);
        messenger.registerIncomingPluginChannel(this, UploadScenePacket.CHANNEL, listener);
        messenger.registerIncomingPluginChannel(this, DownloadStructurePacket.CHANNEL, listener);
        messenger.registerIncomingPluginChannel(this, AiRequestPacket.CHANNEL, listener);
        messenger.registerOutgoingPluginChannel(this, SyncResponsePacket.CHANNEL);
        messenger.registerOutgoingPluginChannel(this, UploadResponsePacket.CHANNEL);
        messenger.registerOutgoingPluginChannel(this, DownloadStructureResultPacket.CHANNEL);
        messenger.registerOutgoingPluginChannel(this, AiResponsePacket.CHANNEL);
        messenger.registerOutgoingPluginChannel(this, ClientCommandPacket.CHANNEL);
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getLogger().info("PondererServer disabled.");
    }
}
