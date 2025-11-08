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
 * Ollama 本地模型提供者
 * 实现 Ollama 本地模型的 AI 服务
 * 
 * @author BrowserOS Team
 */
public class OllamaProvider implements AIService {
    private static final String TAG = "OllamaProvider";
    private static final String DEFAULT_BASE_URL = "http://localhost:11434";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    private String baseUrl;
    private String model;
    private OkHttpClient httpClient;
    private Gson gson;
    
    /**
     * 构造函数
     * @param baseUrl Ollama 服务器地址（如 http://localhost:11434）
     * @param model 使用的模型名称（如 llama2, mistral）
     */
    public OllamaProvider(String baseUrl, String model) {
        this.baseUrl = baseUrl != null && !baseUrl.isEmpty() ? baseUrl : DEFAULT_BASE_URL;
        this.model = model != null && !model.isEmpty() ? model : "llama2";
        
        // 对于本地服务器，使用更长的超时时间
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS) // 本地模型可能需要更长时间
                .build();
        this.gson = new Gson();
    }
    
    /**
     * 构造函数（使用默认地址）
     * @param model 使用的模型名称
     */
    public OllamaProvider(String model) {
        this(DEFAULT_BASE_URL, model);
    }
    
    @Override
    public void chat(String message, AICallback callback) {
        try {
            String apiUrl = baseUrl + "/api/generate";
            
            // 构建请求体
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);
            requestBody.addProperty("prompt", message);
            requestBody.addProperty("stream", false);
            
            // 创建请求
            RequestBody body = RequestBody.create(
                    gson.toJson(requestBody), JSON);
            
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();
            
            // 发送请求
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Ollama API 请求失败", e);
                    callback.onError("无法连接到 Ollama 服务器: " + e.getMessage() + 
                            "\n请确保 Ollama 正在运行，并且地址正确: " + baseUrl);
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "未知错误";
                        Log.e(TAG, "Ollama API 错误: " + response.code() + " - " + errorBody);
                        callback.onError("API 错误: " + response.code());
                        return;
                    }
                    
                    String responseBody = response.body().string();
                    try {
                        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                        String content = jsonResponse.get("response").getAsString();
                        
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
     * 设置服务器地址
     * @param baseUrl 新的服务器地址
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    /**
     * 设置模型
     * @param model 模型名称
     */
    public void setModel(String model) {
        this.model = model;
    }
    
    /**
     * 获取服务器地址
     * @return 服务器地址
     */
    public String getBaseUrl() {
        return baseUrl;
    }
}

