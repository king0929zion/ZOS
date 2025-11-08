package com.browseros.android.privacy;

import android.content.Context;
import android.util.Log;

import com.browseros.android.browser.HistoryManager;

import java.io.File;

/**
 * 数据管理器
 * 管理用户数据，提供数据清除和导出功能
 * 
 * @author BrowserOS Team
 */
public class DataManager {
    private static final String TAG = "DataManager";
    
    private Context context;
    private HistoryManager historyManager;
    private SecureStorage secureStorage;
    
    /**
     * 构造函数
     * @param context Android 上下文
     */
    public DataManager(Context context) {
        this.context = context;
        this.historyManager = new HistoryManager(context);
        this.secureStorage = new SecureStorage(context);
    }
    
    /**
     * 清除所有浏览数据
     * 包括：历史记录、缓存、Cookie、表单数据等
     */
    public void clearAllBrowsingData() {
        Log.d(TAG, "清除所有浏览数据");
        
        // 清除历史记录
        historyManager.clearHistory();
        
        // 清除 WebView 缓存
        clearWebViewCache();
        
        // 清除 Cookie
        clearCookies();
        
        // 清除表单数据
        clearFormData();
        
        Log.d(TAG, "所有浏览数据已清除");
    }
    
    /**
     * 清除历史记录
     */
    public void clearHistory() {
        historyManager.clearHistory();
        Log.d(TAG, "历史记录已清除");
    }
    
    /**
     * 清除指定时间之前的历史记录
     * @param daysAgo 多少天之前
     */
    public void clearHistoryOlderThan(int daysAgo) {
        long timestamp = System.currentTimeMillis() - (daysAgo * 24L * 60 * 60 * 1000);
        historyManager.deleteHistoryBefore(timestamp);
        Log.d(TAG, "已清除 " + daysAgo + " 天前的历史记录");
    }
    
    /**
     * 清除 WebView 缓存
     */
    private void clearWebViewCache() {
        try {
            File cacheDir = context.getCacheDir();
            File webViewCacheDir = new File(cacheDir, "webview");
            if (webViewCacheDir.exists()) {
                deleteDirectory(webViewCacheDir);
            }
            Log.d(TAG, "WebView 缓存已清除");
        } catch (Exception e) {
            Log.e(TAG, "清除 WebView 缓存失败", e);
        }
    }
    
    /**
     * 清除 Cookie
     */
    private void clearCookies() {
        try {
            android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
            cookieManager.removeAllCookies(null);
            cookieManager.flush();
            Log.d(TAG, "Cookie 已清除");
        } catch (Exception e) {
            Log.e(TAG, "清除 Cookie 失败", e);
        }
    }
    
    /**
     * 清除表单数据
     */
    private void clearFormData() {
        try {
            android.webkit.WebStorage.getInstance().deleteAllData();
            Log.d(TAG, "表单数据已清除");
        } catch (Exception e) {
            Log.e(TAG, "清除表单数据失败", e);
        }
    }
    
    /**
     * 获取数据大小（字节）
     * @return 数据大小
     */
    public long getDataSize() {
        long size = 0;
        
        // 计算数据库大小
        File dbFile = context.getDatabasePath("browser_history.db");
        if (dbFile.exists()) {
            size += dbFile.length();
        }
        
        // 计算缓存大小
        File cacheDir = context.getCacheDir();
        size += getDirectorySize(cacheDir);
        
        return size;
    }
    
    /**
     * 格式化数据大小
     * @param bytes 字节数
     * @return 格式化后的字符串（如 "1.5 MB"）
     */
    public String formatDataSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
    
    /**
     * 删除目录及其所有内容
     * @param directory 要删除的目录
     */
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
    
    /**
     * 计算目录大小
     * @param directory 目录
     * @return 目录大小（字节）
     */
    private long getDirectorySize(File directory) {
        long size = 0;
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        size += getDirectorySize(file);
                    } else {
                        size += file.length();
                    }
                }
            }
        }
        return size;
    }
    
    /**
     * 获取历史记录管理器
     * @return HistoryManager 实例
     */
    public HistoryManager getHistoryManager() {
        return historyManager;
    }
    
    /**
     * 获取安全存储实例
     * @return SecureStorage 实例
     */
    public SecureStorage getSecureStorage() {
        return secureStorage;
    }
}

