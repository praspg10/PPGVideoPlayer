package com.ppg.VPlayer;

import android.net.Uri;

public class Video {
    private final String uriString;
    private final String name;
    private final int duration;
    private final long size;

    public Video(Uri uri, String name, int duration, long size) {
        this.uriString = uri.toString();
        this.name = name;
        this.duration = duration;
        this.size = size;
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
}
