package com.ponderer.addon.neoforge;

import com.ponderer.addon.PondererAddonClient;
import com.ponderer.addon.network.AiRequestPayload;
import com.ponderer.addon.network.AiResponsePayload;
import com.ponderer.addon.network.ClientCommandPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(PondererAddonNeoForge.MOD_ID)
public final class PondererAddonNeoForge {

    public static final String MOD_ID = "ponderer_client_addon";
    private static final String NETWORK_VERSION = "1";

    public PondererAddonNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::registerPayloads);
        PondererAddonClient.init(PacketDistributor::sendToServer, FMLPaths.CONFIGDIR.get());
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);

        registrar.playToServer(AiRequestPayload.TYPE, AiRequestPayload.CODEC, (payload, context) -> {
            // The Paper plugin handles this packet on multiplayer servers.
        });
        registrar.playToClient(AiResponsePayload.TYPE, AiResponsePayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> PondererAddonClient.handleAiResponse(
                        payload.requestId(), payload.result(), payload.error())));
        registrar.playToClient(ClientCommandPayload.TYPE, ClientCommandPayload.CODEC, (payload, context) ->
                context.enqueueWork(() -> PondererAddonClient.handleClientCommand(payload)));
    }
}
