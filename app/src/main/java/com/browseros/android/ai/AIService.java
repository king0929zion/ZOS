package com.browseros.android.ai;

/**
 * AI 服务接口
 * 定义统一的 AI 服务方法
 * 
 * @author BrowserOS Team
 */
public interface AIService {
    /**
     * 发送聊天消息
     * @param message 用户消息
     * @param callback 回调接口
     */
    void chat(String message, AICallback callback);
    
    /**
     * 发送聊天消息（支持工具调用）
     * @param message 用户消息
     * @param tools 可用工具定义（JSON格式）
     * @param callback 回调接口
     */
    default void chatWithTools(String message, org.json.JSONArray tools, AICallback callback) {
        // 默认实现：回退到普通聊天
        chat(message, callback);
    }
    
    /**
     * 分析网页内容
     * @param url 网页 URL
     * @param callback 回调接口
     */
    void analyzePage(String url, AICallback callback);
    
    /**
     * 提取网页数据
     * @param url 网页 URL
     * @param selector CSS 选择器或 XPath
     * @param callback 回调接口
     */
    void extractData(String url, String selector, AICallback callback);
    
    /**
     * 自动化任务
     * @param task 任务描述
     * @param callback 回调接口
     */
    void automateTask(String task, AICallback callback);
    
    /**
     * AI 回调接口
     */
    interface AICallback {
        /**
         * 成功回调
         * @param response AI 响应内容
         */
        void onSuccess(String response);
        
        /**
         * 错误回调
         * @param error 错误信息
         */
        void onError(String error);
    }
}

