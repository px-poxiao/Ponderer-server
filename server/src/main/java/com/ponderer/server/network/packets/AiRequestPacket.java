package com.ponderer.server.network.packets;

import com.ponderer.server.network.MinecraftByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public record AiRequestPacket(String requestId, String provider, String systemPrompt, String userContent) {
    public static final String CHANNEL = "ponderer:ai_request";

    public static AiRequestPacket decode(byte[] data) {
        ByteBuf buf = Unpooled.wrappedBuffer(data);
        String requestId = MinecraftByteBuf.readUtf(buf);
        String provider = MinecraftByteBuf.readUtf(buf);
        String systemPrompt = decompress(MinecraftByteBuf.readByteArray(buf));
        String userContent = decompress(MinecraftByteBuf.readByteArray(buf));
        return new AiRequestPacket(requestId, provider, systemPrompt, userContent);
    }

    private static String decompress(byte[] bytes) {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decompress AI request", e);
        }
    }
}
