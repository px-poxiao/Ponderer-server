package com.ponderer.server.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;

public final class OpenAiProvider implements AiProvider {

    private static final String DEFAULT_BASE = "https://api.openai.com";
    private static final String DEFAULT_MODEL = "gpt-4o";
    private static final Gson GSON = new Gson();
    private final OkHttpClient client;

    public OpenAiProvider(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public CompletableFuture<String> generate(String systemPrompt, String userContent,
                                               String baseUrl, String apiKey, String model, int maxTokens) {
        String base = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE : baseUrl.trim();
        String mdl  = (model == null || model.isBlank()) ? DEFAULT_MODEL : model;

        JsonArray messages = new JsonArray();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content", systemPrompt);
            messages.add(sys);
        }
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", userContent);
        messages.add(user);

        JsonObject body = new JsonObject();
        body.addProperty("model", mdl);
        body.addProperty("max_tokens", maxTokens);
        body.add("messages", messages);

        Request request = new Request.Builder()
                .url(chatCompletionsUrl(base))
                .header("Authorization", "Bearer " + apiKey)
                .header("content-type", "application/json")
                .post(RequestBody.create(GSON.toJson(body), MediaType.get("application/json")))
                .build();

        CompletableFuture<String> future = new CompletableFuture<>();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) { future.completeExceptionally(e); }
            @Override public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody rb = response.body()) {
                    String json = rb != null ? rb.string() : "";
                    if (!response.isSuccessful()) {
                        future.completeExceptionally(new IOException("OpenAI API error " + response.code() + ": " + json));
                        return;
                    }
                    JsonObject obj = GSON.fromJson(json, JsonObject.class);
                    String text = obj.getAsJsonArray("choices").get(0).getAsJsonObject()
                            .getAsJsonObject("message").get("content").getAsString();
                    future.complete(text);
                }
            }
        });
        return future;
    }

    private static String chatCompletionsUrl(String baseUrl) {
        String base = stripTrailingSlash(baseUrl);
        if (base.endsWith("/chat/completions")) {
            return base;
        }
        if (base.endsWith("/v1")) {
            return base + "/chat/completions";
        }
        if (isDeepSeek(base)) {
            return base + "/chat/completions";
        }
        return base + "/v1/chat/completions";
    }

    private static String stripTrailingSlash(String value) {
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static boolean isDeepSeek(String baseUrl) {
        try {
            String host = URI.create(baseUrl).getHost();
            return host != null && host.equalsIgnoreCase("api.deepseek.com");
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }
}
