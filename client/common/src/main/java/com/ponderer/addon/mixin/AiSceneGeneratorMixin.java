package com.ponderer.addon.mixin;

import com.ponderer.addon.PondererAddonClient;
import com.ponderer.addon.PondererAddonConfig;
import com.ponderer.addon.PondererAddonMessages;
import com.ponderer.addon.PondererConfigAccess;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

@Pseudo
@Mixin(targets = "com.nododiiiii.ponderer.ai.AiSceneGenerator", remap = false)
public class AiSceneGeneratorMixin {

    @Inject(
        method = "generate",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 0
    )
    private static void onGenerate(
            List<Path> structurePaths,
            String carrierItemId,
            String userPrompt,
            List<String> referenceUrls,
            String existingJson,
            boolean buildTutorial,
            boolean includeImages,
            Consumer<String> onSuccess,
            Consumer<String> onError,
            Consumer<String> onStatus,
            CallbackInfo ci) {

        if (!PondererAddonConfig.isServerAiProxyEnabled() || !PondererConfigAccess.getApiKey().isEmpty()) return;

        ci.cancel();
        onStatus.accept(PondererAddonMessages.get("client.ai_status_connecting"));

        StringBuilder userContent = new StringBuilder();
        userContent.append("Target item: ").append(carrierItemId).append("\n");
        userContent.append("User instruction: ").append(userPrompt).append("\n");

        if (existingJson != null && !existingJson.isBlank()) {
            userContent.append("\nExisting scene JSON to adjust:\n").append(existingJson).append("\n");
        }

        if (!structurePaths.isEmpty()) {
            userContent.append("\nStructures:\n");
            for (Path p : structurePaths) {
                String name = p.getFileName().toString();
                if (name.endsWith(".nbt")) name = name.substring(0, name.length() - 4);
                userContent.append("  - ponderer:").append(name).append("\n");
            }
        }

        if (referenceUrls != null) {
            for (String url : referenceUrls) {
                if (url != null && !url.isBlank()) {
                    userContent.append("\nReference URL: ").append(url);
                }
            }
        }

        String systemPrompt = buildTutorial
                ? "You are a Minecraft Ponder scene author. Generate a tutorial-style Ponder scene JSON."
                : "You are a Minecraft Ponder scene author. Generate a Ponder scene JSON.";

        PondererAddonClient.requestFromServer(
            systemPrompt,
            userContent.toString(),
            result -> Minecraft.getInstance().execute(() -> {
                try {
                    String json = extractJson(result);
                    if (PondererAddonConfig.shouldValidateAiJson()) {
                        validateJson(json);
                    }
                    Path sceneDir = getSceneDir();
                    Files.createDirectories(sceneDir);
                    String filename = carrierItemId.contains(":")
                            ? carrierItemId.substring(carrierItemId.indexOf(':') + 1).replace('/', '_') + ".json"
                            : carrierItemId + ".json";
                    Path outPath = sceneDir.resolve(filename);
                    Files.writeString(outPath, json);
                    reloadScenes();
                    onSuccess.accept(outPath.toString());
                } catch (Exception e) {
                    onError.accept(PondererAddonMessages.get("client.ai_save_failed", e.getMessage()));
                }
            }),
            error -> Minecraft.getInstance().execute(() ->
                    onError.accept(PondererAddonMessages.get("client.ai_server_error", error)))
        );
    }

    private static String extractJson(String text) {
        if (text == null) return "";
        if (text.contains("```")) {
            int start = text.indexOf("```");
            int end = text.lastIndexOf("```");
            if (start != end) {
                String inner = text.substring(start + 3, end).trim();
                if (inner.startsWith("json")) inner = inner.substring(4).trim();
                return inner;
            }
        }
        int first = text.indexOf('{');
        int last = text.lastIndexOf('}');
        if (first >= 0 && last > first) return text.substring(first, last + 1);
        return text;
    }

    private static void validateJson(String json) {
        com.google.gson.JsonElement element = com.google.gson.JsonParser.parseString(json);
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException(PondererAddonMessages.get("client.ai_invalid_json"));
        }
    }

    private static Path getSceneDir() {
        try {
            Class<?> storeClass = Class.forName("com.nododiiiii.ponderer.ponder.SceneStore");
            return (Path) storeClass.getMethod("getSceneDir").invoke(null);
        } catch (Exception e) {
            return Path.of("config", "ponderer", "scripts");
        }
    }

    private static void reloadScenes() {
        try {
            Class<?> storeClass = Class.forName("com.nododiiiii.ponderer.ponder.SceneStore");
            storeClass.getMethod("reloadFromDisk").invoke(null);
        } catch (Exception ignored) {}
    }
}
