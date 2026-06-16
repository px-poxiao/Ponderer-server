package com.ponderer.server.handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ponderer.server.ai.AiProvider;
import com.ponderer.server.ai.AnthropicProvider;
import com.ponderer.server.ai.OpenAiProvider;
import com.ponderer.server.config.PluginConfig;
import com.ponderer.server.network.packets.UploadScenePacket;
import com.ponderer.server.storage.ReviewStore;
import okhttp3.OkHttpClient;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public final class ReviewAiService {

    private final PluginConfig config;
    private final ReviewStore reviewStore;
    private final UploadHandler uploadHandler;
    private final Plugin plugin;
    private final Logger logger;
    private final AiProvider anthropic;
    private final AiProvider openai;

    public ReviewAiService(PluginConfig config, ReviewStore reviewStore,
                           UploadHandler uploadHandler, Plugin plugin, Logger logger) {
        this.config = config;
        this.reviewStore = reviewStore;
        this.uploadHandler = uploadHandler;
        this.plugin = plugin;
        this.logger = logger;
        OkHttpClient client = new OkHttpClient();
        this.anthropic = new AnthropicProvider(client);
        this.openai = new OpenAiProvider(client);
    }

    public void review(UploadScenePacket packet, Player player) {
        reviewStore.enqueue(packet.sceneId(), player.getUniqueId().toString(), packet.json(),
                packet.structures() == null ? List.of() :
                        packet.structures().stream()
                                .filter(e -> e != null && e.id() != null && e.bytes() != null)
                                .map(e -> new ReviewStore.StructureEntry(e.id(), e.bytes()))
                                .toList());

        if (config.getReviewAiApiKey().isBlank()) {
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> handleAiError(packet, player, "Review AI API key is not configured"));
            return;
        }

        String systemPrompt = "You are a Minecraft server content moderator. Reply only yes or no.";
        String userContent = buildPrompt(packet);

        buildProvider().generate(systemPrompt, userContent,
                config.getReviewAiBaseUrl(), config.getReviewAiApiKey(),
                config.getReviewAiModel(), 16)
                .whenComplete((result, err) -> {
                    if (err != null) {
                        logger.warning("Review AI error: " + err.getMessage());
                        plugin.getServer().getScheduler().runTask(plugin,
                                () -> handleAiError(packet, player, err.getMessage()));
                        return;
                    }

                    boolean approved = result != null
                            && result.trim().toLowerCase(Locale.ROOT).startsWith("yes");
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (approved) {
                            ReviewStore.ReviewEntry entry = reviewStore.getEntry(packet.sceneId());
                            if (entry != null) uploadHandler.commitApproved(entry, player);
                        } else {
                            reviewStore.reject(packet.sceneId(), "AI review rejected");
                            if (player.isOnline()) {
                                player.sendMessage("\u00A7cYour scene \u00A7f" + packet.sceneId()
                                        + " \u00A7cwas rejected by AI review.");
                            }
                        }
                    });
                });
    }

    private void handleAiError(UploadScenePacket packet, Player player, String reason) {
        String fallback = config.getReviewAiErrorFallback().toLowerCase(Locale.ROOT);
        ReviewStore.ReviewEntry entry = reviewStore.getEntry(packet.sceneId());
        switch (fallback) {
            case "approve" -> {
                if (entry != null) uploadHandler.commitApproved(entry, player);
            }
            case "reject" -> {
                reviewStore.reject(packet.sceneId(), "AI review error: " + reason);
                if (player.isOnline()) {
                    player.sendMessage("\u00A7cYour scene \u00A7f" + packet.sceneId()
                            + " \u00A7cwas rejected because AI review failed.");
                }
            }
            default -> {
                if (player.isOnline()) {
                    player.sendMessage("\u00A7eScene \u00A7f" + packet.sceneId()
                            + " \u00A7eis waiting for manual review because AI review failed.");
                }
            }
        }
    }

    private String buildPrompt(UploadScenePacket packet) {
        String template = config.getReviewAiSystemPrompt();
        String item = extractCarrierItem(packet.json());
        return template
                .replace("{item}", item != null ? item : "unknown")
                .replace("{scene}", packet.sceneId())
                .replace("{json}", packet.json() != null ? packet.json() : "");
    }

    private String extractCarrierItem(String json) {
        if (json == null) return null;
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("carrierItem")) return obj.get("carrierItem").getAsString();
            if (obj.has("item")) return obj.get("item").getAsString();
        } catch (Exception ignored) {
        }
        return null;
    }

    private AiProvider buildProvider() {
        return "openai".equalsIgnoreCase(config.getReviewAiProvider()) ? openai : anthropic;
    }
}
