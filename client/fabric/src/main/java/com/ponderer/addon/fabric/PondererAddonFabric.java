package com.ponderer.addon.fabric;

import com.ponderer.addon.PondererAddonClient;
import com.ponderer.addon.network.AiRequestPayload;
import com.ponderer.addon.network.AiResponsePayload;
import com.ponderer.addon.network.ClientCommandPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;

public final class PondererAddonFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.playC2S().register(AiRequestPayload.TYPE, AiRequestPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AiResponsePayload.TYPE, AiResponsePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ClientCommandPayload.TYPE, ClientCommandPayload.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(AiResponsePayload.TYPE, (payload, context) ->
                PondererAddonClient.handleAiResponse(payload.requestId(), payload.result(), payload.error()));
        ClientPlayNetworking.registerGlobalReceiver(ClientCommandPayload.TYPE, (payload, context) ->
                PondererAddonClient.handleClientCommand(payload));

        PondererAddonClient.init(
                payload -> ClientPlayNetworking.send(payload),
                FabricLoader.getInstance().getConfigDir()
        );
    }
}
