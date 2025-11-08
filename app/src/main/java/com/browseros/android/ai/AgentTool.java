package com.browseros.android.ai;

import org.json.JSONObject;

/**
 * AI Agent 工具接口
 * 定义 AI Agent 可以调用的浏览器操作工具
 * 
 * @author BrowserOS Team
 */
public interface AgentTool {
    /**
     * 工具名称
     * @return 工具名称
     */
    String getName();
    
    /**
     * 工具描述
     * @return 工具描述
     */
    String getDescription();
    
    /**
     * 工具参数定义（JSON Schema格式）
     * @return 参数定义
     */
    JSONObject getParameters();
    
    /**
     * 执行工具
     * @param arguments 工具参数（JSON格式）
     * @return 执行结果
     */
    String execute(JSONObject arguments);
}

