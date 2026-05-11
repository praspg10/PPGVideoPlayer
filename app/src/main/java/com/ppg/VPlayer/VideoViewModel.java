package com.ppg.VPlayer;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VideoViewModel extends AndroidViewModel {
    private final MutableLiveData<List<Video>> videos = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isScanning = new MutableLiveData<>(false);
    private final MutableLiveData<String> currentFolderPath = new MutableLiveData<>();
    private final MutableLiveData<Long> totalPlaybackMillis = new MutableLiveData<>(0L);
    private final MutableLiveData<Boolean> isScreenTimeOver = new MutableLiveData<>(false);
    private List<Video> sessionVideos = new ArrayList<>();

    public VideoViewModel(@NonNull Application application) {
        super(application);
        currentFolderPath.setValue(SettingsManager.getFolderPath(application));
        loadInitialVideos();
    }

    public LiveData<List<Video>> getVideos() {
        return videos;
    }

    public LiveData<Boolean> getIsScanning() {
        return isScanning;
    }

    public LiveData<String> getCurrentFolderPath() {
        return currentFolderPath;
    }

    public LiveData<Long> getTotalPlaybackMillis() {
        return totalPlaybackMillis;
    }

    public LiveData<Integer> getTotalPlaybackSeconds() {
        return androidx.lifecycle.Transformations.map(totalPlaybackMillis, millis -> (int)(millis / 1000));
    }

    public void addPlaybackMillis(long delta) {
        Long current = totalPlaybackMillis.getValue();
        if (current == null) current = 0L;
        long next = current + delta;
        
        if (next / 1000 > current / 1000) {
            Log.d("PPG_AST_ACC", "AST Accrued: " + (next/1000) + "s (added " + delta + "ms)");
        }

        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            totalPlaybackMillis.setValue(next);
        } else {
            totalPlaybackMillis.postValue(next);
        }

        // Proactive check for limit inside ViewModel
        int limitMins = SettingsManager.getASTLimit(getApplication());
        if (next >= (long)limitMins * 60 * 1000) {
            setScreenTimeOver(true);
        }
    }

    public void incrementPlaybackSeconds() {
        addPlaybackMillis(1000L);
    }

    public LiveData<Boolean> getIsScreenTimeOver() {
        return isScreenTimeOver;
    }

    public void setScreenTimeOver(boolean over) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            isScreenTimeOver.setValue(over);
        } else {
            isScreenTimeOver.postValue(over);
        }
    }

    public void updateFolderPath(String path) {
        SettingsManager.saveFolderPath(getApplication(), path);
        currentFolderPath.setValue(path);
        loadVideos(path);
    }

    private void loadInitialVideos() {
        List<Video> saved = SettingsManager.getSavedVideoList(getApplication());
        if (!saved.isEmpty()) {
            sessionVideos = new ArrayList<>(saved);
            Collections.shuffle(sessionVideos);
            videos.setValue(sessionVideos);
        } else {
            videos.setValue(new ArrayList<>());
        }
    }

    public void reshuffleAll() {
        if (!sessionVideos.isEmpty()) {
            Collections.shuffle(sessionVideos);
            videos.setValue(sessionVideos);
        }
    }

    public void clearSVL() {
        sessionVideos.clear();
        SettingsManager.saveVideoList(getApplication(), new ArrayList<>());
        videos.setValue(new ArrayList<>());
    }

    public void incrementPlayCount(android.net.Uri uri) {
        if (sessionVideos != null) {
            for (Video v : sessionVideos) {
                if (v.getUri().equals(uri)) {
                    v.incrementPlayCount();
                    // Persist updated list with play counts
                    SettingsManager.saveVideoList(getApplication(), sessionVideos);
                    videos.postValue(sessionVideos);
                    break;
                }
            }
        }
    }

    public void loadVideos(String folderPath) {
        if (folderPath == null) return;
        isScanning.setValue(true);
        new Thread(() -> {
            List<Video> scanned = VideoLibrary.getVideos(getApplication(), folderPath);
            SettingsManager.saveVideoList(getApplication(), scanned);
            sessionVideos = new ArrayList<>(scanned);
            Collections.shuffle(sessionVideos);
            videos.postValue(sessionVideos);
            isScanning.postValue(false);
        }).start();
    }
}