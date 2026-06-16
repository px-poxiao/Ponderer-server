package com.ponderer.server.listener;

import com.ponderer.server.config.PluginConfig;
import com.ponderer.server.storage.SubscriptionStore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener implements Listener {

    private final SubscriptionStore subscriptions;
    private final PluginConfig config;

    public PlayerJoinListener(SubscriptionStore subscriptions, PluginConfig config) {
        this.subscriptions = subscriptions;
        this.config = config;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!config.isSubscriptionsEnabled()) return;
        subscriptions.drainNotifications(event.getPlayer().getUniqueId())
                .forEach(event.getPlayer()::sendMessage);
    }
}
