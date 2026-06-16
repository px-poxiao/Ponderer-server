package com.ponderer.server.network.packets;

import com.ponderer.server.network.MinecraftByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public record DownloadStructurePacket(String sourceId) {
    public static final String CHANNEL = "ponderer:download_structure";

    public static DownloadStructurePacket decode(byte[] data) {
        ByteBuf buf = Unpooled.wrappedBuffer(data);
        return new DownloadStructurePacket(MinecraftByteBuf.readUtf(buf));
    }
}