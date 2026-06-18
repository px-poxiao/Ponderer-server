package com.ponderer.addon;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public final class PondererAddonConfig {

    private static final Properties DEFAULTS = new Properties();
    private static Properties values = new Properties();

    static {
        DEFAULTS.setProperty("features.server_ai_proxy", "true");
        DEFAULTS.setProperty("features.command_relay", "true");
        DEFAULTS.setProperty("features.file_watcher", "true");
        DEFAULTS.setProperty("features.edit_button", "true");
        DEFAULTS.setProperty("ai.validate_json", "true");
        DEFAULTS.setProperty("ai.server_request_timeout_seconds", "180");
        DEFAULTS.setProperty("file_watcher.show_toast", "true");
        DEFAULTS.setProperty("client_commands.allowed",
                "pull,push,reload,new,list,edit,export,import,delete,copy,download,convert,unregister_pack");
    }

    private PondererAddonConfig() {}

    public static void load(Path configDir) {
        Path path = configDir.resolve("ponderer_client_addon.properties");
        values = new Properties(DEFAULTS);
        try {
            Files.createDirectories(path.getParent());
            if (Files.exists(path)) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    values.load(reader);
                }
            }
            try (Writer writer = Files.newBufferedWriter(path)) {
                values.store(writer, PondererAddonMessages.get("config.header"));
            }
        } catch (IOException ignored) {
        }
    }

    public static boolean isServerAiProxyEnabled() {
        return bool("features.server_ai_proxy", true);
    }

    public static boolean isCommandRelayEnabled() {
        return bool("features.command_relay", true);
    }

    public static boolean isFileWatcherEnabled() {
        return bool("features.file_watcher", true);
    }

    public static boolean isEditButtonEnabled() {
        return bool("features.edit_button", true);
    }

    public static boolean shouldValidateAiJson() {
        return bool("ai.validate_json", true);
    }

    public static int getServerAiRequestTimeoutSeconds() {
        return intValue("ai.server_request_timeout_seconds", 180);
    }

    public static boolean shouldShowFileWatcherToast() {
        return bool("file_watcher.show_toast", true);
    }

    public static boolean isClientCommandAllowed(String command) {
        Set<String> allowed = Arrays.stream(values.getProperty("client_commands.allowed", "").split(","))
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        return allowed.isEmpty() || allowed.contains(command.toLowerCase(Locale.ROOT));
    }

    private static boolean bool(String key, boolean def) {
        return Boolean.parseBoolean(values.getProperty(key, Boolean.toString(def)));
    }

    private static int intValue(String key, int def) {
        try {
            return Integer.parseInt(values.getProperty(key, Integer.toString(def)).trim());
        } catch (NumberFormatException ignored) {
            return def;
        }
    }

}
