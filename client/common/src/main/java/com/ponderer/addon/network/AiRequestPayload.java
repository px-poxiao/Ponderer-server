package com.ponderer.addon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record AiRequestPayload(String requestId, String provider,
                                String systemPrompt, String userContent)
        implements CustomPacketPayload {

    public static final Type<AiRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("ponderer", "ai_request"));

    public static final StreamCodec<FriendlyByteBuf, AiRequestPayload> CODEC =
            StreamCodec.of(AiRequestPayload::encode, AiRequestPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    private static void encode(FriendlyByteBuf buf, AiRequestPayload p) {
        buf.writeUtf(p.requestId());
        buf.writeUtf(p.provider());
        buf.writeUtf(p.systemPrompt() == null ? "" : p.systemPrompt());
        buf.writeUtf(p.userContent() == null ? "" : p.userContent());
    }

    private static AiRequestPayload decode(FriendlyByteBuf buf) {
        return new AiRequestPayload(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readUtf());
    }
}
