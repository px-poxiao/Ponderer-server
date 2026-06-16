package com.ponderer.server.permissions;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.MetaNode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;

import java.util.UUID;
import java.util.logging.Logger;

public final class PermissionManager {

    // Permission nodes
    public static final String PERM_PULL             = "ponderer.pull";
    public static final String PERM_UPLOAD           = "ponderer.upload";
    public static final String PERM_ADMIN            = "ponderer.admin";
    public static final String PERM_CREATE_REGION    = "ponderer.create.region";
    public static final String PERM_BLUEPRINT        = "ponderer.blueprint";
    public static final String PERM_PACK_IMPORT      = "ponderer.pack.import";
    public static final String PERM_PACK_EXPORT      = "ponderer.pack.export";
    public static final String PERM_AI_SERVER_API    = "ponderer.ai.use_server_api";
    public static final String PERM_ADMIN_TOPUP      = "ponderer.admin.topup";
    public static final String PERM_REPORT           = "ponderer.report";
    public static final String PERM_LOCK             = "ponderer.lock";
    public static final String PERM_ADMIN_REVIEW     = "ponderer.admin.review";
    public static final String PERM_ADMIN_GROUPS     = "ponderer.admin.groups";
    public static final String PERM_ADMIN_COLLAB     = "ponderer.admin.collab";
    public static final String PERM_ADMIN_REPORTS    = "ponderer.admin.reports";

    // Meta keys
    public static final String META_UPLOAD_LIMIT     = "ponderer-upload-limit";

    private final LuckPerms luckPerms;
    private final Logger logger;

    public PermissionManager(ServicesManager services, Logger logger) {
        RegisteredServiceProvider<LuckPerms> provider = services.getRegistration(LuckPerms.class);
        if (provider == null) throw new IllegalStateException("LuckPerms not found");
        this.luckPerms = provider.getProvider();
        this.logger = logger;
    }

    public boolean canPull(Player player) {
        return player.hasPermission(PERM_PULL);
    }

    public boolean canUpload(Player player) {
        return player.hasPermission(PERM_UPLOAD);
    }

    public boolean isAdmin(Player player) {
        return player.hasPermission(PERM_ADMIN);
    }

    public boolean canCreateRegion(Player player) {
        return player.hasPermission(PERM_CREATE_REGION);
    }

    public boolean canUseBlueprint(Player player) {
        return player.hasPermission(PERM_BLUEPRINT);
    }

    public boolean canUseServerApi(Player player) {
        return player.hasPermission(PERM_AI_SERVER_API);
    }

    public boolean canTopup(Player player) {
        return player.hasPermission(PERM_ADMIN_TOPUP);
    }

    /** Returns -1 for unlimited, 0 if no meta set (use server default). */
    public int getUploadLimit(UUID uuid) {
        User user = luckPerms.getUserManager().getUser(uuid);
        if (user == null) return 0;
        String raw = user.getCachedData().getMetaData().getMetaValue(META_UPLOAD_LIMIT);
        if (raw == null) return 0;
        try { return Integer.parseInt(raw); } catch (NumberFormatException e) { return 0; }
    }

    public void setUploadLimit(UUID uuid, int limit) {
        luckPerms.getUserManager().loadUser(uuid).thenAccept(user -> {
            if (user == null) return;
            user.data().clear(n -> n instanceof MetaNode mn && META_UPLOAD_LIMIT.equals(mn.getMetaKey()));
            user.data().add(MetaNode.builder(META_UPLOAD_LIMIT, String.valueOf(limit)).build());
            luckPerms.getUserManager().saveUser(user);
        });
    }

    public void grantPermission(UUID uuid, String permission) {
        luckPerms.getUserManager().loadUser(uuid).thenAccept(user -> {
            if (user == null) return;
            user.data().add(Node.builder(permission).build());
            luckPerms.getUserManager().saveUser(user);
        });
    }

    public void revokePermission(UUID uuid, String permission) {
        luckPerms.getUserManager().loadUser(uuid).thenAccept(user -> {
            if (user == null) return;
            user.data().remove(Node.builder(permission).build());
            luckPerms.getUserManager().saveUser(user);
        });
    }
}