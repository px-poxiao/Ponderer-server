package com.ponderer.server.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class MessageConfig {

    private static MessageConfig current;

    private final Plugin plugin;
    private FileConfiguration cfg;
    private String prefix;

    public MessageConfig(Plugin plugin) {
        this.plugin = plugin;
        load();
        current = this;
    }

    public void reload() {
        load();
    }

    private void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        cfg = YamlConfiguration.loadConfiguration(file);

        try (InputStream defaultStream = plugin.getResource("messages.yml")) {
            if (defaultStream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
                cfg.setDefaults(defaults);
                cfg.options().copyDefaults(true);
                cfg.save(file);
            }
        } catch (Exception e) {
            plugin.getLogger().warning(get("log_messages_merge_failed", e.getMessage()));
        }

        this.prefix = color(cfg.getString("prefix", "&8[&bPonderer&8]&r"));
    }

    public String get(String key, Object... args) {
        String raw = cfg.getString(key, "&c[Missing message: " + key + "]");
        raw = color(raw).replace("{prefix}", prefix == null ? "" : prefix);
        for (int i = 0; i < args.length; i++) {
            raw = raw.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return raw;
    }

    public String featureName(String key) {
        if (key != null && key.startsWith("client_command:")) {
            return get("feature_name_client_command", key.substring("client_command:".length()));
        }
        String messageKey = "feature_name_" + String.valueOf(key).replaceAll("[^A-Za-z0-9_]", "_");
        return cfg.isString(messageKey) ? get(messageKey) : String.valueOf(key);
    }

    public static String global(String key, Object... args) {
        return current != null ? current.get(key, args) : fallback(key, args);
    }

    private static String fallback(String key, Object... args) {
        String raw = "[Missing message: " + key + "]";
        for (int i = 0; i < args.length; i++) {
            raw = raw.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return raw;
    }

    private static String color(String s) {
        return s == null ? "" : s.replace('&', '\u00A7');
    }
}
