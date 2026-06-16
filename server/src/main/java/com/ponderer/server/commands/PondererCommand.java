package com.ponderer.server.commands;

import com.ponderer.server.config.MessageConfig;
import com.ponderer.server.config.PluginConfig;
import com.ponderer.server.network.packets.ClientCommandPacket;
import com.ponderer.server.permissions.PermissionManager;
import com.ponderer.server.storage.LockStore;
import com.ponderer.server.storage.ReportStore;
import com.ponderer.server.storage.SceneOwnerStore;
import com.ponderer.server.storage.SubscriptionStore;
import com.ponderer.server.storage.VisibilityStore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class PondererCommand implements CommandExecutor, TabCompleter {

    private static final Set<String> UPLOAD_CMDS = Set.of("push", "new", "edit", "delete", "copy", "download");

    private final PermissionManager permissions;
    private final MessageConfig messages;
    private final PluginConfig config;
    private final Plugin plugin;
    private final LockStore lockStore;
    private final SceneOwnerStore ownerStore;
    private final SubscriptionStore subscriptions;
    private final ReportStore reportStore;
    private final VisibilityStore visibilityStore;

    public PondererCommand(PermissionManager permissions, MessageConfig messages, PluginConfig config, Plugin plugin,
                           LockStore lockStore, SceneOwnerStore ownerStore,
                           SubscriptionStore subscriptions, ReportStore reportStore,
                           VisibilityStore visibilityStore) {
        this.permissions = permissions;
        this.messages = messages;
        this.config = config;
        this.plugin = plugin;
        this.lockStore = lockStore;
        this.ownerStore = ownerStore;
        this.subscriptions = subscriptions;
        this.reportStore = reportStore;
        this.visibilityStore = visibilityStore;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.get("player_only"));
            return true;
        }

        String sub = args.length > 0 ? args[0].toLowerCase() : "list";

        switch (sub) {
            case "report" -> { handleReport(player, args); return true; }
            case "lock" -> { handleLock(player, args, true); return true; }
            case "unlock" -> { handleLock(player, args, false); return true; }
            case "subscribe" -> { handleSubscribe(player, args, true); return true; }
            case "unsubscribe" -> { handleSubscribe(player, args, false); return true; }
            case "visibility" -> { handleVisibility(player, args); return true; }
            default -> { }
        }

        if (!config.isCommandRelayEnabled()) {
            player.sendMessage(messages.get("feature_disabled", "command_relay"));
            return true;
        }
        if (!config.isClientCommandAllowed(sub)) {
            player.sendMessage(messages.get("feature_disabled", "client_command:" + sub));
            return true;
        }
        if (sub.equals("pull") && (!config.isSyncEnabled() || !permissions.canPull(player))) {
            player.sendMessage(!config.isSyncEnabled()
                    ? messages.get("feature_disabled", "sync")
                    : messages.get("no_pull_permission"));
            return true;
        }
        if (UPLOAD_CMDS.contains(sub) && (!config.isUploadEnabled() || !permissions.canUpload(player))) {
            player.sendMessage(!config.isUploadEnabled()
                    ? messages.get("feature_disabled", "upload")
                    : messages.get("no_upload_permission"));
            return true;
        }
        if (sub.equals("download") && !config.isStructureImportEnabled()) {
            player.sendMessage(messages.get("feature_disabled", "structure_import"));
            return true;
        }
        if (sub.equals("export") && !player.hasPermission(PermissionManager.PERM_PACK_EXPORT)) {
            player.sendMessage(messages.get("no_permission"));
            return true;
        }
        if (sub.equals("import") && !player.hasPermission(PermissionManager.PERM_PACK_IMPORT)) {
            player.sendMessage(messages.get("no_permission"));
            return true;
        }

        String payload = args.length > 0 ? normalizeRelayedCommand(player, args) : "list";
        new ClientCommandPacket(payload).send(player, plugin);
        return true;
    }

    private void handleReport(Player player, String[] args) {
        if (!config.isReportsEnabled()) {
            player.sendMessage(messages.get("feature_disabled", "reports"));
            return;
        }
        if (!player.hasPermission(PermissionManager.PERM_REPORT)) {
            player.sendMessage(messages.get("no_permission"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(messages.get("invalid_usage", "/ponderer report <sceneId> [reason]"));
            return;
        }

        String sceneId = resolveSceneArg(player, args[1]);
        if (sceneId == null) return;
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "";
        int count = reportStore.addReport(sceneId, player.getUniqueId(), reason);
        player.sendMessage(messages.get("report_submitted", sceneId));

        if (config.isReportAutoHideEnabled()
                && count >= config.getReportThreshold()
                && !reportStore.isFlagged(sceneId)) {
            reportStore.setFlagged(sceneId, true);
            if (config.isVisibilityEnabled()) {
                visibilityStore.setVisibility(sceneId, "private", List.of());
            }
        }
    }

    private void handleLock(Player player, String[] args, boolean lock) {
        if (!config.isLocksEnabled()) {
            player.sendMessage(messages.get("feature_disabled", "locks"));
            return;
        }
        if (!player.hasPermission(PermissionManager.PERM_LOCK)) {
            player.sendMessage(messages.get("no_permission"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(messages.get("invalid_usage", "/ponderer " + (lock ? "lock" : "unlock") + " <sceneId>"));
            return;
        }
        String sceneId = resolveSceneArg(player, args[1]);
        if (sceneId == null) return;
        if (!ownerStore.isOwner(sceneId, player.getUniqueId()) && !permissions.isAdmin(player)) {
            player.sendMessage(messages.get("no_edit_permission"));
            return;
        }
        if (lock) {
            lockStore.lock(sceneId, player.getUniqueId());
            player.sendMessage(messages.get("scene_locked_success", sceneId));
        } else {
            lockStore.unlock(sceneId);
            player.sendMessage(messages.get("scene_unlocked_success", sceneId));
        }
    }

    private void handleSubscribe(Player player, String[] args, boolean subscribe) {
        if (!config.isSubscriptionsEnabled()) {
            player.sendMessage(messages.get("feature_disabled", "subscriptions"));
            return;
        }
        if (!permissions.canPull(player)) {
            player.sendMessage(messages.get("no_pull_permission"));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(messages.get("invalid_usage", "/ponderer " + (subscribe ? "subscribe" : "unsubscribe") + " <sceneId>"));
            return;
        }
        String sceneId = resolveSceneArg(player, args[1]);
        if (sceneId == null) return;
        if (subscribe) {
            subscriptions.subscribe(sceneId, player.getUniqueId());
            player.sendMessage(messages.get("subscribed", sceneId));
        } else {
            subscriptions.unsubscribe(sceneId, player.getUniqueId());
            player.sendMessage(messages.get("unsubscribed", sceneId));
        }
    }

    private void handleVisibility(Player player, String[] args) {
        if (!config.isVisibilityEnabled()) {
            player.sendMessage(messages.get("feature_disabled", "visibility"));
            return;
        }
        if (args.length < 3) {
            player.sendMessage(messages.get("invalid_usage", "/ponderer visibility <sceneId> <public|private|group> [groups...]"));
            return;
        }
        String sceneId = resolveSceneArg(player, args[1]);
        if (sceneId == null) return;
        if (!ownerStore.isOwner(sceneId, player.getUniqueId()) && !permissions.isAdmin(player)) {
            player.sendMessage(messages.get("no_edit_permission"));
            return;
        }

        String vis = args[2].toLowerCase();
        if (!vis.equals("public") && !vis.equals("private") && !vis.equals("group")) {
            player.sendMessage(messages.get("invalid_usage", "/ponderer visibility <sceneId> <public|private|group> [groups...]"));
            return;
        }
        if (vis.equals("group") && !config.isVisibilityGroupsEnabled()) {
            player.sendMessage(messages.get("feature_disabled", "visibility_groups"));
            return;
        }

        List<String> groups = args.length > 3
                ? Arrays.asList(Arrays.copyOfRange(args, 3, args.length))
                : List.of();
        visibilityStore.setVisibility(sceneId, vis, groups);
        player.sendMessage(messages.get("visibility_set", sceneId, vis));
    }

    private String normalizeRelayedCommand(Player player, String[] args) {
        String[] out = Arrays.copyOf(args, args.length);
        String sub = out[0].toLowerCase();
        switch (sub) {
            case "push" -> {
                if (out.length >= 2 && isHand(out[1])) {
                    String sceneId = handSceneId(player);
                    if (sceneId != null) out[1] = sceneId;
                } else if (out.length >= 3 && out[1].equalsIgnoreCase("force") && isHand(out[2])) {
                    String sceneId = handSceneId(player);
                    if (sceneId != null) out[2] = sceneId;
                }
            }
            case "delete" -> {
                if (out.length >= 3 && out[1].equalsIgnoreCase("item") && isHand(out[2])) {
                    String itemId = handItemId(player);
                    if (itemId != null) out[2] = itemId;
                } else if (out.length >= 2 && isHand(out[1])) {
                    String sceneId = handSceneId(player);
                    if (sceneId != null) out[1] = sceneId;
                }
            }
            case "copy" -> {
                if (out.length >= 3 && isHand(out[2])) {
                    String itemId = handItemId(player);
                    if (itemId != null) out[2] = itemId;
                }
            }
            case "convert" -> {
                if (out.length >= 3 && isHand(out[2])) {
                    String sceneId = handSceneId(player);
                    if (sceneId != null) out[2] = sceneId;
                }
            }
            case "edit" -> {
                if (out.length >= 2 && isHand(out[1])) {
                    String sceneId = handSceneId(player);
                    if (sceneId != null) out[1] = sceneId;
                }
            }
            default -> { }
        }
        return String.join(" ", out);
    }

    private String resolveSceneArg(Player player, String raw) {
        if (!isHand(raw)) return raw;
        String sceneId = handSceneId(player);
        if (sceneId == null) {
            player.sendMessage("\u00A7cYour main hand is empty.");
        }
        return sceneId;
    }

    private static boolean isHand(String value) {
        return value != null && value.equalsIgnoreCase("hand");
    }

    private static String handSceneId(Player player) {
        String itemId = handItemId(player);
        if (itemId == null) return null;
        int colon = itemId.indexOf(':');
        String path = colon >= 0 ? itemId.substring(colon + 1) : itemId;
        return "ponderer:" + path;
    }

    private static String handItemId(Player player) {
        ItemStack stack = player.getInventory().getItemInMainHand();
        if (stack == null || stack.getType().isAir()) return null;
        var key = stack.getType().getKey();
        return key.getNamespace() + ":" + key.getKey();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("pull", "push", "reload", "new", "list", "export", "import",
                    "edit", "delete", "copy", "download", "convert", "unregister_pack",
                    "report", "lock", "unlock", "subscribe", "unsubscribe", "visibility");
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "pull" -> Arrays.asList("force", "keep_local");
                case "push" -> Arrays.asList("force");
                case "new", "edit", "report", "lock", "unlock", "subscribe", "unsubscribe" -> Arrays.asList("hand");
                case "convert" -> Arrays.asList("to_ponderjs", "from_ponderjs");
                case "visibility" -> Arrays.asList("public", "private", "group");
                case "delete" -> Arrays.asList("hand", "item");
                default -> List.of();
            };
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("delete") && args[1].equalsIgnoreCase("item")) {
            return List.of("hand");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("copy")) {
            return List.of("hand");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("convert")) {
            return List.of("all");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("visibility")) {
            return Arrays.asList("public", "private", "group");
        }
        return List.of();
    }
}
