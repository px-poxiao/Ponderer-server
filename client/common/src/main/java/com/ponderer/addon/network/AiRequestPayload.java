package com.ponderer.addon.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public record AiRequestPayload(String requestId, String provider,
                                String systemPrompt, String userContent)
        implements CustomPacketPayload {

    private static final int MAX_STRING_BYTES = 8 * 1024 * 1024;
    private static final int MAX_BYTE_ARRAY_BYTES = 8 * 1024 * 1024;

    public static final Type<AiRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("ponderer", "ai_request"));

    public static final StreamCodec<FriendlyByteBuf, AiRequestPayload> CODEC =
            StreamCodec.of(AiRequestPayload::encode, AiRequestPayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    private static void encode(FriendlyByteBuf buf, AiRequestPayload p) {
        buf.writeUtf(p.requestId(), MAX_STRING_BYTES);
        buf.writeUtf(p.provider(), MAX_STRING_BYTES);
        buf.writeByteArray(compress(p.systemPrompt() == null ? "" : p.systemPrompt()));
        buf.writeByteArray(compress(p.userContent() == null ? "" : p.userContent()));
    }

    private static AiRequestPayload decode(FriendlyByteBuf buf) {
        return new AiRequestPayload(
                buf.readUtf(MAX_STRING_BYTES),
                buf.readUtf(MAX_STRING_BYTES),
                decompress(buf.readByteArray(MAX_BYTE_ARRAY_BYTES)),
                decompress(buf.readByteArray(MAX_BYTE_ARRAY_BYTES)));
    }

    private static byte[] compress(String value) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
                gzip.write(value.getBytes(StandardCharsets.UTF_8));
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to compress AI request", e);
        }
    }

    private static String decompress(byte[] bytes) {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decompress AI request", e);
        }
    }
}
