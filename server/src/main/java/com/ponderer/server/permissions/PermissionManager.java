package com.ponderer.server.permissions;

import com.ponderer.server.config.MessageConfig;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
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

    private final Object luckPerms;
    private final Logger logger;

    public PermissionManager(ServicesManager services, Logger logger) {
        this.logger = logger;
        this.luckPerms = findLuckPerms(services);
        if (luckPerms == null) {
            logger.info(MessageConfig.global("log_luckperms_missing"));
        } else {
            logger.info(MessageConfig.global("log_luckperms_found"));
        }
    }

    public boolean hasLuckPerms() {
        return luckPerms != null;
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
        if (luckPerms == null) return 0;
        try {
            Object userManager = invoke(luckPerms, "getUserManager");
            Object user = invoke(userManager, "getUser", uuid);
            if (user == null) return 0;
            Object cachedData = invoke(user, "getCachedData");
            Object metaData = invoke(cachedData, "getMetaData");
            Object raw = invoke(metaData, "getMetaValue", META_UPLOAD_LIMIT);
            if (!(raw instanceof String value)) return 0;
            try { return Integer.parseInt(value); } catch (NumberFormatException e) { return 0; }
        } catch (ReflectiveOperationException e) {
            logger.warning(MessageConfig.global("log_luckperms_upload_limit_read_failed", uuid, e.getMessage()));
            return 0;
        }
    }

    public boolean setUploadLimit(UUID uuid, int limit) {
        if (luckPerms == null) return false;
        withLoadedLuckPermsUser(uuid, user -> {
            Object data = invoke(user, "data");
            clearUploadLimitMeta(data);
            addNode(data, buildMetaNode(META_UPLOAD_LIMIT, String.valueOf(limit)));
        });
        return true;
    }

    public boolean grantPermission(UUID uuid, String permission) {
        if (luckPerms == null) return false;
        withLoadedLuckPermsUser(uuid, user -> addNode(invoke(user, "data"), buildPermissionNode(permission)));
        return true;
    }

    public boolean revokePermission(UUID uuid, String permission) {
        if (luckPerms == null) return false;
        withLoadedLuckPermsUser(uuid, user -> removeNode(invoke(user, "data"), buildPermissionNode(permission)));
        return true;
    }

    private Object findLuckPerms(ServicesManager services) {
        try {
            Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");
            @SuppressWarnings({ "rawtypes", "unchecked" })
            RegisteredServiceProvider<?> provider = services.getRegistration((Class) luckPermsClass);
            return provider == null ? null : provider.getProvider();
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private void withLoadedLuckPermsUser(UUID uuid, LuckPermsUserAction action) {
        try {
            Object userManager = invoke(luckPerms, "getUserManager");
            @SuppressWarnings("unchecked")
            CompletionStage<Object> stage = (CompletionStage<Object>) invoke(userManager, "loadUser", uuid);
            stage.thenAccept(user -> {
                if (user == null) return;
                try {
                    action.accept(user);
                    invoke(userManager, "saveUser", user);
                } catch (ReflectiveOperationException e) {
                    logger.warning(MessageConfig.global("log_luckperms_user_update_failed", uuid, e.getMessage()));
                }
            });
        } catch (ReflectiveOperationException | ClassCastException e) {
            logger.warning(MessageConfig.global("log_luckperms_user_load_failed", uuid, e.getMessage()));
        }
    }

    private Object buildPermissionNode(String permission) throws ReflectiveOperationException {
        Class<?> nodeClass = Class.forName("net.luckperms.api.node.Node");
        Object builder = nodeClass.getMethod("builder", String.class).invoke(null, permission);
        return invoke(builder, "build");
    }

    private Object buildMetaNode(String key, String value) throws ReflectiveOperationException {
        Class<?> metaNodeClass = Class.forName("net.luckperms.api.node.types.MetaNode");
        Object builder = metaNodeClass.getMethod("builder", String.class, String.class).invoke(null, key, value);
        return invoke(builder, "build");
    }

    private void clearUploadLimitMeta(Object data) throws ReflectiveOperationException {
        Predicate<Object> uploadLimitMeta = node -> {
            try {
                Class<?> metaNodeClass = Class.forName("net.luckperms.api.node.types.MetaNode");
                return metaNodeClass.isInstance(node) && META_UPLOAD_LIMIT.equals(invoke(node, "getMetaKey"));
            } catch (ReflectiveOperationException e) {
                return false;
            }
        };
        invoke(data, "clear", uploadLimitMeta);
    }

    private void addNode(Object data, Object node) throws ReflectiveOperationException {
        invoke(data, "add", node);
    }

    private void removeNode(Object data, Object node) throws ReflectiveOperationException {
        invoke(data, "remove", node);
    }

    private static Object invoke(Object target, String name, Object... args) throws ReflectiveOperationException {
        Method method = findMethod(target.getClass(), name, args.length);
        return method.invoke(target, args);
    }

    private static Method findMethod(Class<?> type, String name, int argCount) throws NoSuchMethodException {
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == argCount) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodException(type.getName() + "." + name + "/" + argCount);
    }

    @FunctionalInterface
    private interface LuckPermsUserAction {
        void accept(Object user) throws ReflectiveOperationException;
    }
}
