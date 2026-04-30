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

    public static void saveFolderPath(Context context, String path) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_FOLDER_URI, path).apply();
    }

    public static String getFolderPath(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_FOLDER_URI, null);
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
}
