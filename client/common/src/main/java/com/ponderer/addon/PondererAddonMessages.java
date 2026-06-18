package com.ponderer.addon;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;

public final class PondererAddonMessages {

    private static final String RESOURCE = "ponderer_client_addon_messages.yml";
    private static final Map<String, String> defaults = new LinkedHashMap<>();
    private static final Map<String, String> values = new LinkedHashMap<>();

    static {
        loadDefaults();
        values.putAll(defaults);
    }

    private PondererAddonMessages() {}

    public static void load(Path configDir) {
        defaults.clear();
        values.clear();
        loadDefaults();
        values.putAll(defaults);

        Path path = configDir.resolve(RESOURCE);
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path)) {
                writeDefaults(path);
            }
            values.putAll(parse(Files.readString(path, StandardCharsets.UTF_8)));
            appendMissingDefaults(path);
        } catch (IOException ignored) {
        }
    }

    public static String get(String key, Object... args) {
        String raw = values.getOrDefault(key, defaults.getOrDefault(key, key));
        for (int i = 0; i < args.length; i++) {
            raw = raw.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return raw;
    }

    private static void loadDefaults() {
        try (InputStream stream = PondererAddonMessages.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) return;
            defaults.putAll(parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8)));
        } catch (IOException ignored) {
        }
    }

    private static Map<String, String> parse(String content) {
        Map<String, String> parsed = new LinkedHashMap<>();
        for (String rawLine : content.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int split = line.indexOf(':');
            if (split <= 0) continue;
            String key = line.substring(0, split).trim();
            String value = line.substring(split + 1).trim();
            parsed.put(key, unquote(value));
        }
        return parsed;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1)
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\");
            }
        }
        return value;
    }

    private static void writeDefaults(Path path) throws IOException {
        try (InputStream stream = PondererAddonMessages.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream != null) {
                Files.copy(stream, path);
            }
        }
    }

    private static void appendMissingDefaults(Path path) throws IOException {
        Map<String, String> current = parse(Files.readString(path, StandardCharsets.UTF_8));
        StringBuilder missing = new StringBuilder();
        for (Map.Entry<String, String> entry : defaults.entrySet()) {
            if (!current.containsKey(entry.getKey())) {
                missing.append(entry.getKey()).append(": \"")
                        .append(entry.getValue().replace("\\", "\\\\").replace("\"", "\\\""))
                        .append("\"").append(System.lineSeparator());
            }
        }
        if (!missing.isEmpty()) {
            Files.writeString(path, missing.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        }
    }
}
