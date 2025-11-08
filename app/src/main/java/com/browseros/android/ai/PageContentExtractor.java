package com.browseros.android.ai;

import android.util.Log;
import android.webkit.WebView;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * 页面内容提取器
 * 从 WebView 中提取页面内容（文本、链接、图片等）
 * 
 * @author BrowserOS Team
 */
public class PageContentExtractor {
    private static final String TAG = "PageContentExtractor";
    private WebView webView;
    private Gson gson;
    
    public PageContentExtractor(WebView webView) {
        this.webView = webView;
        this.gson = new Gson();
    }
    
    /**
     * 提取页面文本内容
     * @param callback 回调接口
     */
    public void extractText(ExtractionCallback callback) {
        String script = "document.body.innerText";
        extractWithScript(script, callback);
    }
    
    /**
     * 提取页面 HTML
     * @param callback 回调接口
     */
    public void extractHTML(ExtractionCallback callback) {
        String script = "document.body.innerHTML";
        extractWithScript(script, callback);
    }
    
    /**
     * 提取所有链接
     * @param callback 回调接口
     */
    public void extractLinks(ExtractionCallback callback) {
        String script = "JSON.stringify(Array.from(document.querySelectorAll('a')).map(a => ({text: a.innerText.trim(), href: a.href})).filter(l => l.href && l.text).slice(0, 100))";
        extractWithScript(script, callback);
    }
    
    /**
     * 提取所有图片
     * @param callback 回调接口
     */
    public void extractImages(ExtractionCallback callback) {
        String script = "JSON.stringify(Array.from(document.querySelectorAll('img')).map(img => ({src: img.src, alt: img.alt || '', width: img.width, height: img.height})).filter(img => img.src).slice(0, 100))";
        extractWithScript(script, callback);
    }
    
    /**
     * 提取表单元素
     * @param callback 回调接口
     */
    public void extractForms(ExtractionCallback callback) {
        String script = "JSON.stringify(Array.from(document.querySelectorAll('form')).map(form => ({action: form.action, method: form.method, inputs: Array.from(form.querySelectorAll('input, textarea, select')).map(input => ({type: input.type, name: input.name, placeholder: input.placeholder || '', value: input.value || ''}))})))";
        extractWithScript(script, callback);
    }
    
    /**
     * 根据选择器提取内容
     * @param selector CSS 选择器
     * @param callback 回调接口
     */
    public void extractBySelector(String selector, ExtractionCallback callback) {
        String escapedSelector = selector.replace("'", "\\'");
        String script = String.format(
            "(() => { const el = document.querySelector('%s'); return el ? el.innerText : ''; })()",
            escapedSelector
        );
        extractWithScript(script, callback);
    }
    
    /**
     * 提取页面结构化信息
     * @param callback 回调接口
     */
    public void extractPageStructure(ExtractionCallback callback) {
        String script = "JSON.stringify({" +
            "title: document.title," +
            "url: window.location.href," +
            "headings: Array.from(document.querySelectorAll('h1, h2, h3, h4, h5, h6')).map(h => ({tag: h.tagName, text: h.innerText.trim()}))," +
            "links: Array.from(document.querySelectorAll('a')).slice(0, 50).map(a => ({text: a.innerText.trim(), href: a.href})).filter(l => l.href)," +
            "images: Array.from(document.querySelectorAll('img')).slice(0, 50).map(img => ({src: img.src, alt: img.alt || ''})).filter(img => img.src)," +
            "forms: Array.from(document.querySelectorAll('form')).map(form => ({action: form.action, method: form.method}))" +
            "})";
        extractWithScript(script, callback);
    }
    
    /**
     * 使用脚本提取内容
     * @param script JavaScript 脚本
     * @param callback 回调接口
     */
    private void extractWithScript(String script, ExtractionCallback callback) {
        if (webView == null) {
            callback.onError("WebView 未初始化");
            return;
        }
        
        webView.evaluateJavascript(script, result -> {
            try {
                // 移除 JSON 字符串的引号
                if (result != null && result.startsWith("\"") && result.endsWith("\"")) {
                    result = result.substring(1, result.length() - 1);
                    // 处理转义字符
                    result = result.replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\");
                }
                
                if (result == null || result.equals("null")) {
                    callback.onError("提取结果为空");
                    return;
                }
                
                callback.onSuccess(result);
            } catch (Exception e) {
                Log.e(TAG, "提取内容失败", e);
                callback.onError("提取失败: " + e.getMessage());
            }
        });
    }
    
    /**
     * 提取回调接口
     */
    public interface ExtractionCallback {
        void onSuccess(String result);
        void onError(String error);
    }
}

