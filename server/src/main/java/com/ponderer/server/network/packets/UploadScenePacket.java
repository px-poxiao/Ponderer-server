package com.ponderer.server.network.packets;

import com.ponderer.server.network.MinecraftByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;

public record UploadScenePacket(
        String sceneId,
        String pack,
        String json,
        List<StructureEntry> structures,
        String mode,
        String lastSyncHash
) {
    public static final String CHANNEL = "ponderer:upload_scene";

    public record StructureEntry(String id, String pack, byte[] bytes) {
        public StructureEntry(String id, byte[] bytes) {
            this(id, null, bytes);
        }
    }

    public UploadScenePacket(String sceneId, String json, List<StructureEntry> structures,
                             String mode, String lastSyncHash) {
        this(sceneId, null, json, structures, mode, lastSyncHash);
    }

    public static UploadScenePacket decode(byte[] data) {
        ByteBuf buf = Unpooled.wrappedBuffer(data);
        String sceneId = MinecraftByteBuf.readUtf(buf);
        String pack = MinecraftByteBuf.readOptionalUtf(buf);
        String json = MinecraftByteBuf.readUtf(buf);
        int size = MinecraftByteBuf.readVarInt(buf);
        List<StructureEntry> structures = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            structures.add(new StructureEntry(
                    MinecraftByteBuf.readUtf(buf),
                    MinecraftByteBuf.readOptionalUtf(buf),
                    MinecraftByteBuf.readByteArray(buf)
            ));
        }
        String mode = MinecraftByteBuf.readUtf(buf);
        String lastSyncHash = MinecraftByteBuf.readUtf(buf);
        return new UploadScenePacket(sceneId, pack, json, structures, mode, lastSyncHash);
    }
}
