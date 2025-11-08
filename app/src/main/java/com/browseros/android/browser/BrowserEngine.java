package com.browseros.android.browser;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * 浏览器引擎类
 * 封装 WebView，提供浏览器核心功能
 * 
 * @author BrowserOS Team
 */
public class BrowserEngine {
    private static final String TAG = "BrowserEngine";
    
    private WebView webView;
    private BrowserListener listener;
    private Context context;
    
    /**
     * 浏览器事件监听器接口
     */
    public interface BrowserListener {
        /**
         * 页面开始加载时调用
         * @param url 正在加载的 URL
         */
        void onPageStarted(String url);
        
        /**
         * 页面加载完成时调用
         * @param url 加载完成的 URL
         */
        void onPageFinished(String url);
        
        /**
         * 页面标题更新时调用
         * @param title 页面标题
         */
        void onTitleReceived(String title);
        
        /**
         * 页面加载进度更新时调用
         * @param progress 加载进度 (0-100)
         */
        void onProgressChanged(int progress);
        
        /**
         * 发生错误时调用
         * @param errorCode 错误代码
         * @param description 错误描述
         * @param failingUrl 失败的 URL
         */
        void onReceivedError(int errorCode, String description, String failingUrl);
    }
    
    /**
     * 构造函数
     * @param context Android 上下文
     * @param webView WebView 实例
     */
    public BrowserEngine(Context context, WebView webView) {
        this.context = context;
        this.webView = webView;
        initializeWebView();
    }
    
    /**
     * 初始化 WebView 设置
     */
    private void initializeWebView() {
        // 启用 JavaScript
        webView.getSettings().setJavaScriptEnabled(true);
        
        // 启用 DOM 存储
        webView.getSettings().setDomStorageEnabled(true);
        
        // 启用数据库存储
        webView.getSettings().setDatabaseEnabled(true);
        
        // 设置缓存模式
        webView.getSettings().setCacheMode(android.webkit.WebSettings.LOAD_DEFAULT);
        
        // 启用缩放功能
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        
        // 设置用户代理
        webView.getSettings().setUserAgentString(
            webView.getSettings().getUserAgentString() + " BrowserOS/0.1.0"
        );
        
        // 设置 WebViewClient
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                Log.d(TAG, "页面开始加载: " + url);
                if (listener != null) {
                    listener.onPageStarted(url);
                }
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "页面加载完成: " + url);
                if (listener != null) {
                    listener.onPageFinished(url);
                }
            }
            
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // 处理 SSL 错误（可以根据需要修改策略）
                Log.w(TAG, "SSL 错误: " + error.toString());
                // 默认继续加载（生产环境应该更严格）
                handler.proceed();
            }
            
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                // 可以在这里拦截请求，实现广告拦截等功能
                return super.shouldInterceptRequest(view, request);
            }
        });
        
        // 设置 WebChromeClient
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                Log.d(TAG, "页面标题: " + title);
                if (listener != null) {
                    listener.onTitleReceived(title);
                }
            }
            
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (listener != null) {
                    listener.onProgressChanged(newProgress);
                }
            }
        });
    }
    
    /**
     * 设置浏览器事件监听器
     * @param listener 监听器实例
     */
    public void setBrowserListener(BrowserListener listener) {
        this.listener = listener;
    }
    
    /**
     * 加载 URL
     * @param url 要加载的 URL
     */
    public void loadUrl(String url) {
        if (url == null || url.isEmpty()) {
            Log.w(TAG, "尝试加载空 URL");
            return;
        }
        
        // 如果 URL 不包含协议，添加 https://
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        
        Log.d(TAG, "加载 URL: " + url);
        webView.loadUrl(url);
    }
    
    /**
     * 后退
     * @return 是否可以后退
     */
    public boolean goBack() {
        if (webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return false;
    }
    
    /**
     * 前进
     * @return 是否可以前进
     */
    public boolean goForward() {
        if (webView.canGoForward()) {
            webView.goForward();
            return true;
        }
        return false;
    }
    
    /**
     * 刷新页面
     */
    public void reload() {
        webView.reload();
    }
    
    /**
     * 停止加载
     */
    public void stopLoading() {
        webView.stopLoading();
    }
    
    /**
     * 获取当前 URL
     * @return 当前 URL
     */
    public String getUrl() {
        return webView.getUrl();
    }
    
    /**
     * 获取当前标题
     * @return 当前标题
     */
    public String getTitle() {
        return webView.getTitle();
    }
    
    /**
     * 检查是否可以后退
     * @return 是否可以后退
     */
    public boolean canGoBack() {
        return webView.canGoBack();
    }
    
    /**
     * 检查是否可以前进
     * @return 是否可以前进
     */
    public boolean canGoForward() {
        return webView.canGoForward();
    }
    
    /**
     * 执行 JavaScript 代码
     * @param script JavaScript 代码
     */
    public void evaluateJavaScript(String script) {
        webView.evaluateJavascript(script, null);
    }
    
    /**
     * 获取 WebView 实例（用于高级操作）
     * @return WebView 实例
     */
    public WebView getWebView() {
        return webView;
    }
    
    /**
     * 清除缓存
     */
    public void clearCache() {
        webView.clearCache(true);
    }
    
    /**
     * 清除历史记录
     */
    public void clearHistory() {
        webView.clearHistory();
    }
}

