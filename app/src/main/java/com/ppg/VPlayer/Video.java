package com.ppg.VPlayer;

import android.net.Uri;

public class Video {
    private final String uriString;
    private final String name;
    private final int duration;
    private final long size;
    private final String folderName;
    private int playCount = 0;

    public Video(Uri uri, String name, int duration, long size, String folderName) {
        this.uriString = uri.toString();
        this.name = name;
        this.duration = duration;
        this.size = size;
        this.folderName = folderName;
    }

    public Uri getUri() {
        return Uri.parse(uriString);
    }

    public String getName() {
        return name;
    }

    public int getDuration() {
        return duration;
    }

    public long getSize() {
        return size;
    }

    public String getFolderName() {
        return folderName;
    }

    public int getPlayCount() {
        return playCount;
    }

    public void incrementPlayCount() {
        this.playCount++;
    }
}
