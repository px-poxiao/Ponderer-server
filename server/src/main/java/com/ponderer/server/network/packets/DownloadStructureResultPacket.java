package com.ponderer.server.network.packets;

import com.ponderer.server.network.MinecraftByteBuf;
import io.netty.buffer.ByteBuf;

public record DownloadStructureResultPacket(String sourceId, String targetId, boolean success, String message) {
    public static final String CHANNEL = "ponderer:download_structure_result";

    public byte[] encode() {
        ByteBuf buf = MinecraftByteBuf.create();
        MinecraftByteBuf.writeUtf(buf, sourceId);
        MinecraftByteBuf.writeUtf(buf, targetId);
        buf.writeBoolean(success);
        MinecraftByteBuf.writeUtf(buf, message);
        return MinecraftByteBuf.toBytes(buf);
    }
}