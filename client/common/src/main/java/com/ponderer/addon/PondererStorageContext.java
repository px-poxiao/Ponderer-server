package com.ponderer.addon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.server.MinecraftServer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.HexFormat;
import java.util.Locale;

public final class PondererStorageContext {

    private static Path configDir = Path.of("config");
    private static String lastContextKey = "";

    private PondererStorageContext() {}

    public static void init(Path rootConfigDir) {
        if (rootConfigDir != null) {
            configDir = rootConfigDir;
        }
        lastContextKey = contextKey();
    }

    public static Path sceneDir() {
        return contextRoot().resolve("scripts");
    }

    public static Path structureDir() {
        return contextRoot().resolve("structures");
    }

    public static Path registryPath() {
        return contextRoot().resolve(".ponderer_registry.json");
    }

    public static Path triggerStatePath() {
        return contextRoot().resolve("trigger_state.json");
    }

    public static Path promptsDir() {
        return contextRoot().resolve("prompts");
    }

    public static Path logsDir() {
        return contextRoot().resolve("logs");
    }

    public static String contextKey() {
        if (!PondererAddonConfig.isStorageSeparationEnabled()) {
            return "shared";
        }

        Minecraft mc = Minecraft.getInstance();
        String singleplayer = singleplayerName(mc);
        if (!singleplayer.isBlank()) {
            return "singleplayer_" + safeName(singleplayer);
        }

        String server = serverAddress(mc);
        if (!server.isBlank()) {
            return "server_" + safeName(server);
        }

        return "shared";
    }

    public static void reloadPondererIfContextChanged() {
        String current = contextKey();
        if (current.equals(lastContextKey)) return;
        lastContextKey = current;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        mc.execute(PondererStorageContext::reloadPonderer);
    }

    private static Path contextRoot() {
        Path base = configDir.resolve("ponderer");
        String key = contextKey();
        if ("shared".equals(key)) {
            return base;
        }
        return base.resolve(PondererAddonConfig.getStorageContextRootDir()).resolve(key);
    }

    private static void reloadPonderer() {
        resetTriggerState();
        try {
            PondererReflection.invokeStatic("com.nododiiiii.ponderer.ponder.PonderPackRegistry", "load");
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            PondererReflection.invokeStatic("com.nododiiiii.ponderer.ponder.SceneStore", "reloadFromDisk");
            PondererReflection.invokeStatic("net.createmod.ponder.foundation.PonderIndex", "reload");
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    private static void resetTriggerState() {
        try {
            Class<?> triggerManager = Class.forName("com.nododiiiii.ponderer.ponder.TriggerManager");
            Field stateLoaded = triggerManager.getDeclaredField("stateLoaded");
            stateLoaded.setAccessible(true);
            stateLoaded.setBoolean(null, false);

            Field shownSet = triggerManager.getDeclaredField("shownSet");
            shownSet.setAccessible(true);
            Object shown = shownSet.get(null);
            if (shown instanceof Set<?> set) {
                ((Set<Object>) set).clear();
            }

            Field readSet = triggerManager.getDeclaredField("readSet");
            readSet.setAccessible(true);
            Object read = readSet.get(null);
            if (read instanceof Set<?> set) {
                ((Set<Object>) set).clear();
            }
        } catch (Exception ignored) {
        }
    }

    private static String singleplayerName(Minecraft mc) {
        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) return "";
        String levelName = server.getWorldData().getLevelName();
        return levelName == null ? "" : levelName.trim();
    }

    private static String serverAddress(Minecraft mc) {
        ServerData serverData = mc.getCurrentServer();
        if (serverData == null) return "";
        if (serverData.ip != null && !serverData.ip.isBlank()) {
            return serverData.ip.trim();
        }
        return serverData.name == null ? "" : serverData.name.trim();
    }

    private static String safeName(String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT)
                .replace(':', '_')
                .replaceAll("[^a-z0-9._-]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) {
            normalized = "unknown";
        }
        if (normalized.length() > 72) {
            normalized = normalized.substring(0, 72);
        }
        return normalized + "_" + shortHash(raw);
    }

    private static String shortHash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 8);
        } catch (Exception ignored) {
            return Integer.toHexString(raw.hashCode());
        }
    }
}
