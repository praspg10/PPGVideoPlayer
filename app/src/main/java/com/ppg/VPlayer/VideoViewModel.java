package com.ppg.VPlayer;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VideoViewModel extends AndroidViewModel {
    private final MutableLiveData<List<Video>> videos = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isScanning = new MutableLiveData<>(false);
    private final MutableLiveData<String> currentFolderPath = new MutableLiveData<>();
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
