package com.ponderer.server.network.packets;

import com.ponderer.server.network.MinecraftByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public record ClientCommandPacket(String command) {
    public static final String CHANNEL = "ponderer:client_command";

    public void send(Player player, Plugin plugin) {
        var buf = Unpooled.buffer();
        MinecraftByteBuf.writeUtf(buf, command);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        buf.release();
        player.sendPluginMessage(plugin, CHANNEL, bytes);
    }
}
