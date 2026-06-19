package com.ponderer.server.config;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class PluginConfig {

    private FileConfiguration cfg;

    public PluginConfig(FileConfiguration cfg) {
        this.cfg = cfg;
    }

    public void reload(FileConfiguration cfg) {
        this.cfg = cfg;
    }

    public boolean isSyncEnabled() { return feature("sync", true); }
    public boolean isUploadEnabled() { return feature("upload", true); }
    public boolean isCommandRelayEnabled() { return feature("command_relay", true); }
    public boolean isStructureImportEnabled() { return feature("structure_import", true); }
    public boolean isServerAiEnabled() { return feature("server_ai", true); }
    public boolean isReviewEnabled() { return feature("review", true); }
    public boolean isVisibilityEnabled() { return feature("visibility", true); }
    public boolean isVisibilityGroupsEnabled() { return feature("visibility_groups", true); }
    public boolean isCollaborationEnabled() { return feature("collaboration", true); }
    public boolean isReportsEnabled() { return feature("reports", true); }
    public boolean isSubscriptionsEnabled() { return feature("subscriptions", true); }
    public boolean isLocksEnabled() { return feature("locks", true); }
    public boolean isBackupsEnabled() { return feature("backups", true); }
    public boolean isHistoryEnabled() { return feature("history", true); }
    public boolean isPermissionAdminEnabled() { return feature("permission_admin", true); }
    public boolean isPlayerStatsEnabled() { return feature("player_stats", true); }

    public Set<String> getAllowedClientCommands() {
        return cfg.getStringList("client_commands.allowed").stream()
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isClientCommandAllowed(String command) {
        Set<String> allowed = getAllowedClientCommands();
        return allowed.isEmpty() || allowed.contains(command.toLowerCase(Locale.ROOT));
    }

    // Player AI
    public String getAiProvider() { return text("ai.provider", "anthropic"); }
    public String getAiBaseUrl() { return text("ai.api_base_url", ""); }
    public String getAiApiKey() { return text("ai.api_key", ""); }
    public String getAiModel() { return text("ai.model", ""); }
    public int getAiMaxTokens() { return cfg.getInt("ai.max_tokens", 16384); }
    public boolean shouldFailIfAiKeyMissing() { return cfg.getBoolean("ai.fail_if_api_key_missing", true); }
    public boolean isClientProviderOverrideAllowed() { return cfg.getBoolean("ai.allow_client_provider_override", false); }

    public double getAnthropicInputPrice() { return cfg.getDouble("ai.pricing.anthropic_input", 3.0); }
    public double getAnthropicOutputPrice() { return cfg.getDouble("ai.pricing.anthropic_output", 15.0); }
    public double getOpenAiInputPrice() { return cfg.getDouble("ai.pricing.openai_input", 5.0); }
    public double getOpenAiOutputPrice() { return cfg.getDouble("ai.pricing.openai_output", 15.0); }

    public int getUploadsPerHour() { return cfg.getInt("rate_limit.uploads_per_hour", 20); }

    public int getMaxBackups() {
        return isBackupsEnabled() ? cfg.getInt("backup.max_backups", 10) : 0;
    }

    public boolean isLogAiCalls() { return cfg.getBoolean("logging.log_ai_calls", true); }
    public boolean isLogUploads() { return cfg.getBoolean("logging.log_uploads", true); }
    public boolean isLogTopups() { return cfg.getBoolean("logging.log_topups", true); }
    public int getDefaultUploadLimit() { return cfg.getInt("default_upload_limit", 0); }

    // Scene filter
    public boolean isSceneFilterEnabled() { return cfg.getBoolean("scene_filter.enabled", true); }
    public String getSceneFilterMode() {
        return isSceneFilterEnabled() ? cfg.getString("scene_filter.mode", "off") : "off";
    }
    public List<String> getSceneFilterItems() { return cfg.getStringList("scene_filter.items"); }

    // NBT restriction
    public boolean isNbtRestrictionEnabled() { return cfg.getBoolean("nbt_restriction.enabled", false); }
    public String getNbtRestrictionMode() { return cfg.getString("nbt_restriction.mode", "whitelist"); }

    // Upload checks
    public long getMaxSceneSizeBytes() {
        return getLongCompat("upload_validation.max_scene_size_bytes", "max_scene_size_bytes", 0);
    }

    public int getPushCooldownSeconds() {
        return getIntCompat("upload_validation.push_cooldown_seconds", "push_cooldown_seconds", 0);
    }

    public boolean isRegionPermissionRequired() {
        return cfg.getBoolean("upload_validation.require_region_permission", true);
    }

    public boolean isConflictCheckEnabled() {
        return cfg.getBoolean("upload_validation.check_conflicts", true);
    }

    public boolean isForcePushAllowed() {
        return cfg.getBoolean("upload_validation.allow_force_push", true);
    }

    // Reports
    public int getReportThreshold() {
        return getIntCompat("reports.threshold", "report_threshold", 3);
    }

    public boolean isReportAutoHideEnabled() {
        return cfg.getBoolean("reports.auto_hide", true);
    }

    // Review
    public String getReviewMode() {
        if (!isReviewEnabled()) return "auto";
        return cfg.getString("review.mode", "auto");
    }

    public String getReviewAiErrorFallback() {
        return cfg.getString("review.ai_error_fallback", "pending");
    }

    public String getReviewAiProvider() { return text("review_ai.provider", "anthropic"); }
    public String getReviewAiBaseUrl() { return text("review_ai.api_base_url", ""); }
    public String getReviewAiApiKey() { return text("review_ai.api_key", ""); }
    public String getReviewAiModel() { return text("review_ai.model", ""); }
    public String getReviewAiSystemPrompt() {
        return cfg.getString("review_ai.system_prompt",
                "You are a content moderator for a Minecraft server. Review this Ponder scene upload. " +
                        "Carrier item: {item}. Scene ID: {scene}. Scene JSON: {json}. " +
                        "If appropriate, reply only: yes. If inappropriate, reply only: no.");
    }

    // Scheduled backup
    public boolean isScheduledBackupEnabled() {
        return isBackupsEnabled() && cfg.getBoolean("scheduled_backup.enabled", false);
    }

    public int getScheduledBackupIntervalHours() {
        return Math.max(1, cfg.getInt("scheduled_backup.interval_hours", 24));
    }

    // Expiry
    public boolean isExpiryEnabled() { return cfg.getBoolean("expiry.enabled", false); }
    public int getExpiryDays() { return Math.max(1, cfg.getInt("expiry.days", 30)); }

    private boolean feature(String name, boolean def) {
        return cfg.getBoolean("features." + name, def);
    }

    private String text(String path, String def) {
        String value = cfg.getString(path, def);
        return value == null ? "" : value.trim();
    }

    private int getIntCompat(String preferred, String legacy, int def) {
        return cfg.contains(preferred) ? cfg.getInt(preferred, def) : cfg.getInt(legacy, def);
    }

    private long getLongCompat(String preferred, String legacy, long def) {
        return cfg.contains(preferred) ? cfg.getLong(preferred, def) : cfg.getLong(legacy, def);
    }
}
