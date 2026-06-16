package com.ponderer.server.storage;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PushCooldownStore {

    private final ConcurrentHashMap<UUID, Long> lastPush = new ConcurrentHashMap<>();

    /** Returns true if the player can push (cooldown elapsed or disabled). Updates timestamp on success. */
    public boolean tryAcquire(UUID uuid, int cooldownSeconds) {
        if (cooldownSeconds <= 0) return true;
        long now = System.currentTimeMillis();
        Long last = lastPush.get(uuid);
        if (last != null && now - last < (long) cooldownSeconds * 1000L) return false;
        lastPush.put(uuid, now);
        return true;
    }

    /** Returns remaining cooldown seconds (0 if none). */
    public long getRemainingSeconds(UUID uuid, int cooldownSeconds) {
        Long last = lastPush.get(uuid);
        if (last == null) return 0;
        long elapsed = System.currentTimeMillis() - last;
        long remaining = (long) cooldownSeconds * 1000L - elapsed;
        return remaining > 0 ? (remaining / 1000L) + 1 : 0;
    }
}
