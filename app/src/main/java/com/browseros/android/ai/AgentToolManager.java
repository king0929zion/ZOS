package com.browseros.android.ai;

import android.content.Context;
import android.util.Log;

import com.browseros.android.browser.BrowserEngine;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI Agent 工具管理器
 * 管理所有可用的浏览器操作工具
 * 
 * @author BrowserOS Team
 */
public class AgentToolManager {
    private static final String TAG = "AgentToolManager";
    
    private Map<String, AgentTool> tools;
    private BrowserEngine browserEngine;
    private Context context;
    
    public AgentToolManager(Context context, BrowserEngine browserEngine) {
        this.context = context;
        this.browserEngine = browserEngine;
        this.tools = new HashMap<>();
        registerDefaultTools();
    }
    
    /**
     * 注册默认工具
     */
    private void registerDefaultTools() {
        // 导航工具
        registerTool(new NavigateTool());
        registerTool(new ClickTool());
        registerTool(new TypeTool());
        registerTool(new ScrollTool());
        registerTool(new WaitTool());
        
        // 页面操作工具
        registerTool(new ExtractContentTool());
        registerTool(new GetPageInfoTool());
        registerTool(new ExecuteScriptTool());
        
        // 浏览器控制工具
        registerTool(new GoBackTool());
        registerTool(new GoForwardTool());
        registerTool(new ReloadTool());
        registerTool(new NewTabTool());
    }
    
    /**
     * 注册工具
     * @param tool 工具实例
     */
    public void registerTool(AgentTool tool) {
        tools.put(tool.getName(), tool);
        Log.d(TAG, "注册工具: " + tool.getName());
    }
    
    /**
     * 获取所有工具
     * @return 工具列表
     */
    public List<AgentTool> getAllTools() {
        return new ArrayList<>(tools.values());
    }
    
    /**
     * 获取工具定义（用于 AI 模型）
     * @return 工具定义 JSON 数组
     */
    public JSONArray getToolsDefinition() {
        JSONArray toolsArray = new JSONArray();
        for (AgentTool tool : tools.values()) {
            try {
                JSONObject toolDef = new JSONObject();
                toolDef.put("type", "function");
                toolDef.put("name", tool.getName());
                toolDef.put("description", tool.getDescription());
                
                JSONObject parameters = new JSONObject();
                parameters.put("type", "object");
                parameters.put("properties", tool.getParameters());
                toolDef.put("parameters", parameters);
                
                toolsArray.put(toolDef);
            } catch (Exception e) {
                Log.e(TAG, "生成工具定义失败: " + tool.getName(), e);
            }
        }
        return toolsArray;
    }
    
    /**
     * 执行工具
     * @param toolName 工具名称
     * @param arguments 工具参数
     * @return 执行结果
     */
    public String executeTool(String toolName, JSONObject arguments) {
        AgentTool tool = tools.get(toolName);
        if (tool == null) {
            return "错误: 未找到工具 " + toolName;
        }
        
        try {
            return tool.execute(arguments);
        } catch (Exception e) {
            Log.e(TAG, "执行工具失败: " + toolName, e);
            return "错误: " + e.getMessage();
        }
    }
    
    // ========== 默认工具实现 ==========
    
    /**
     * 导航工具
     */
    private class NavigateTool implements AgentTool {
        @Override
        public String getName() {
            return "navigate";
        }
        
        @Override
        public String getDescription() {
            return "导航到指定URL";
        }
        
        @Override
        public JSONObject getParameters() {
            try {
                JSONObject params = new JSONObject();
                JSONObject urlParam = new JSONObject();
                urlParam.put("type", "string");
                urlParam.put("description", "要导航到的URL");
                params.put("url", urlParam);
                return params;
            } catch (Exception e) {
                return new JSONObject();
            }
        }
        
        @Override
        public String execute(JSONObject arguments) {
            try {
                String url = arguments.getString("url");
                browserEngine.loadUrl(url);
                return "已导航到: " + url;
            } catch (Exception e) {
                return "错误: " + e.getMessage();
            }
        }
    }
    
    /**
     * 点击工具
     */
    private class ClickTool implements AgentTool {
        @Override
        public String getName() {
            return "click";
        }
        
        @Override
        public String getDescription() {
            return "点击页面上的元素";
        }
        
        @Override
        public JSONObject getParameters() {
            try {
                JSONObject params = new JSONObject();
                JSONObject selectorParam = new JSONObject();
                selectorParam.put("type", "string");
                selectorParam.put("description", "CSS选择器或XPath");
                params.put("selector", selectorParam);
                return params;
            } catch (Exception e) {
                return new JSONObject();
            }
        }
        
        @Override
        public String execute(JSONObject arguments) {
            try {
                String selector = arguments.getString("selector");
                String script = String.format(
                    "document.querySelector('%s')?.click();",
                    selector.replace("'", "\\'")
                );
                browserEngine.evaluateJavaScript(script);
                return "已点击元素: " + selector;
            } catch (Exception e) {
                return "错误: " + e.getMessage();
            }
        }
    }
    
