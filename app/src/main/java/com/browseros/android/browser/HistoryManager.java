package com.browseros.android.browser;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * 历史记录管理器
 * 管理浏览历史记录，使用 SQLite 数据库存储
 * 
 * @author BrowserOS Team
 */
public class HistoryManager {
    private static final String TAG = "HistoryManager";
    private static final String DATABASE_NAME = "browser_history.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_HISTORY = "history";
    
    private HistoryDatabaseHelper dbHelper;
    private Context context;
    
    /**
     * 历史记录项
     */
    public static class HistoryItem {
        private long id;
        private String url;
        private String title;
        private long timestamp;
        private int visitCount;
        
        public HistoryItem(long id, String url, String title, long timestamp, int visitCount) {
            this.id = id;
            this.url = url;
            this.title = title;
            this.timestamp = timestamp;
            this.visitCount = visitCount;
        }
        
        // Getters
        public long getId() { return id; }
        public String getUrl() { return url; }
        public String getTitle() { return title; }
        public long getTimestamp() { return timestamp; }
        public int getVisitCount() { return visitCount; }
    }
    
    /**
     * 数据库帮助类
     */
    private static class HistoryDatabaseHelper extends SQLiteOpenHelper {
        public HistoryDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }
        
        @Override
        public void onCreate(SQLiteDatabase db) {
            String createTable = "CREATE TABLE " + TABLE_HISTORY + " (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "url TEXT NOT NULL, " +
                    "title TEXT, " +
                    "timestamp INTEGER NOT NULL, " +
                    "visit_count INTEGER DEFAULT 1" +
                    ")";
            db.execSQL(createTable);
            
            // 创建索引以提高查询性能
            db.execSQL("CREATE INDEX idx_url ON " + TABLE_HISTORY + " (url)");
            db.execSQL("CREATE INDEX idx_timestamp ON " + TABLE_HISTORY + " (timestamp DESC)");
        }
        
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
            onCreate(db);
        }
    }
    
    /**
     * 构造函数
     * @param context Android 上下文
     */
    public HistoryManager(Context context) {
        this.context = context;
        this.dbHelper = new HistoryDatabaseHelper(context);
    }
    
    /**
     * 添加历史记录
     * 如果 URL 已存在，则更新访问次数和时间戳
     * @param url 网页 URL
     * @param title 网页标题
     */
    public void addHistory(String url, String title) {
        if (url == null || url.isEmpty()) {
            Log.w(TAG, "尝试添加空 URL 到历史记录");
            return;
        }
        
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        
        // 检查 URL 是否已存在
        Cursor cursor = db.query(TABLE_HISTORY, 
                new String[]{"id", "visit_count"}, 
                "url = ?", 
                new String[]{url}, 
                null, null, null);
        
        if (cursor.moveToFirst()) {
            // 更新现有记录
            long id = cursor.getLong(0);
            int visitCount = cursor.getInt(1) + 1;
            
            ContentValues values = new ContentValues();
            values.put("title", title);
            values.put("timestamp", System.currentTimeMillis());
            values.put("visit_count", visitCount);
            
            db.update(TABLE_HISTORY, values, "id = ?", new String[]{String.valueOf(id)});
            Log.d(TAG, "更新历史记录: " + url + " (访问次数: " + visitCount + ")");
        } else {
            // 插入新记录
            ContentValues values = new ContentValues();
            values.put("url", url);
            values.put("title", title != null ? title : url);
            values.put("timestamp", System.currentTimeMillis());
            values.put("visit_count", 1);
            
            db.insert(TABLE_HISTORY, null, values);
            Log.d(TAG, "添加历史记录: " + url);
        }
        
        cursor.close();
        db.close();
    }
    
    /**
     * 获取所有历史记录
     * @return 历史记录列表，按时间倒序排列
     */
    public List<HistoryItem> getHistory() {
        return getHistory(0); // 0 表示获取所有记录
    }
    
    /**
     * 获取历史记录
     * @param limit 限制返回数量，0 表示不限制
     * @return 历史记录列表，按时间倒序排列
     */
    public List<HistoryItem> getHistory(int limit) {
        List<HistoryItem> historyList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String limitClause = limit > 0 ? " LIMIT " + limit : "";
        Cursor cursor = db.query(TABLE_HISTORY, 
                null, 
                null, null, 
                null, null, 
                "timestamp DESC" + limitClause);
        
        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            String url = cursor.getString(1);
            String title = cursor.getString(2);
            long timestamp = cursor.getLong(3);
            int visitCount = cursor.getInt(4);
            
            historyList.add(new HistoryItem(id, url, title, timestamp, visitCount));
        }
        
        cursor.close();
        db.close();
        return historyList;
    }
    
    /**
     * 搜索历史记录
     * @param keyword 搜索关键词
     * @return 匹配的历史记录列表
     */
    public List<HistoryItem> searchHistory(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return getHistory();
        }
        
        List<HistoryItem> historyList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        String searchPattern = "%" + keyword + "%";
        Cursor cursor = db.query(TABLE_HISTORY, 
                null, 
                "url LIKE ? OR title LIKE ?", 
                new String[]{searchPattern, searchPattern}, 
                null, null, 
                "timestamp DESC");
        
        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            String url = cursor.getString(1);
            String title = cursor.getString(2);
            long timestamp = cursor.getLong(3);
            int visitCount = cursor.getInt(4);
            
            historyList.add(new HistoryItem(id, url, title, timestamp, visitCount));
        }
        
        cursor.close();
        db.close();
        return historyList;
    }
    
    /**
     * 删除历史记录
     * @param id 历史记录 ID
     * @return 是否删除成功
     */
    public boolean deleteHistory(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deleted = db.delete(TABLE_HISTORY, "id = ?", new String[]{String.valueOf(id)});
        db.close();
        
        if (deleted > 0) {
            Log.d(TAG, "删除历史记录: " + id);
            return true;
        }
        return false;
    }
    
    /**
     * 清空所有历史记录
     */
    public void clearHistory() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(TABLE_HISTORY, null, null);
        db.close();
        Log.d(TAG, "清空所有历史记录");
    }
    
    /**
     * 删除指定时间之前的历史记录
     * @param beforeTimestamp 时间戳
     * @return 删除的记录数
     */
    public int deleteHistoryBefore(long beforeTimestamp) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deleted = db.delete(TABLE_HISTORY, "timestamp < ?", 
                new String[]{String.valueOf(beforeTimestamp)});
        db.close();
        Log.d(TAG, "删除 " + deleted + " 条历史记录");
        return deleted;
    }
}

