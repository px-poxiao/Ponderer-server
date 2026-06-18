package com.ponderer.addon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AiResponsePayload(String requestId, String result, String error)
        implements CustomPacketPayload {

    public static final Type<AiResponsePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("ponderer", "ai_response"));

    public static final StreamCodec<FriendlyByteBuf, AiResponsePayload> CODEC =
            StreamCodec.of(AiResponsePayload::encode, AiResponsePayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    private static void encode(FriendlyByteBuf buf, AiResponsePayload p) {
        buf.writeUtf(p.requestId());
        buf.writeUtf(p.result() == null ? "" : p.result());
        buf.writeUtf(p.error() == null ? "" : p.error());
    }

    private static AiResponsePayload decode(FriendlyByteBuf buf) {
        return new AiResponsePayload(buf.readUtf(), buf.readUtf(), buf.readUtf());
    }
}
