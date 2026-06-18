package com.ponderer.addon;

import com.ponderer.addon.network.AiRequestPayload;
import com.ponderer.addon.network.ClientCommandPayload;
import com.ponderer.addon.network.PendingAiRequests;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.nio.file.Path;
import java.util.function.Consumer;

public final class PondererAddonClient {

    private static ClientNetwork network;
    private static boolean initialized;

    private PondererAddonClient() {}

    public static void init(ClientNetwork clientNetwork, Path configDir) {
        network = clientNetwork;
        if (initialized) return;
        initialized = true;

        PondererAddonMessages.load(configDir);
        PondererAddonConfig.load(configDir);
        if (PondererAddonConfig.isFileWatcherEnabled()) {
            FileWatcherService.start();
        }
    }

    public static void handleAiResponse(String requestId, String result, String error) {
        Minecraft.getInstance().execute(() -> PendingAiRequests.resolve(requestId, result, error));
    }

    public static void handleClientCommand(ClientCommandPayload payload) {
        if (!PondererAddonConfig.isCommandRelayEnabled()) return;
        Minecraft.getInstance().execute(() -> ClientCommandDispatcher.dispatch(payload.command()));
    }

    public static void requestFromServer(String systemPrompt, String userContent,
                                         Consumer<String> onSuccess, Consumer<String> onError) {
        if (network == null) {
            onError.accept(PondererAddonMessages.get("client.network_not_initialized"));
            return;
        }

        String requestId = PendingAiRequests.register(onSuccess, onError);
        String provider = PondererConfigAccess.getProvider();
        network.sendToServer(new AiRequestPayload(requestId, provider, systemPrompt, userContent));
    }

    @FunctionalInterface
    public interface ClientNetwork {
        void sendToServer(CustomPacketPayload payload);
    }
}
