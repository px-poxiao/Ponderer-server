package com.ponderer.server.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public final class AnthropicProvider implements AiProvider {

    private static final String DEFAULT_BASE = "https://api.anthropic.com";
    private static final String DEFAULT_MODEL = "claude-sonnet-4-6";
    private static final Gson GSON = new Gson();
    private final OkHttpClient client;

    public AnthropicProvider(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<String> generate(String systemPrompt, String userContent,
                                               String baseUrl, String apiKey, String model, int maxTokens) {
        String base = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE : baseUrl.trim();
        String mdl  = (model == null || model.isBlank()) ? DEFAULT_MODEL : model;

        JsonObject body = new JsonObject();
        body.addProperty("model", mdl);
        body.addProperty("max_tokens", maxTokens);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            body.addProperty("system", systemPrompt);
        }
        JsonArray messages = new JsonArray();
        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userContent);
        messages.add(userMsg);
        body.add("messages", messages);

        Request request = new Request.Builder()
                .url(messagesUrl(base))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("content-type", "application/json")
                .post(RequestBody.create(GSON.toJson(body), MediaType.get("application/json")))
                .build();

        CompletableFuture<String> future = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { future.completeExceptionally(e); }
            @Override public void onResponse(Call call, Response response) {
                try (ResponseBody rb = response.body()) {
                    String json = rb != null ? rb.string() : "";
                    if (!response.isSuccessful()) {
                        future.completeExceptionally(new IOException("Anthropic API error " + response.code() + ": " + json));
                        return;
                    }
                    JsonObject obj = GSON.fromJson(json, JsonObject.class);
                    String text = obj.getAsJsonArray("content").get(0).getAsJsonObject().get("text").getAsString();
                    future.complete(text);
                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            }
        });
        return future;
    }

    public static String messagesUrl(String baseUrl) {
        String base = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE : baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/v1/messages")) {
            return base;
        }
        if (base.endsWith("/v1")) {
            return base + "/messages";
        }
        return base + "/v1/messages";
    }
}
