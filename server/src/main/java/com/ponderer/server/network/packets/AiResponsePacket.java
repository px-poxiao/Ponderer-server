package com.ponderer.server.network.packets;

import com.ponderer.server.network.MinecraftByteBuf;
import io.netty.buffer.ByteBuf;

public record AiResponsePacket(String requestId, String result, String error) {
    public static final String CHANNEL = "ponderer:ai_response";

    public byte[] encode() {
        ByteBuf buf = MinecraftByteBuf.create();
        MinecraftByteBuf.writeUtf(buf, requestId);
        MinecraftByteBuf.writeUtf(buf, result == null ? "" : result);
        MinecraftByteBuf.writeUtf(buf, error == null ? "" : error);
        return MinecraftByteBuf.toBytes(buf);
    }
}