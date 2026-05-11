package com.ppg.VPlayer;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SettingsManager {
    private static final String PREF_NAME = "VPlayerPrefs";
    private static final String KEY_FOLDER_URI = "folder_uri";
    private static final String KEY_VIDEO_LIST = "video_list";
    private static final String KEY_SHOW_RECENT = "show_recent";
    private static final String KEY_AST_LIMIT = "ast_limit";
    private static final String KEY_COOL_OFF_PERIOD = "cool_off_period";
    private static final String KEY_RANDOM_THRESHOLD = "random_threshold";
    private static final String KEY_LAST_LIMIT_TIMESTAMP = "last_limit_reached";

    public static void saveFolderPath(Context context, String path) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_FOLDER_URI, path).apply();
    }

    public static String getFolderPath(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_FOLDER_URI, null);
    }

    public static void saveShowRecent(Context context, boolean show) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SHOW_RECENT, show).apply();
    }

    public static boolean isShowRecentEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SHOW_RECENT, false); // Default disabled
    }

    public static void saveASTLimit(Context context, int mins) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_AST_LIMIT, mins).apply();
    }

    public static int getASTLimit(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_AST_LIMIT, 30);
    }

    public static void saveCoolOffPeriod(Context context, int mins) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_COOL_OFF_PERIOD, mins).apply();
    }

    public static int getCoolOffPeriod(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_COOL_OFF_PERIOD, 15);
    }

    public static void saveRandomThreshold(Context context, int count) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_RANDOM_THRESHOLD, count).apply();
    }

    public static int getRandomThreshold(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_RANDOM_THRESHOLD, 5);
    }

    public static void saveLastLimitTimestamp(Context context, long timestamp) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_LIMIT_TIMESTAMP, timestamp).apply();
    }

    public static long getLastLimitTimestamp(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getLong(KEY_LAST_LIMIT_TIMESTAMP, 0);
    }

    public static void saveVideoList(Context context, List<Video> videos) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = new Gson().toJson(videos);
        prefs.edit().putString(KEY_VIDEO_LIST, json).apply();
    }

    public static List<Video> getSavedVideoList(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_VIDEO_LIST, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<Video>>() {}.getType();
        return new Gson().fromJson(json, type);
    }

    public static void clearCache(Context context) {
        try {
            java.io.File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            android.util.Log.e("PPG_VPlayer", "Error clearing cache", e);
        }
    }

    private static boolean deleteDir(java.io.File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new java.io.File(dir, child));
                    if (!success) {
                        return false;
                    }
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }
}