package com.ponderer.server.network.packets;

import com.ponderer.server.network.MinecraftByteBuf;
import io.netty.buffer.ByteBuf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

public record AiResponsePacket(String requestId, String result, String error) {
    public static final String CHANNEL = "ponderer:ai_response";

    public byte[] encode() {
        ByteBuf buf = MinecraftByteBuf.create();
        MinecraftByteBuf.writeUtf(buf, requestId);
        boolean isError = error != null && !error.isBlank();
        buf.writeBoolean(isError);
        MinecraftByteBuf.writeByteArray(buf, compress(isError ? error : (result == null ? "" : result)));
        return MinecraftByteBuf.toBytes(buf);
    }

    private static byte[] compress(String value) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
                gzip.write(value.getBytes(StandardCharsets.UTF_8));
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to compress AI response", e);
        }
    }
}
