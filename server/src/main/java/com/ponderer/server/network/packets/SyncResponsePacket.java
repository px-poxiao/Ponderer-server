package com.ponderer.server.network.packets;

import com.ponderer.server.network.MinecraftByteBuf;
import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

public record SyncResponsePacket(List<FileEntry> scripts, List<FileEntry> structures, boolean finalChunk,
                                 int serverSkippedCount) {
    public static final String CHANNEL = "ponderer:sync_response";

    public record FileEntry(String id, String pack, byte[] bytes) {
        public FileEntry(String id, byte[] bytes) {
            this(id, null, bytes);
        }
    }

    public SyncResponsePacket(List<FileEntry> scripts, List<FileEntry> structures) {
        this(scripts, structures, true, 0);
    }

    public byte[] encode() {
        ByteBuf buf = MinecraftByteBuf.create();
        MinecraftByteBuf.writeVarInt(buf, scripts.size());
        for (FileEntry e : scripts) {
            MinecraftByteBuf.writeUtf(buf, e.id());
            MinecraftByteBuf.writeOptionalUtf(buf, e.pack());
            MinecraftByteBuf.writeByteArray(buf, e.bytes());
        }
        MinecraftByteBuf.writeVarInt(buf, structures.size());
        for (FileEntry e : structures) {
            MinecraftByteBuf.writeUtf(buf, e.id());
            MinecraftByteBuf.writeOptionalUtf(buf, e.pack());
            MinecraftByteBuf.writeByteArray(buf, e.bytes());
        }
        buf.writeBoolean(finalChunk);
        MinecraftByteBuf.writeVarInt(buf, serverSkippedCount);
        return MinecraftByteBuf.toBytes(buf);
    }
}
