package com.ponderer.server.network.packets;

public record SyncRequestPacket() {
    public static final String CHANNEL = "ponderer:sync_request";
}