package com.ponderer.addon.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PendingAiRequests {

    private PendingAiRequests() {}

    public record Callbacks(Consumer<String> onSuccess, Consumer<String> onError) {}

    private static final Map<String, Callbacks> PENDING = new ConcurrentHashMap<>();

    public static String register(Consumer<String> onSuccess, Consumer<String> onError) {
        String id = UUID.randomUUID().toString();
        PENDING.put(id, new Callbacks(onSuccess, onError));
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
