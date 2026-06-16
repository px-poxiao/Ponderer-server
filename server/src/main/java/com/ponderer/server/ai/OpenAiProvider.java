package com.ponderer.server.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
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
        String base = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE : baseUrl;
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
                .url(base + "/v1/chat/completions")
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
}