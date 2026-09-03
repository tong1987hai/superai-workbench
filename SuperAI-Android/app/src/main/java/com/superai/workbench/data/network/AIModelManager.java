package com.superai.workbench.data.network;

import com.superai.workbench.data.local.ConfigManager;
import com.superai.workbench.data.model.AIModel;
import com.superai.workbench.data.model.ChatMessage;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;

import okhttp3.*;
import okio.BufferedSource;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 多模型 API 管理器
 * 支持 DeepSeek / 豆包 / GPT-4o / 通义 / Kimi / Claude 六大模型
 * 直接调用各服务商官方 API，支持动态 Base URL 和模型名配置
 */
public class AIModelManager {
    private static final String TAG = "AIModelManager";
    private static AIModelManager instance;
    private final OkHttpClient client;
    private final Gson gson = new Gson();

    private AIModelManager() {
        client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    public static synchronized AIModelManager getInstance() {
        if (instance == null) instance = new AIModelManager();
        return instance;
    }

    /**
     * 发送聊天请求（流式）
     * @param config 配置管理器（读取动态URL、模型名、API Key）
     * @param model  模型定义
     * @param messages 对话历史
     * @param systemPrompt 可选系统提示词（智能体注入）
     * @param callback 回调
     */
    public void sendChatStream(ConfigManager config, AIModel model,
                               List<ChatMessage> messages,
                               String systemPrompt,
                               ChatStreamCallback callback) {
        String apiKey = config.getApiKey(model.getId());
        if (apiKey.isEmpty()) {
            callback.onError(new IOException("未配置 " + model.getName() + " 的 API Key，请前往设置中添加"));
            return;
        }

        // 从 ConfigManager 读取动态配置的 Base URL 和模型名
        String baseUrl = config.getModelBaseUrl(model.getId());
        String modelName = config.getModelName(model.getId());
        String url = baseUrl + "/chat/completions";

        // 构建请求体
        JsonObject payload = new JsonObject();
        payload.addProperty("model", modelName);
        payload.addProperty("stream", true);
        payload.addProperty("temperature", 0.7);
        payload.addProperty("max_tokens", model.getMaxTokens());

        // 构建消息数组
        JsonArray messagesArray = new JsonArray();

        // 系统提示词
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", systemPrompt);
            messagesArray.add(sysMsg);
        }

        // 对话历史（排除正在流式输出的空消息）
        for (ChatMessage msg : messages) {
            if (msg.getContent() == null || msg.getContent().isEmpty()) continue;
            JsonObject msgObj = new JsonObject();
            msgObj.addProperty("role", msg.getType() == ChatMessage.TYPE_USER ? "user" : "assistant");
            msgObj.addProperty("content", msg.getContent());
            messagesArray.add(msgObj);
        }

        payload.add("messages", messagesArray);

        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .post(RequestBody.create(payload.toString().getBytes(),
                MediaType.parse("application/json")))
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String errorBody = "";
                    try (ResponseBody body = response.body()) {
                        if (body != null) errorBody = body.string();
                    }
                    callback.onError(new IOException("API错误 " + response.code() + ": " + errorBody));
                    return;
                }

                try (ResponseBody body = response.body()) {
                    if (body == null) {
                        callback.onComplete();
                        return;
                    }

                    BufferedSource source = body.source();
                    String line;
                    while ((line = source.readUtf8Line()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if ("[DONE]".equals(data)) {
                                callback.onComplete();
                                return;
                            }
                            try {
                                JsonObject chunk = gson.fromJson(data, JsonObject.class);
                                if (chunk.has("choices") && chunk.getAsJsonArray("choices").size() > 0) {
                                    JsonObject delta = chunk.getAsJsonArray("choices")
                                        .get(0).getAsJsonObject()
                                        .getAsJsonObject("delta");
                                    if (delta != null && delta.has("content")) {
                                        String content = delta.get("content").getAsString();
                                        callback.onChunk(content);
                                    }
                                }
                            } catch (Exception e) {
                                // 忽略单行解析错误，继续读取
                            }
                        }
                    }
                    callback.onComplete();
                }
            }
        });
    }

    /**
     * 兼容旧接口（无 systemPrompt）
     */
    public void sendChatStream(ConfigManager config, AIModel model,
                               List<ChatMessage> messages,
                               ChatStreamCallback callback) {
        sendChatStream(config, model, messages, null, callback);
    }

    /**
     * DALL-E 3 图像生成
     */
    public void generateImage(ConfigManager config, String prompt,
                              String style, ImageCallback callback) {
        String apiKey = config.getApiKey("gpt-4o");
        if (apiKey.isEmpty()) {
            callback.onError(new IOException("未配置 OpenAI API Key，图像生成需要 OpenAI Key"));
            return;
        }

        String baseUrl = config.getModelBaseUrl("gpt-4o");
        String url = baseUrl + "/images/generations";

        JsonObject payload = new JsonObject();
        payload.addProperty("model", "dall-e-3");
        payload.addProperty("prompt", prompt);
        payload.addProperty("n", 1);
        payload.addProperty("size", "1024x1024");

        Request request = new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .post(RequestBody.create(payload.toString().getBytes(),
                MediaType.parse("application/json")))
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError(new IOException("图像生成失败: " + response.code()));
                    return;
                }
                try {
                    String body = response.body().string();
                    JsonObject result = gson.fromJson(body, JsonObject.class);
                    if (result.has("data") && result.getAsJsonArray("data").size() > 0) {
                        String imageUrl = result.getAsJsonArray("data")
                            .get(0).getAsJsonObject()
                            .get("url").getAsString();
                        callback.onSuccess(imageUrl);
                    } else {
                        callback.onError(new IOException("返回结果中没有图片URL"));
                    }
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }

    public interface ChatStreamCallback {
        void onChunk(String content);
        void onComplete();
        void onError(Exception e);
    }

    public interface ImageCallback {
        void onSuccess(String imageUrl);
        void onError(Exception e);
    }
}
