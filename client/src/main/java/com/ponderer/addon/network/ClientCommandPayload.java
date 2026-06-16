package com.ponderer.addon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ClientCommandPayload(String command) implements CustomPacketPayload {
    public static final Type<ClientCommandPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("ponderer", "client_command"));
    public static final StreamCodec<FriendlyByteBuf, ClientCommandPayload> CODEC =
            StreamCodec.of(
                    (buf, p) -> buf.writeUtf(p.command()),
                    buf -> new ClientCommandPayload(buf.readUtf())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
