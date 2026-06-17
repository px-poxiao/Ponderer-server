package com.ponderer.server.ai;

import com.ponderer.server.config.MessageConfig;
import com.ponderer.server.config.PluginConfig;
import com.ponderer.server.network.packets.AiRequestPacket;
import com.ponderer.server.network.packets.AiResponsePacket;
import com.ponderer.server.permissions.PermissionManager;
import com.ponderer.server.stats.StatsTracker;
import com.ponderer.server.storage.PlayerDataStore;
import okhttp3.OkHttpClient;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.logging.Logger;

public final class AiProxyHandler {

    private final PluginConfig config;
    private final PermissionManager permissions;
    private final PlayerDataStore playerData;
    private final StatsTracker stats;
    private final MessageConfig messages;
    private final Plugin plugin;
    private final Logger logger;
    private final AnthropicProvider anthropic;
    private final OpenAiProvider openai;

    public AiProxyHandler(PluginConfig config, PermissionManager permissions,
                          PlayerDataStore playerData, StatsTracker stats,
                          MessageConfig messages, Plugin plugin, Logger logger) {
        this.config = config;
        this.permissions = permissions;
        this.playerData = playerData;
        this.stats = stats;
        this.messages = messages;
        this.plugin = plugin;
        this.logger = logger;
        OkHttpClient client = new OkHttpClient();
        this.anthropic = new AnthropicProvider(client);
        this.openai = new OpenAiProvider(client);
    }

    public void handle(Player player, AiRequestPacket packet) {
        if (!config.isServerAiEnabled()) {
            sendError(player, packet.requestId(), messages.get("feature_disabled", "server_ai"));
            return;
        }
        if (!permissions.canUseServerApi(player)) {
            sendError(player, packet.requestId(), messages.get("ai_no_permission"));
            return;
        }
        if (config.shouldFailIfAiKeyMissing() && config.getAiApiKey().isBlank()) {
            sendError(player, packet.requestId(), messages.get("ai_not_configured"));
            player.sendMessage(messages.get("ai_not_configured"));
            return;
        }

        long estimatedInputTokens = TokenCounter.estimate(packet.systemPrompt())
                + TokenCounter.estimate(packet.userContent());

        long remaining = playerData.getRemainingTokens(player.getUniqueId());
        if (remaining <= 0) {
            sendError(player, packet.requestId(), messages.get("ai_no_tokens"));
            player.sendMessage(messages.get("ai_no_tokens"));
            return;
        }

        String provider = config.isClientProviderOverrideAllowed() && packet.provider() != null && !packet.provider().isBlank()
                ? packet.provider()
                : config.getAiProvider();
        AiProvider aiProvider = "openai".equalsIgnoreCase(provider) ? openai : anthropic;

        aiProvider.generate(
                packet.systemPrompt(),
                packet.userContent(),
                config.getAiBaseUrl(),
                config.getAiApiKey(),
                config.getAiModel(),
                config.getAiMaxTokens()
        ).whenComplete((result, err) -> {
            if (err != null) {
                logger.warning("[Ponderer] AI call failed for " + player.getName() + ": " + err.getMessage());
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> sendError(player, packet.requestId(), messages.get("ai_call_failed", err.getMessage())));
                return;
            }

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                long outputTokens = TokenCounter.estimate(result);
                long totalTokens = estimatedInputTokens + outputTokens;
                playerData.deductTokens(player.getUniqueId(), totalTokens);
                playerData.incrementAiCalls(player.getUniqueId());

                if (config.isLogAiCalls()) {
                    stats.recordAiCall(player.getUniqueId(), player.getName(),
                            estimatedInputTokens, outputTokens, provider);
                }

                long newBalance = playerData.getRemainingTokens(player.getUniqueId());
                player.sendMessage(messages.get("ai_tokens_used", totalTokens, newBalance));

                sendResponse(player, packet.requestId(), result);
            });
        });
    }

    private void sendResponse(Player player, String requestId, String result) {
        if (!player.isOnline()) return;
        player.sendPluginMessage(plugin, AiResponsePacket.CHANNEL,
                new AiResponsePacket(requestId, result, null).encode());
    }

    private void sendError(Player player, String requestId, String error) {
        if (!player.isOnline()) return;
        player.sendPluginMessage(plugin, AiResponsePacket.CHANNEL,
                new AiResponsePacket(requestId, null, error).encode());
    }
}