    /**
     * 输入工具
     */
    private class TypeTool implements AgentTool {
        @Override
        public String getName() {
            return "type";
        }
        
        @Override
        public String getDescription() {
            return "在输入框中输入文本";
        }
        
        @Override
        public JSONObject getParameters() {
            try {
                JSONObject params = new JSONObject();
                JSONObject selectorParam = new JSONObject();
                selectorParam.put("type", "string");
                selectorParam.put("description", "输入框的CSS选择器");
                params.put("selector", selectorParam);
                
                JSONObject textParam = new JSONObject();
                textParam.put("type", "string");
                textParam.put("description", "要输入的文本");
                params.put("text", textParam);
                return params;
            } catch (Exception e) {
                return new JSONObject();
            }
        }
        
        @Override
        public String execute(JSONObject arguments) {
            try {
                String selector = arguments.getString("selector");
                String text = arguments.getString("text");
                String script = String.format(
                    "(function() { var el = document.querySelector('%s'); if (el) { el.value = '%s'; el.dispatchEvent(new Event('input', { bubbles: true })); el.dispatchEvent(new Event('change', { bubbles: true })); } })();",
                    selector.replace("'", "\\'"),
                    text.replace("'", "\\'")
                );
                browserEngine.evaluateJavaScript(script);
                return "已在 " + selector + " 输入: " + text;
            } catch (Exception e) {
                return "错误: " + e.getMessage();
            }
        }
    }
    
    /**
     * 滚动工具
     */
    private class ScrollTool implements AgentTool {
        @Override
        public String getName() {
            return "scroll";
        }
        
        @Override
        public String getDescription() {
            return "滚动页面";
        }
        
        @Override
        public JSONObject getParameters() {
            try {
                JSONObject params = new JSONObject();
                JSONObject directionParam = new JSONObject();
                directionParam.put("type", "string");
                directionParam.put("enum", new JSONArray().put("up").put("down").put("top").put("bottom"));
                directionParam.put("description", "滚动方向");
                params.put("direction", directionParam);
                
                JSONObject amountParam = new JSONObject();
                amountParam.put("type", "number");
                amountParam.put("description", "滚动距离（像素），仅用于up/down");
                params.put("amount", amountParam);
                return params;
            } catch (Exception e) {
                return new JSONObject();
            }
        }
        
        @Override
        public String execute(JSONObject arguments) {
            try {
                String direction = arguments.getString("direction");
                String script;
                switch (direction) {
                    case "up":
                        int amountUp = arguments.optInt("amount", 300);
                        script = String.format("window.scrollBy(0, -%d);", amountUp);
                        break;
                    case "down":
                        int amountDown = arguments.optInt("amount", 300);
                        script = String.format("window.scrollBy(0, %d);", amountDown);
                        break;
                    case "top":
                        script = "window.scrollTo(0, 0);";
                        break;
                    case "bottom":
                        script = "window.scrollTo(0, document.body.scrollHeight);";
                        break;
                    default:
                        return "错误: 无效的滚动方向";
                }
                browserEngine.evaluateJavaScript(script);
                return "已滚动: " + direction;
            } catch (Exception e) {
                return "错误: " + e.getMessage();
            }
        }
    }
    
    /**
     * 等待工具
     */
    private class WaitTool implements AgentTool {
        @Override
        public String getName() {
            return "wait";
        }
        
        @Override
        public String getDescription() {
            return "等待指定时间（毫秒）";
        }
        
        @Override
        public JSONObject getParameters() {
            try {
                JSONObject params = new JSONObject();
                JSONObject msParam = new JSONObject();
                msParam.put("type", "number");
                msParam.put("description", "等待时间（毫秒）");
                params.put("ms", msParam);
                return params;
            } catch (Exception e) {
                return new JSONObject();
            }
        }
        
        @Override
        public String execute(JSONObject arguments) {
            try {
                int ms = arguments.getInt("ms");
                Thread.sleep(ms);
                return "已等待 " + ms + " 毫秒";
            } catch (Exception e) {
                return "错误: " + e.getMessage();
            }
        }
    }
    
    /**
     * 提取内容工具
     */
    private class ExtractContentTool implements AgentTool {
        @Override
        public String getName() {
            return "extract_content";
        }
        
        @Override
        public String getDescription() {
            return "提取页面内容（文本、链接、图片等）";
        }
        
        @Override
        public JSONObject getParameters() {
            try {
                JSONObject params = new JSONObject();
                JSONObject selectorParam = new JSONObject();
                selectorParam.put("type", "string");
                selectorParam.put("description", "CSS选择器，留空则提取整个页面");
                params.put("selector", selectorParam);
                
                JSONObject typeParam = new JSONObject();
                typeParam.put("type", "string");
                typeParam.put("enum", new JSONArray().put("text").put("html").put("links").put("images"));
                typeParam.put("description", "提取类型");
                params.put("type", typeParam);
                return params;
            } catch (Exception e) {
                return new JSONObject();
            }
        }
        
