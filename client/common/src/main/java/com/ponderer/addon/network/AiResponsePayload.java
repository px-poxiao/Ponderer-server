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

public record AiResponsePayload(String requestId, String result, String error)
        implements CustomPacketPayload {

    private static final int MAX_STRING_BYTES = 8 * 1024 * 1024;
    private static final int MAX_BYTE_ARRAY_BYTES = 8 * 1024 * 1024;

    public static final Type<AiResponsePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("ponderer", "ai_response"));

    public static final StreamCodec<FriendlyByteBuf, AiResponsePayload> CODEC =
            StreamCodec.of(AiResponsePayload::encode, AiResponsePayload::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }

    private static void encode(FriendlyByteBuf buf, AiResponsePayload p) {
        buf.writeUtf(p.requestId(), MAX_STRING_BYTES);
        boolean error = p.error() != null && !p.error().isEmpty();
        buf.writeBoolean(error);
        buf.writeByteArray(compress(error ? p.error() : (p.result() == null ? "" : p.result())));
    }

    private static AiResponsePayload decode(FriendlyByteBuf buf) {
        String requestId = buf.readUtf(MAX_STRING_BYTES);
        boolean error = buf.readBoolean();
        String value = decompress(buf.readByteArray(MAX_BYTE_ARRAY_BYTES));
        return error
                ? new AiResponsePayload(requestId, null, value)
                : new AiResponsePayload(requestId, value, null);
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

    private static String decompress(byte[] bytes) {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decompress AI response", e);
        }
    }
}
