package com.browseros.android.ai;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * AI Agent 执行引擎
 * 协调 AI 模型和工具的执行，实现真正的 Agent 能力
 * 
 * @author BrowserOS Team
 */
public class AgentExecutor {
    private static final String TAG = "AgentExecutor";
    private static final int MAX_ITERATIONS = 10; // 最大迭代次数
    
    private AIService aiService;
    private AgentToolManager toolManager;
    private Gson gson;
    
    public AgentExecutor(AIService aiService, AgentToolManager toolManager) {
        this.aiService = aiService;
        this.toolManager = toolManager;
        this.gson = new Gson();
    }
    
    /**
     * 执行 Agent 任务
     * @param userMessage 用户消息
     * @param callback 回调接口
     */
    public void execute(String userMessage, AgentCallback callback) {
        new Thread(() -> {
            try {
                List<JsonObject> conversationHistory = new ArrayList<>();
                
                // 添加系统提示
                JsonObject systemMessage = new JsonObject();
                systemMessage.addProperty("role", "system");
                systemMessage.addProperty("content", buildSystemPrompt());
                conversationHistory.add(systemMessage);
                
                // 添加用户消息
                JsonObject userMsg = new JsonObject();
                userMsg.addProperty("role", "user");
                userMsg.addProperty("content", userMessage);
                conversationHistory.add(userMsg);
                
                // 开始执行循环
                for (int iteration = 0; iteration < MAX_ITERATIONS; iteration++) {
                    Log.d(TAG, "Agent 迭代 " + (iteration + 1));
                    
                    // 调用 AI 服务（需要支持工具调用）
                    String response = callAIWithTools(conversationHistory);
                    
                    // 解析响应，检查是否有工具调用
                    JsonObject aiResponse = gson.fromJson(response, JsonObject.class);
                    
                    if (aiResponse.has("tool_calls")) {
                        // 有工具调用，执行工具
                        JsonArray toolCalls = aiResponse.getAsJsonArray("tool_calls");
                        JsonArray toolResults = new JsonArray();
                        
                        for (JsonElement toolCallElement : toolCalls) {
                            JsonObject toolCall = toolCallElement.getAsJsonObject();
                            String toolName = toolCall.get("name").getAsString();
                            JsonObject toolArgs = toolCall.getAsJsonObject("arguments");
                            
                            Log.d(TAG, "执行工具: " + toolName);
                            
                            // 执行工具
                            JSONObject argsJson = new JSONObject(toolArgs.toString());
                            String toolResult = toolManager.executeTool(toolName, argsJson);
                            
                            // 记录工具结果
                            JsonObject toolResultObj = new JsonObject();
                            toolResultObj.addProperty("tool_call_id", toolCall.get("id").getAsString());
                            toolResultObj.addProperty("result", toolResult);
                            toolResults.add(toolResultObj);
                        }
                        
                        // 将工具结果添加到对话历史
                        JsonObject assistantMsg = new JsonObject();
                        assistantMsg.addProperty("role", "assistant");
                        assistantMsg.add("tool_calls", toolCalls);
                        conversationHistory.add(assistantMsg);
                        
                        JsonObject toolMsg = new JsonObject();
                        toolMsg.addProperty("role", "tool");
                        toolMsg.add("content", toolResults);
                        conversationHistory.add(toolMsg);
                        
                        // 继续下一轮迭代
                        continue;
                    } else {
                        // 没有工具调用，返回最终结果
                        String finalResponse = aiResponse.get("content").getAsString();
                        callback.onSuccess(finalResponse);
                        return;
                    }
                }
                
                callback.onError("达到最大迭代次数");
            } catch (Exception e) {
                Log.e(TAG, "Agent 执行失败", e);
                callback.onError("执行失败: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * 构建系统提示
     */
    private String buildSystemPrompt() {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是一个智能浏览器助手，可以帮助用户完成各种浏览任务。\n");
        prompt.append("你可以使用以下工具来操作浏览器：\n\n");
        
        // 列出所有可用工具
        for (AgentTool tool : toolManager.getAllTools()) {
            prompt.append("- ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
        }
        
        prompt.append("\n");
        prompt.append("当用户提出任务时，你应该：\n");
        prompt.append("1. 理解用户意图\n");
        prompt.append("2. 选择合适的工具执行操作\n");
        prompt.append("3. 根据结果决定下一步操作\n");
        prompt.append("4. 完成任务后给出总结\n");
        
        return prompt.toString();
    }
    
    /**
     * 调用 AI 服务（支持工具调用）
     */
    private String callAIWithTools(List<JsonObject> conversationHistory) {
        // 构建工具定义
        org.json.JSONArray toolsDef = toolManager.getToolsDefinition();
        
        // 构建消息数组
        com.google.gson.JsonArray messagesArray = new com.google.gson.JsonArray();
        for (JsonObject msg : conversationHistory) {
            messagesArray.add(msg);
        }
        
        // 使用同步调用（在实际应用中应该使用异步）
        final String[] result = new String[1];
        final Exception[] exception = new Exception[1];
        final Object lock = new Object();
        
        // 构建用户消息（从对话历史中提取最后一条）
        String userMessage = conversationHistory.get(conversationHistory.size() - 1).get("content").getAsString();
        
        aiService.chatWithTools(userMessage, toolsDef, new AIService.AICallback() {
            @Override
            public void onSuccess(String response) {
                synchronized (lock) {
                    result[0] = response;
                    lock.notify();
                }
            }
            
            @Override
            public void onError(String error) {
                synchronized (lock) {
                    exception[0] = new Exception(error);
                    lock.notify();
                }
            }
            
            @Override
            public void onToolCall(String toolName, String arguments) {
                // 工具调用会在主流程中处理
            }
        });
        
        // 等待结果
        synchronized (lock) {
            try {
                lock.wait(30000); // 30秒超时
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        if (exception[0] != null) {
            throw new RuntimeException(exception[0]);
        }
        
        if (result[0] == null) {
            throw new RuntimeException("请求超时");
        }
        
        return result[0];
    }
    
    /**
     * Agent 回调接口
     */
    public interface AgentCallback {
        void onSuccess(String result);
        void onError(String error);
    }
}