        @Override
        public String execute(JSONObject arguments) {
            try {
                String selector = arguments.optString("selector", "");
                String type = arguments.getString("type");
                
                String script;
                switch (type) {
                    case "text":
                        if (selector.isEmpty()) {
                            script = "document.body.innerText";
                        } else {
                            script = String.format("document.querySelector('%s')?.innerText || ''", selector.replace("'", "\\'"));
                        }
                        break;
                    case "html":
                        if (selector.isEmpty()) {
                            script = "document.body.innerHTML";
                        } else {
                            script = String.format("document.querySelector('%s')?.innerHTML || ''", selector.replace("'", "\\'"));
                        }
                        break;
                    case "links":
                        script = "Array.from(document.querySelectorAll('a')).map(a => ({text: a.innerText, href: a.href})).filter(l => l.href).slice(0, 50)";
                        break;
                    case "images":
                        script = "Array.from(document.querySelectorAll('img')).map(img => ({src: img.src, alt: img.alt})).slice(0, 50)";
                        break;
                    default:
                        return "错误: 无效的提取类型";
                }
                
                // 注意：这里需要异步获取结果，实际实现中应该使用回调
                browserEngine.evaluateJavaScript("(function() { return " + script + "; })()");
                return "正在提取内容...";
            } catch (Exception e) {
                return "错误: " + e.getMessage();
            }
        }
    }
    
    /**
     * 获取页面信息工具
     */
    private class GetPageInfoTool implements AgentTool {
        @Override
        public String getName() {
            return "get_page_info";
        }
        
        @Override
        public String getDescription() {
            return "获取当前页面信息（URL、标题等）";
        }
        
        @Override
        public JSONObject getParameters() {
            return new JSONObject();
        }
        
        @Override
        public String execute(JSONObject arguments) {
            try {
                String url = browserEngine.getUrl();
                String title = browserEngine.getTitle();
                return String.format("URL: %s\n标题: %s", url, title);
            } catch (Exception e) {
                return "错误: " + e.getMessage();
            }
        }
    }
    
    /**
     * 执行脚本工具
     */
    private class ExecuteScriptTool implements AgentTool {
        @Override
        public String getName() {
            return "execute_script";
        }
        
        @Override
        public String getDescription() {
            return "在页面中执行JavaScript代码";
        }
        
        @Override
        public JSONObject getParameters() {
            try {
                JSONObject params = new JSONObject();
                JSONObject scriptParam = new JSONObject();
                scriptParam.put("type", "string");
                scriptParam.put("description", "要执行的JavaScript代码");
                params.put("script", scriptParam);
                return params;
            } catch (Exception e) {
                return new JSONObject();
            }
        }
        
        @Override
        public String execute(JSONObject arguments) {
            try {
                String script = arguments.getString("script");
                browserEngine.evaluateJavaScript(script);
                return "已执行脚本";
            } catch (Exception e) {
                return "错误: " + e.getMessage();
            }
        }
    }
    
    /**
     * 后退工具
     */
    private class GoBackTool implements AgentTool {
        @Override
        public String getName() {
            return "go_back";
        }
        
        @Override
        public String getDescription() {
            return "浏览器后退";
        }
        
        @Override
        public JSONObject getParameters() {
            return new JSONObject();
        }
        
        @Override
        public String execute(JSONObject arguments) {
            if (browserEngine.goBack()) {
                return "已后退";
            }
            return "无法后退";
        }
    }
    
    /**
     * 前进工具
     */
    private class GoForwardTool implements AgentTool {
        @Override
        public String getName() {
            return "go_forward";
        }
        
        @Override
        public String getDescription() {
            return "浏览器前进";
        }
        
        @Override
        public JSONObject getParameters() {
            return new JSONObject();
        }
        
        @Override
        public String execute(JSONObject arguments) {
            if (browserEngine.goForward()) {
                return "已前进";
            }
            return "无法前进";
        }
    }
    
    /**
     * 刷新工具
     */
    private class ReloadTool implements AgentTool {
        @Override
        public String getName() {
            return "reload";
        }
        
        @Override
        public String getDescription() {
            return "刷新当前页面";
        }
        
        @Override
        public JSONObject getParameters() {
            return new JSONObject();
        }
        
        @Override
        public String execute(JSONObject arguments) {
            browserEngine.reload();
            return "已刷新页面";
        }
    }
    
    /**
     * 新标签工具
     */
    private class NewTabTool implements AgentTool {
        @Override
        public String getName() {
            return "new_tab";
        }
        
        @Override
        public String getDescription() {
            return "创建新标签页";
        }
        
        @Override
        public JSONObject getParameters() {
            try {
                JSONObject params = new JSONObject();
                JSONObject urlParam = new JSONObject();
                urlParam.put("type", "string");
                urlParam.put("description", "新标签页的URL（可选）");
                params.put("url", urlParam);
                return params;
            } catch (Exception e) {
                return new JSONObject();
            }
        }
        
        @Override
        public String execute(JSONObject arguments) {
            // 这个需要TabManager支持，暂时返回提示
            return "新标签功能需要TabManager支持";
        }
    }
}

