package com.browseros.android.ai;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * OpenAI API 提供者
 * 实现 OpenAI 规范的 AI 服务，支持所有兼容 OpenAI API 的模型
 * 支持自定义 API URL 和模型名称
 * 
 * @author BrowserOS Team
 */
public class OpenAIProvider implements AIService {
    private static final String TAG = "OpenAIProvider";
    private static final String DEFAULT_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private String apiKey;
    private String apiUrl;
    private String model;
    private OkHttpClient httpClient;
    private Gson gson;
    
    /**
     * 构造函数（使用默认URL和模型）
     * @param apiKey API 密钥
     */
    public OpenAIProvider(String apiKey) {
        this(apiKey, DEFAULT_API_URL, DEFAULT_MODEL);
    }
    
    /**
     * 构造函数（使用默认URL）
     * @param apiKey API 密钥
     * @param model 使用的模型名称（如 gpt-4, gpt-3.5-turbo, claude-3-sonnet 等）
     */
    public OpenAIProvider(String apiKey, String model) {
        this(apiKey, DEFAULT_API_URL, model);
    }
    
    /**
     * 完整构造函数（支持自定义URL和模型）
     * @param apiKey API 密钥
     * @param apiUrl API 端点URL（如 https://api.openai.com/v1/chat/completions 或自定义URL）
     * @param model 使用的模型名称（支持所有OpenAI规范的模型）
     */
    public OpenAIProvider(String apiKey, String apiUrl, String model) {
        this.apiKey = apiKey;
        // 如果URL不包含完整路径，自动添加 /v1/chat/completions
        if (apiUrl != null && !apiUrl.isEmpty()) {
            if (!apiUrl.contains("/chat/completions") && !apiUrl.contains("/v1/")) {
                // 移除末尾的斜杠
                String baseUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
                this.apiUrl = baseUrl + "/v1/chat/completions";
            } else {
                this.apiUrl = apiUrl;
            }
        } else {
            this.apiUrl = DEFAULT_API_URL;
        }
        this.model = (model != null && !model.isEmpty()) ? model : DEFAULT_MODEL;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }
    
    @Override
    public void chat(String message, AICallback callback) {
        if (apiKey == null || apiKey.isEmpty()) {
            callback.onError("OpenAI API 密钥未配置");
            return;
        }
        
        try {
            // 构建请求体
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);
            requestBody.addProperty("temperature", 0.7);
            
            JsonObject messageObj = new JsonObject();
            messageObj.addProperty("role", "user");
            messageObj.addProperty("content", message);
            
            JsonObject[] messages = new JsonObject[]{messageObj};
            requestBody.add("messages", gson.toJsonTree(messages).getAsJsonArray());
            
            // 创建请求
            RequestBody body = RequestBody.create(
                    gson.toJson(requestBody), JSON);
            
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();
            
            // 发送请求
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "OpenAI API 请求失败", e);
                    callback.onError("网络错误: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "未知错误";
                        Log.e(TAG, "OpenAI API 错误: " + response.code() + " - " + errorBody);
                        callback.onError("API 错误: " + response.code());
                        return;
                    }
                    
                    String responseBody = response.body().string();
                    try {
                        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                        String content = jsonResponse.getAsJsonArray("choices")
                                .get(0).getAsJsonObject()
                                .getAsJsonObject("message")
                                .get("content").getAsString();
                        
                        callback.onSuccess(content);
                    } catch (Exception e) {
                        Log.e(TAG, "解析响应失败", e);
                        callback.onError("解析响应失败: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "构建请求失败", e);
            callback.onError("请求构建失败: " + e.getMessage());
        }
    }
    
    @Override
    public void analyzePage(String url, AICallback callback) {
        String prompt = "请分析这个网页的内容和结构: " + url + 
                "\n请提供网页的主要信息、主题和关键内容摘要。";
        chat(prompt, callback);
    }
    
    @Override
    public void extractData(String url, String selector, AICallback callback) {
        String prompt = "请从网页 " + url + " 中提取数据。" +
                "\n选择器: " + selector +
                "\n请提取匹配的数据并以结构化格式返回。";
        chat(prompt, callback);
    }
    
    @Override
    public void automateTask(String task, AICallback callback) {
        String prompt = "请帮我完成以下浏览任务: " + task +
                "\n请提供详细的步骤说明，包括需要访问的网页和操作步骤。";
        chat(prompt, callback);
    }
    
    /**
     * 设置 API 密钥
     * @param apiKey 新的 API 密钥
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    /**
     * 设置 API URL
     * @param apiUrl API 端点URL
     */
    public void setApiUrl(String apiUrl) {
        if (apiUrl != null && !apiUrl.isEmpty()) {
            if (!apiUrl.contains("/chat/completions") && !apiUrl.contains("/v1/")) {
                String baseUrl = apiUrl.endsWith("/") ? apiUrl.substring(0, apiUrl.length() - 1) : apiUrl;
                this.apiUrl = baseUrl + "/v1/chat/completions";
            } else {
                this.apiUrl = apiUrl;
            }
        }
    }
    
    /**
     * 设置模型
     * @param model 模型名称
     */
    public void setModel(String model) {
        if (model != null && !model.isEmpty()) {
            this.model = model;
        }
    }
    
    /**
     * 获取当前 API URL
     * @return API URL
     */
    public String getApiUrl() {
        return apiUrl;
    }
    
    /**
     * 获取当前模型名称
     * @return 模型名称
     */
    public String getModel() {
        return model;
    }
}

