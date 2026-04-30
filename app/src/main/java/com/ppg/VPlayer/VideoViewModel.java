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

    public VideoViewModel(@NonNull Application application) {
        super(application);
        loadInitialVideos();
    }

    public LiveData<List<Video>> getVideos() {
        return videos;
    }

    public LiveData<Boolean> getIsScanning() {
        return isScanning;
    }

    private void loadInitialVideos() {
        List<Video> saved = SettingsManager.getSavedVideoList(getApplication());
        if (saved.isEmpty()) {
            loadVideos(SettingsManager.getFolderPath(getApplication()));
        } else {
            Collections.shuffle(saved);
            List<Video> initialVerified = new ArrayList<>();
            List<Video> remaining = new ArrayList<>();
            
            // Step 1: Quickly verify and show the first 10
            for (Video v : saved) {
                if (initialVerified.size() < 10) {
                    if (VideoLibrary.isVideoAvailable(getApplication(), v.getUri())) {
                        initialVerified.add(v);
                    }
                } else {
                    remaining.add(v);
                }
            }
            videos.setValue(initialVerified);
            
            // Step 2: Thoroughly verify the rest in the background
            if (!remaining.isEmpty()) {
                new Thread(() -> {
                    List<Video> fullList = new ArrayList<>(initialVerified);
                    for (Video v : remaining) {
                        if (VideoLibrary.isVideoAvailable(getApplication(), v.getUri())) {
                            fullList.add(v);
                        }
                    }
                    // Update UI with the full verified list
                    videos.postValue(fullList);
                }).start();
            }
        }
    }

    public void loadVideos(String folderPath) {
        isScanning.setValue(true);
        new Thread(() -> {
            List<Video> scanned = VideoLibrary.getVideos(getApplication(), folderPath);
            SettingsManager.saveVideoList(getApplication(), scanned);
            videos.postValue(scanned);
            isScanning.postValue(false);
        }).start();
    }
}
