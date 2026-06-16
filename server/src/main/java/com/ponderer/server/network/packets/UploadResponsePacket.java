package com.ponderer.server.network.packets;

import com.ponderer.server.network.MinecraftByteBuf;
import io.netty.buffer.ByteBuf;

public record UploadResponsePacket(String sceneId, String pack, String status) {
    public static final String CHANNEL = "ponderer:upload_response";

    public UploadResponsePacket(String sceneId, String status) {
        this(sceneId, null, status);
    }

    public byte[] encode() {
        ByteBuf buf = MinecraftByteBuf.create();
        MinecraftByteBuf.writeUtf(buf, sceneId);
        MinecraftByteBuf.writeOptionalUtf(buf, pack);
        MinecraftByteBuf.writeUtf(buf, status);
        return MinecraftByteBuf.toBytes(buf);
    }
}
