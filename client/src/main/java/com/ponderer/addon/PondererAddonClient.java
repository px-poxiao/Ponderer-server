package com.ponderer.addon;

import com.ponderer.addon.network.AiRequestPayload;
import com.ponderer.addon.network.AiResponsePayload;
import com.ponderer.addon.network.ClientCommandPayload;
import com.ponderer.addon.network.PendingAiRequests;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.Minecraft;

import java.util.function.Consumer;

public class PondererAddonClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PondererAddonConfig.load();

        // Register outgoing packet type (client → server)
        PayloadTypeRegistry.playC2S().register(AiRequestPayload.TYPE, AiRequestPayload.CODEC);

        // Register incoming packet types (server → client)
        PayloadTypeRegistry.playS2C().register(AiResponsePayload.TYPE, AiResponsePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ClientCommandPayload.TYPE, ClientCommandPayload.CODEC);

        // Handle server AI response
        ClientPlayNetworking.registerGlobalReceiver(AiResponsePayload.TYPE, (payload, context) -> {
            Minecraft mc = context.client();
            mc.execute(() -> PendingAiRequests.resolve(payload.requestId(), payload.result(), payload.error()));
        });

        // Handle server-relayed client commands
        ClientPlayNetworking.registerGlobalReceiver(ClientCommandPayload.TYPE, (payload, context) -> {
            if (!PondererAddonConfig.isCommandRelayEnabled()) return;
            context.client().execute(() -> ClientCommandDispatcher.dispatch(payload.command()));
        });

        // Start file watcher for local scene change notifications
        if (PondererAddonConfig.isFileWatcherEnabled()) {
            FileWatcherService.start();
        }
    }

    public static void requestFromServer(String systemPrompt, String userContent,
                                          Consumer<String> onSuccess, Consumer<String> onError) {
        String requestId = PendingAiRequests.register(onSuccess, onError);
        String provider = PondererConfigAccess.getProvider();
        ClientPlayNetworking.send(new AiRequestPayload(requestId, provider, systemPrompt, userContent));
    }
}
