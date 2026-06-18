package com.ponderer.addon.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class PendingAiRequests {

    private PendingAiRequests() {}

    public record Callbacks(Consumer<String> onSuccess, Consumer<String> onError) {}

    private static final Map<String, Callbacks> PENDING = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService TIMEOUTS =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "ponderer-ai-request-timeout");
                thread.setDaemon(true);
                return thread;
            });

    public static String register(Consumer<String> onSuccess, Consumer<String> onError,
                                  long timeoutSeconds, String timeoutMessage) {
        String id = UUID.randomUUID().toString();
        PENDING.put(id, new Callbacks(onSuccess, onError));
        TIMEOUTS.schedule(() -> {
            Callbacks cb = PENDING.remove(id);
            if (cb != null) {
                cb.onError().accept(timeoutMessage);
            }
        }, Math.max(1, timeoutSeconds), TimeUnit.SECONDS);
        return id;
    }

    public static void resolve(String requestId, String result, String error) {
        Callbacks cb = PENDING.remove(requestId);
        if (cb == null) return;
        if (error != null && !error.isEmpty()) {
            cb.onError().accept(error);
        } else {
            cb.onSuccess().accept(result);
        }
    }
}
