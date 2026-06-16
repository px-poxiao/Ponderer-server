package com.ponderer.server.network.packets;

import com.ponderer.server.network.MinecraftByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public record AiRequestPacket(String requestId, String provider, String systemPrompt, String userContent) {
    public static final String CHANNEL = "ponderer:ai_request";

    public static AiRequestPacket decode(byte[] data) {
        ByteBuf buf = Unpooled.wrappedBuffer(data);
        String requestId = MinecraftByteBuf.readUtf(buf);
        String provider = MinecraftByteBuf.readUtf(buf);
        String systemPrompt = MinecraftByteBuf.readUtf(buf);
        String userContent = MinecraftByteBuf.readUtf(buf);
        return new AiRequestPacket(requestId, provider, systemPrompt, userContent);
    }
}