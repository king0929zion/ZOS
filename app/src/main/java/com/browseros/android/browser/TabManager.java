package com.browseros.android.browser;

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;

import java.util.ArrayList;
import java.util.List;

/**
 * 标签页管理器
 * 管理多个浏览器标签页
 * 
 * @author BrowserOS Team
 */
public class TabManager {
    private static final String TAG = "TabManager";
    
    private List<BrowserTab> tabs;
    private int currentTabId;
    private Context context;
    
    /**
     * 浏览器标签页类
     */
    public static class BrowserTab {
        private int id;
        private String url;
        private String title;
        private WebView webView;
        private BrowserEngine browserEngine;
        private long createdAt;
        private long lastAccessed;
        
        public BrowserTab(int id, Context context) {
            this.id = id;
            this.createdAt = System.currentTimeMillis();
            this.lastAccessed = System.currentTimeMillis();
            
            // 创建 WebView
            this.webView = new WebView(context);
            this.browserEngine = new BrowserEngine(context, webView);
        }
        
        // Getters and Setters
        public int getId() { return id; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public WebView getWebView() { return webView; }
        public BrowserEngine getBrowserEngine() { return browserEngine; }
        public long getCreatedAt() { return createdAt; }
        public long getLastAccessed() { return lastAccessed; }
        public void updateLastAccessed() { this.lastAccessed = System.currentTimeMillis(); }
    }
    
    /**
     * 构造函数
     * @param context Android 上下文
     */
    public TabManager(Context context) {
        this.context = context;
        this.tabs = new ArrayList<>();
        this.currentTabId = -1;
    }
    
    /**
     * 创建新标签页
     * @param url 初始 URL（可为空）
     * @return 新标签页的 ID
     */
    public int createTab(String url) {
        int newTabId = generateTabId();
        BrowserTab tab = new BrowserTab(newTabId, context);
        
        // 设置 URL 监听器
        tab.getBrowserEngine().setBrowserListener(new BrowserEngine.BrowserListener() {
            @Override
            public void onPageStarted(String url) {
                tab.setUrl(url);
            }
            
            @Override
            public void onPageFinished(String url) {
                tab.setUrl(url);
            }
            
            @Override
            public void onTitleReceived(String title) {
                tab.setTitle(title);
                Log.d(TAG, "标签页 " + tab.getId() + " 标题更新: " + title);
            }
            
            @Override
            public void onProgressChanged(int progress) {
                // 可以在这里更新进度条
            }
            
            @Override
            public void onReceivedError(int errorCode, String description, String failingUrl) {
                Log.e(TAG, "标签页 " + tab.getId() + " 加载错误: " + description);
            }
        });
        
        tabs.add(tab);
        currentTabId = newTabId;
        
        // 如果提供了 URL，加载它
        if (url != null && !url.isEmpty()) {
            tab.getBrowserEngine().loadUrl(url);
        }
        
        Log.d(TAG, "创建新标签页，ID: " + newTabId + ", URL: " + url);
        return newTabId;
    }
    
    /**
     * 关闭标签页
     * @param tabId 标签页 ID
     * @return 是否成功关闭
     */
    public boolean closeTab(int tabId) {
        BrowserTab tab = findTab(tabId);
        if (tab == null) {
            Log.w(TAG, "尝试关闭不存在的标签页: " + tabId);
            return false;
        }
        
        // 销毁 WebView
        tab.getWebView().destroy();
        
        // 从列表中移除
        tabs.remove(tab);
        
        // 如果关闭的是当前标签页，切换到其他标签页
        if (currentTabId == tabId) {
            if (tabs.isEmpty()) {
                currentTabId = -1;
            } else {
                // 切换到最后一个标签页
                currentTabId = tabs.get(tabs.size() - 1).getId();
            }
        }
        
        Log.d(TAG, "关闭标签页: " + tabId);
        return true;
    }
    
    /**
     * 切换标签页
     * @param tabId 要切换到的标签页 ID
     * @return 切换后的标签页，如果不存在返回 null
     */
    public BrowserTab switchTab(int tabId) {
        BrowserTab tab = findTab(tabId);
        if (tab != null) {
            currentTabId = tabId;
            tab.updateLastAccessed();
            Log.d(TAG, "切换到标签页: " + tabId);
        } else {
            Log.w(TAG, "尝试切换到不存在的标签页: " + tabId);
        }
        return tab;
    }
    
    /**
     * 获取当前标签页
     * @return 当前标签页，如果没有则返回 null
     */
    public BrowserTab getCurrentTab() {
        return findTab(currentTabId);
    }
    
    /**
     * 获取所有标签页
     * @return 标签页列表
     */
    public List<BrowserTab> getAllTabs() {
        return new ArrayList<>(tabs);
    }
    
    /**
     * 根据 ID 查找标签页
     * @param tabId 标签页 ID
     * @return 标签页，如果不存在返回 null
     */
    public BrowserTab findTab(int tabId) {
        for (BrowserTab tab : tabs) {
            if (tab.getId() == tabId) {
                return tab;
            }
        }
        return null;
    }
    
    /**
     * 获取标签页数量
     * @return 标签页数量
     */
    public int getTabCount() {
        return tabs.size();
    }
    
    /**
     * 关闭所有标签页
     */
    public void closeAllTabs() {
        for (BrowserTab tab : tabs) {
            tab.getWebView().destroy();
        }
        tabs.clear();
        currentTabId = -1;
        Log.d(TAG, "关闭所有标签页");
    }
    
    /**
     * 生成新的标签页 ID
     * @return 新的标签页 ID
     */
    private int generateTabId() {
        int maxId = 0;
        for (BrowserTab tab : tabs) {
            if (tab.getId() > maxId) {
                maxId = tab.getId();
            }
        }
        return maxId + 1;
    }
}

