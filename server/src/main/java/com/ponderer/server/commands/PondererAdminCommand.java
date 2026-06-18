package com.ponderer.server.commands;

import com.ponderer.server.config.MessageConfig;
import com.ponderer.server.config.PluginConfig;
import com.ponderer.server.handler.UploadHandler;
import com.ponderer.server.permissions.PermissionManager;
import com.ponderer.server.stats.StatsTracker;
import com.ponderer.server.storage.BackupManager;
import com.ponderer.server.storage.CollabStore;
import com.ponderer.server.storage.PlayerDataStore;
import com.ponderer.server.storage.ReportStore;
import com.ponderer.server.storage.ReviewStore;
import com.ponderer.server.storage.SceneStore;
import com.ponderer.server.storage.VisibilityStore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class PondererAdminCommand implements CommandExecutor, TabCompleter {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private final PermissionManager permissions;
    private final PlayerDataStore playerData;
    private final StatsTracker stats;
    private final SceneStore sceneStore;
    private final MessageConfig messages;
    private final ReviewStore reviewStore;
    private final VisibilityStore visibilityStore;
    private final CollabStore collabStore;
    private final ReportStore reportStore;
    private final UploadHandler uploadHandler;
    private final BackupManager backupManager;
    private final PluginConfig config;
    private final Plugin plugin;

    public PondererAdminCommand(PermissionManager permissions, PlayerDataStore playerData,
                                StatsTracker stats, SceneStore sceneStore, MessageConfig messages,
                                ReviewStore reviewStore, VisibilityStore visibilityStore,
                                CollabStore collabStore, ReportStore reportStore,
                                UploadHandler uploadHandler, BackupManager backupManager,
                                PluginConfig config, Plugin plugin) {
        this.permissions = permissions;
        this.playerData = playerData;
        this.stats = stats;
        this.sceneStore = sceneStore;
        this.messages = messages;
        this.reviewStore = reviewStore;
        this.visibilityStore = visibilityStore;
        this.collabStore = collabStore;
        this.reportStore = reportStore;
        this.uploadHandler = uploadHandler;
        this.backupManager = backupManager;
        this.config = config;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(PermissionManager.PERM_ADMIN)) {
            sender.sendMessage(messages.get("admin_no_permission"));
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "topup" -> handleTopup(sender, args);
            case "settokens" -> handleSetTokens(sender, args);
            case "tokens" -> handleTokens(sender, args);
            case "setlimit" -> handleSetLimit(sender, args);
            case "resetcount" -> handleResetCount(sender, args);
            case "stats" -> handleStats(sender, args);
            case "scenes" -> handleScenes(sender);
            case "grant" -> handleGrant(sender, args);
            case "revoke" -> handleRevoke(sender, args);
            case "reload" -> handleReload(sender);
            case "review" -> handleReview(sender, args);
            case "group" -> handleGroup(sender, args);
            case "collab" -> handleCollab(sender, args);
            case "reports" -> handleReports(sender, args);
            case "history" -> handleHistory(sender, args);
            case "rollback" -> handleRollback(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleTopup(CommandSender sender, String[] args) {
        if (!config.isServerAiEnabled()) {
            sender.sendMessage(messages.get("feature_disabled", messages.featureName("server_ai")));
            return;
        }
        if (!sender.hasPermission(PermissionManager.PERM_ADMIN_TOPUP)) {
            sender.sendMessage(messages.get("admin_no_topup_permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_topup")));
            return;
        }
        UUID target = resolveUuid(args[1]);
        if (target == null) {
            sender.sendMessage(messages.get("admin_player_not_found", args[1]));
            return;
        }
        long amount = parseLong(sender, args[2]);
        if (amount < 0) return;

        playerData.addTokens(target, amount);
        if (config.isLogTopups()) {
            UUID adminUuid = sender instanceof Player p ? p.getUniqueId() : new UUID(0, 0);
            stats.recordTopup(target, adminUuid, amount);
        }
        sender.sendMessage(messages.get("admin_topup_success", args[1], amount, playerData.getRemainingTokens(target)));
    }

    private void handleSetTokens(CommandSender sender, String[] args) {
        if (!config.isServerAiEnabled()) {
            sender.sendMessage(messages.get("feature_disabled", messages.featureName("server_ai")));
            return;
        }
        if (!sender.hasPermission(PermissionManager.PERM_ADMIN_TOPUP)) {
            sender.sendMessage(messages.get("admin_no_topup_permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_settokens")));
            return;
        }
        UUID target = resolveUuid(args[1]);
        if (target == null) {
            sender.sendMessage(messages.get("admin_player_not_found", args[1]));
            return;
        }
        long amount = parseLong(sender, args[2]);
        if (amount < 0) return;

        playerData.setRemainingTokens(target, amount);
        sender.sendMessage(messages.get("admin_settokens_success", args[1], amount));
    }

    private void handleTokens(CommandSender sender, String[] args) {
        if (!config.isServerAiEnabled()) {
            sender.sendMessage(messages.get("feature_disabled", messages.featureName("server_ai")));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_tokens")));
            return;
        }
        UUID target = resolveUuid(args[1]);
        if (target == null) {
            sender.sendMessage(messages.get("admin_player_not_found", args[1]));
            return;
        }
        sender.sendMessage(messages.get("admin_tokens_info", args[1],
                playerData.getRemainingTokens(target), playerData.getTotalAiCalls(target)));
    }

    private void handleSetLimit(CommandSender sender, String[] args) {
        if (!config.isPermissionAdminEnabled()) {
            sender.sendMessage(messages.get("feature_disabled", messages.featureName("permission_admin")));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_setlimit")));
            return;
        }
        UUID target = resolveUuid(args[1]);
        if (target == null) {
            sender.sendMessage(messages.get("admin_player_not_found", args[1]));
            return;
        }
        int limit;
        try {
            limit = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(messages.get("admin_invalid_limit"));
            return;
        }
        if (!permissions.setUploadLimit(target, limit)) {
            sender.sendMessage(messages.get("admin_luckperms_meta_unavailable"));
            return;
        }
        sender.sendMessage(messages.get("admin_setlimit_success", args[1],
                limit < 0 ? messages.get("admin_unlimited") : limit));
    }

    private void handleResetCount(CommandSender sender, String[] args) {
        if (!config.isPlayerStatsEnabled()) {
            sender.sendMessage(messages.get("feature_disabled", messages.featureName("player_stats")));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_resetcount")));
            return;
        }
        UUID target = resolveUuid(args[1]);
        if (target == null) {
            sender.sendMessage(messages.get("admin_player_not_found", args[1]));
            return;
        }
        playerData.setUploadCount(target, 0);
        sender.sendMessage(messages.get("admin_resetcount_success", args[1]));
    }

    private void handleStats(CommandSender sender, String[] args) {
        if (!config.isPlayerStatsEnabled()) {
            sender.sendMessage(messages.get("feature_disabled", messages.featureName("player_stats")));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_stats")));
            return;
        }
        UUID target = resolveUuid(args[1]);
        if (target == null) {
            sender.sendMessage(messages.get("admin_player_not_found", args[1]));
            return;
        }
        PlayerDataStore.PlayerData data = playerData.getData(target);
        sender.sendMessage(messages.get("admin_stats_header", args[1]));
        sender.sendMessage(messages.get("admin_stats_uploads", data.uploadCount));
        sender.sendMessage(messages.get("admin_stats_ai_calls", data.totalAiCalls));
        sender.sendMessage(messages.get("admin_stats_tokens", data.remainingTokens));
    }

    private void handleScenes(CommandSender sender) {
        sender.sendMessage(messages.get("admin_scenes_total", sceneStore.countTotalScenes()));
    }

    private void handleGrant(CommandSender sender, String[] args) {
        if (!config.isPermissionAdminEnabled()) {
            sender.sendMessage(messages.get("feature_disabled", messages.featureName("permission_admin")));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_grant")));
            return;
        }
        UUID target = resolveUuid(args[1]);
        if (target == null) {
            sender.sendMessage(messages.get("admin_player_not_found", args[1]));
            return;
        }
        if (!permissions.grantPermission(target, args[2])) {
            sender.sendMessage(messages.get("admin_luckperms_permission_unavailable"));
            return;
        }
        sender.sendMessage(messages.get("admin_grant_success", args[1], args[2]));
    }

    private void handleRevoke(CommandSender sender, String[] args) {
        if (!config.isPermissionAdminEnabled()) {
            sender.sendMessage(messages.get("feature_disabled", messages.featureName("permission_admin")));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_revoke")));
            return;
        }
        UUID target = resolveUuid(args[1]);
        if (target == null) {
            sender.sendMessage(messages.get("admin_player_not_found", args[1]));
            return;
        }
        if (!permissions.revokePermission(target, args[2])) {
            sender.sendMessage(messages.get("admin_luckperms_permission_unavailable"));
            return;
        }
        sender.sendMessage(messages.get("admin_revoke_success", args[1], args[2]));
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        config.reload(plugin.getConfig());
        messages.reload();
        sender.sendMessage(messages.get("admin_reload_success"));
    }

    private void handleReview(CommandSender sender, String[] args) {
        if (!config.isReviewEnabled()) {
            sender.sendMessage(messages.get("feature_disabled", messages.featureName("review")));
            return;
        }
        if (!sender.hasPermission(PermissionManager.PERM_ADMIN_REVIEW)) {
            sender.sendMessage(messages.get("admin_no_permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_review")));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "list" -> {
                var pending = reviewStore.getPending();
                if (pending.isEmpty()) {
                    sender.sendMessage(messages.get("review_list_empty"));
                    return;
                }
                sender.sendMessage(messages.get("review_list_header", pending.size()));
                for (var e : pending) {
                    sender.sendMessage(messages.get("review_list_entry",
                            e.sceneId, e.ownerUuid, DT.format(Instant.ofEpochMilli(e.submittedAt))));
                }
            }
            case "approve" -> {
                if (args.length < 3) {
                    sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_review_approve")));
                    return;
                }
                String sceneId = args[2];
                ReviewStore.ReviewEntry entry = reviewStore.getEntry(sceneId);
                if (entry == null || !"pending".equals(entry.status)) {
                    sender.sendMessage(messages.get("review_not_found", sceneId));
                    return;
                }
                Player owner = getOnlineOwner(entry.ownerUuid);
                if (!uploadHandler.commitApproved(entry, owner)) {
                    sender.sendMessage(messages.get("upload_failed", sceneId));
                    return;
                }
                sender.sendMessage(messages.get("review_approved", sceneId));
            }
            case "reject" -> {
                if (args.length < 3) {
                    sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_review_reject")));
                    return;
                }
                String sceneId = args[2];
                String reason = args.length > 3
                        ? String.join(" ", Arrays.copyOfRange(args, 3, args.length))
                        : messages.get("admin_reject_default_reason");
                ReviewStore.ReviewEntry entry = reviewStore.getEntry(sceneId);
                if (entry == null) {
                    sender.sendMessage(messages.get("review_not_found", sceneId));
                    return;
                }
                reviewStore.reject(sceneId, reason);
                Player owner = getOnlineOwner(entry.ownerUuid);
                if (owner != null) {
                    owner.sendMessage(messages.get("review_rejected_notify", sceneId, reason));
                }
                sender.sendMessage(messages.get("review_rejected", sceneId, reason));
            }
            default -> sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_review")));
        }
    }

    private void handleGroup(CommandSender sender, String[] args) {
        if (!config.isVisibilityEnabled() || !config.isVisibilityGroupsEnabled()) {
            sender.sendMessage(messages.get("feature_disabled", messages.featureName("visibility_groups")));
            return;
        }
        if (!sender.hasPermission(PermissionManager.PERM_ADMIN_GROUPS)) {
            sender.sendMessage(messages.get("admin_no_permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_group")));
            return;
        }
        String sub = args[1].toLowerCase();
        String groupName = args[2];
        switch (sub) {
            case "create" -> {
                visibilityStore.createGroup(groupName);
                sender.sendMessage(messages.get("group_created", groupName));
            }
            case "delete" -> {
                visibilityStore.deleteGroup(groupName);
                sender.sendMessage(messages.get("group_deleted", groupName));
            }
            case "addplayer" -> {
                if (args.length < 4) {
                    sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_group_addplayer")));
                    return;
                }
                UUID target = resolveUuid(args[3]);
                if (target == null) {
                    sender.sendMessage(messages.get("admin_player_not_found", args[3]));
                    return;
                }
                visibilityStore.addPlayerToGroup(groupName, target);
                sender.sendMessage(messages.get("group_player_added", args[3], groupName));
            }
            case "removeplayer" -> {
                if (args.length < 4) {
                    sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_group_removeplayer")));
                    return;
                }
                UUID target = resolveUuid(args[3]);
                if (target == null) {
                    sender.sendMessage(messages.get("admin_player_not_found", args[3]));
                    return;
                }
                visibilityStore.removePlayerFromGroup(groupName, target);
                sender.sendMessage(messages.get("group_player_removed", args[3], groupName));
            }
            case "list" -> {
                List<String> members = visibilityStore.getGroupMembers(groupName);
                sender.sendMessage(messages.get("group_members_header", groupName, members.size()));
                members.forEach(m -> sender.sendMessage(messages.get("list_entry", m)));
            }
            default -> sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_group")));
        }
    }

    private void handleCollab(CommandSender sender, String[] args) {
        if (!config.isCollaborationEnabled()) {
            sender.sendMessage(messages.get("feature_disabled", messages.featureName("collaboration")));
            return;
        }
        if (!sender.hasPermission(PermissionManager.PERM_ADMIN_COLLAB)) {
            sender.sendMessage(messages.get("admin_no_permission"));
            return;
        }
        if (args.length < 4) {
            sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_collab")));
            return;
        }
        String sub = args[1].toLowerCase();
        switch (sub) {
            case "add-scene" -> {
                UUID target = resolveUuid(args[3]);
                if (target == null) {
                    sender.sendMessage(messages.get("admin_player_not_found", args[3]));
                    return;
                }
                collabStore.addSceneCollab(args[2], target);
                sender.sendMessage(messages.get("collab_scene_added", args[3], args[2]));
            }
            case "remove-scene" -> {
                UUID target = resolveUuid(args[3]);
                if (target == null) {
                    sender.sendMessage(messages.get("admin_player_not_found", args[3]));
                    return;
                }
                collabStore.removeSceneCollab(args[2], target);
                sender.sendMessage(messages.get("collab_scene_removed", args[3], args[2]));
            }
            case "add-global" -> {
                UUID owner = resolveUuid(args[2]);
                UUID collab = resolveUuid(args[3]);
                if (owner == null) {
                    sender.sendMessage(messages.get("admin_player_not_found", args[2]));
                    return;
                }
                if (collab == null) {
                    sender.sendMessage(messages.get("admin_player_not_found", args[3]));
                    return;
                }
                collabStore.addGlobalCollab(owner, collab);
                sender.sendMessage(messages.get("collab_global_added", args[3], args[2]));
            }
            case "remove-global" -> {
                UUID owner = resolveUuid(args[2]);
                UUID collab = resolveUuid(args[3]);
                if (owner == null) {
                    sender.sendMessage(messages.get("admin_player_not_found", args[2]));
                    return;
                }
                if (collab == null) {
                    sender.sendMessage(messages.get("admin_player_not_found", args[3]));
                    return;
                }
                collabStore.removeGlobalCollab(owner, collab);
                sender.sendMessage(messages.get("collab_global_removed", args[3], args[2]));
            }
            default -> sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_collab")));
        }
    }

    private void handleReports(CommandSender sender, String[] args) {
        if (!config.isReportsEnabled()) {
            sender.sendMessage(messages.get("feature_disabled", messages.featureName("reports")));
            return;
        }
        if (!sender.hasPermission(PermissionManager.PERM_ADMIN_REPORTS)) {
            sender.sendMessage(messages.get("admin_no_permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_reports")));
            return;
        }
        switch (args[1].toLowerCase()) {
            case "list" -> {
                List<String> flagged = reportStore.getAllFlagged();
                if (flagged.isEmpty()) {
                    sender.sendMessage(messages.get("reports_list_empty"));
                    return;
                }
                sender.sendMessage(messages.get("reports_list_header", flagged.size()));
                for (String id : flagged) {
                    sender.sendMessage(messages.get("reports_list_entry", id, reportStore.getReportCount(id)));
                }
            }
            case "dismiss" -> {
                if (args.length < 3) {
                    sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_reports_dismiss")));
                    return;
                }
                reportStore.dismiss(args[2]);
                sender.sendMessage(messages.get("reports_dismissed", args[2]));
            }
            default -> sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_reports")));
        }
    }

    private void handleHistory(CommandSender sender, String[] args) {
        if (!config.isHistoryEnabled() || !config.isBackupsEnabled()) {
            sender.sendMessage(messages.get("feature_disabled", messages.featureName("history")));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_history")));
            return;
        }
        String sceneId = args[1];
        Path backupDir = backupDir(sceneId);
        if (!Files.exists(backupDir)) {
            sender.sendMessage(messages.get("history_none", sceneId));
            return;
        }
        try (var stream = Files.list(backupDir)) {
            List<String> files = stream.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
            if (files.isEmpty()) {
                sender.sendMessage(messages.get("history_none", sceneId));
                return;
            }
            sender.sendMessage(messages.get("history_header", sceneId, files.size()));
            files.forEach(f -> sender.sendMessage(messages.get("list_entry", f.replace(".json", ""))));
        } catch (IOException e) {
            sender.sendMessage(messages.get("history_read_failed", e.getMessage()));
        }
    }

    private void handleRollback(CommandSender sender, String[] args) {
        if (!config.isHistoryEnabled() || !config.isBackupsEnabled()) {
            sender.sendMessage(messages.get("feature_disabled", messages.featureName("history")));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(messages.get("invalid_usage", messages.get("usage_admin_rollback")));
            return;
        }
        String sceneId = args[1];
        String timestamp = args[2];
        Path backupFile = backupDir(sceneId).resolve(timestamp + ".json");
        if (!Files.exists(backupFile)) {
            sender.sendMessage(messages.get("rollback_not_found", timestamp));
            return;
        }
        try {
            byte[] existing = sceneStore.readScene(sceneId);
            if (existing != null) {
                backupManager.backupScene(sceneId, existing);
            }
            boolean ok = sceneStore.saveScene(sceneId, Files.readString(backupFile));
            sender.sendMessage(ok
                    ? messages.get("rollback_success", sceneId, timestamp)
                    : messages.get("rollback_failed", sceneId));
        } catch (IOException e) {
            sender.sendMessage(messages.get("rollback_error", e.getMessage()));
        }
    }

    private Path backupDir(String sceneId) {
        String safeName = sceneId.replace(':', '_').replace('/', '_');
        return sceneStore.getSceneDir().getParent().resolve(".backups").resolve(safeName);
    }

    private long parseLong(CommandSender sender, String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            sender.sendMessage(messages.get("admin_invalid_amount"));
            return -1;
        }
    }

    private Player getOnlineOwner(String uuid) {
        try {
            return Bukkit.getPlayer(UUID.fromString(uuid));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(messages.get("admin_help_header"));
        for (String key : List.of(
                "admin_help_topup",
                "admin_help_settokens",
                "admin_help_tokens",
                "admin_help_setlimit",
                "admin_help_resetcount",
                "admin_help_stats",
                "admin_help_scenes",
                "admin_help_grant",
                "admin_help_revoke",
                "admin_help_reload",
                "admin_help_review",
                "admin_help_group",
                "admin_help_collab",
                "admin_help_reports",
                "admin_help_history",
                "admin_help_rollback")) {
            sender.sendMessage(messages.get(key));
        }
    }

    private UUID resolveUuid(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getUniqueId();
        @SuppressWarnings("deprecation")
        var profile = Bukkit.getOfflinePlayer(name);
        return profile.hasPlayedBefore() ? profile.getUniqueId() : null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("topup", "settokens", "tokens", "setlimit", "resetcount", "stats",
                    "scenes", "grant", "revoke", "reload", "review", "group", "collab", "reports",
                    "history", "rollback");
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "topup", "settokens", "tokens", "setlimit", "resetcount", "stats",
                        "grant", "revoke" -> Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
                case "review" -> Arrays.asList("list", "approve", "reject");
                case "group" -> Arrays.asList("create", "delete", "addplayer", "removeplayer", "list");
                case "collab" -> Arrays.asList("add-scene", "remove-scene", "add-global", "remove-global");
                case "reports" -> Arrays.asList("list", "dismiss");
                default -> List.of();
            };
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("grant") || args[0].equalsIgnoreCase("revoke")) {
                return Arrays.asList(
                        PermissionManager.PERM_PULL, PermissionManager.PERM_UPLOAD,
                        PermissionManager.PERM_ADMIN, PermissionManager.PERM_CREATE_REGION,
                        PermissionManager.PERM_BLUEPRINT, PermissionManager.PERM_AI_SERVER_API,
                        PermissionManager.PERM_PACK_IMPORT, PermissionManager.PERM_PACK_EXPORT,
                        PermissionManager.PERM_REPORT, PermissionManager.PERM_LOCK);
            }
            if (args[0].equalsIgnoreCase("group")
                    && (args[1].equalsIgnoreCase("addplayer") || args[1].equalsIgnoreCase("removeplayer"))) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            }
            if (args[0].equalsIgnoreCase("collab")) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            }
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("collab")
                && (args[1].equalsIgnoreCase("add-global") || args[1].equalsIgnoreCase("remove-global"))) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }
}
