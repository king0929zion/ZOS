package com.browseros.android.browser;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 书签管理器：使用 SharedPreferences 持久化存储。
 */
public class BookmarkManager {
    private static final String PREFS = "browseros_bookmarks";
    private static final String KEY_BOOKMARKS = "bookmarks_json";

    private final SharedPreferences preferences;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<BookmarkItem>>() {}.getType();

    public static class BookmarkItem {
        public String title;
        public String url;
        public long timestamp;
    }

    public BookmarkManager(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void addBookmark(String url, String title) {
        if (url == null || url.isEmpty()) {
            return;
        }
        List<BookmarkItem> bookmarks = new ArrayList<>(getBookmarks());
        for (int i = bookmarks.size() - 1; i >= 0; i--) {
            if (url.equalsIgnoreCase(bookmarks.get(i).url)) {
                bookmarks.remove(i);
            }
        }
        BookmarkItem item = new BookmarkItem();
        item.url = url;
        item.title = title == null || title.isEmpty() ? url : title;
        item.timestamp = System.currentTimeMillis();
        bookmarks.add(item);
        persist(bookmarks);
    }

    public List<BookmarkItem> getBookmarks() {
        String json = preferences.getString(KEY_BOOKMARKS, "");
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        List<BookmarkItem> list = gson.fromJson(json, listType);
        if (list == null) {
            return new ArrayList<>();
        }
        list.sort(Comparator.comparingLong((BookmarkItem b) -> b.timestamp).reversed());
        return list;
    }

    public boolean removeBookmark(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        List<BookmarkItem> bookmarks = new ArrayList<>(getBookmarks());
        boolean removed = bookmarks.removeIf(item -> url.equalsIgnoreCase(item.url));
        if (removed) {
            persist(bookmarks);
        }
        return removed;
    }

    public void clearBookmarks() {
        preferences.edit().remove(KEY_BOOKMARKS).apply();
    }

    private void persist(List<BookmarkItem> bookmarks) {
        preferences.edit().putString(KEY_BOOKMARKS, gson.toJson(bookmarks)).apply();
    }
}
