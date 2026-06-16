package com.ponderer.server.ratelimit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RateLimiter {

    private final int maxPerHour;
    private final Map<UUID, long[]> buckets = new ConcurrentHashMap<>();

    public RateLimiter(int maxPerHour) {
        this.maxPerHour = maxPerHour;
    }

    /** Returns true if the action is allowed, false if rate-limited. */
    public boolean tryAcquire(UUID uuid) {
        if (maxPerHour <= 0) return true;
        long now = System.currentTimeMillis();
        long windowStart = now - 3_600_000L;
        buckets.compute(uuid, (k, timestamps) -> {
            if (timestamps == null) return new long[]{now};
            // Shift out old entries
            int valid = 0;
            for (long t : timestamps) if (t > windowStart) valid++;
            long[] next = new long[valid + 1];
            int idx = 0;
            for (long t : timestamps) if (t > windowStart) next[idx++] = t;
            next[idx] = now;
            return next;
        });
        long[] current = buckets.get(uuid);
        return current != null && current.length <= maxPerHour;
    }

    public int getUsage(UUID uuid) {
        long windowStart = System.currentTimeMillis() - 3_600_000L;
        long[] ts = buckets.get(uuid);
        if (ts == null) return 0;
        int count = 0;
        for (long t : ts) if (t > windowStart) count++;
        return count;
    }
}