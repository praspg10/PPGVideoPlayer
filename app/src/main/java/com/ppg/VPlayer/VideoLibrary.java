package com.ppg.VPlayer;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import androidx.documentfile.provider.DocumentFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VideoLibrary {

    public static List<Video> getVideos(Context context, String folderUriStr) {
        List<Video> videoList = new ArrayList<>();
        if (folderUriStr == null || folderUriStr.isEmpty()) {
            // Requirement: Do not auto-scan storage. Return empty if no folder is configured.
            return videoList;
        }

        boolean skipScanEnabled = SettingsManager.isSkipScanEnabled(context);

        try {
            Uri treeUri = Uri.parse(folderUriStr);
            DocumentFile root = DocumentFile.fromTreeUri(context, treeUri);
            if (root != null && root.isDirectory()) {
                scanRecursive(context, root, videoList, skipScanEnabled);
            }
        } catch (Exception e) {
            // Requirement: Do not fall back to scanning all videos.
            return videoList;
        }

        Collections.shuffle(videoList);
        return videoList;
    }

    private static void scanRecursive(Context context, DocumentFile parent, List<Video> videoList, boolean skipScanEnabled) {
        String folderName = parent.getName();
        if (folderName == null) folderName = "Unknown";
        
        // Requirement: Skip folders (and their subfolders) containing "-skipscan" ONLY IF ENABLED
        if (skipScanEnabled && folderName.toLowerCase().contains("-skipscan")) {
            android.util.Log.d("PPG_SCAN", "Skipping folder: " + folderName);
            return;
        }

        DocumentFile[] files = parent.listFiles();
        for (DocumentFile file : files) {
            if (file.isDirectory()) {
                scanRecursive(context, file, videoList, skipScanEnabled);
            } else {
                String name = file.getName();
                if (name != null) {
                    String lowerName = name.toLowerCase();
                    if (lowerName.endsWith(".mp4") || lowerName.endsWith(".mp3") || lowerName.endsWith(".mkv")) {
                        int duration = 0;
                        android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                        try {
                            retriever.setDataSource(context, file.getUri());
                            String time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                            if (time != null) {
                                duration = Integer.parseInt(time);
                                // Validation: Some files report extremely high invalid durations
                                if (duration > 86400000 || duration < 0) {
                                    duration = 0;
                                }
                            }
                        } catch (Exception ignored) {
                        } finally {
                            try {
                                retriever.release();
                            } catch (Exception ignored) {}
                        }
                        
                        videoList.add(new Video(file.getUri(), name, duration, file.length(), folderName));
                    }
                }
            }
        }
    }

    private static List<Video> scanAllVideos(Context context) {
        List<Video> videoList = new ArrayList<>();
        Uri collection = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                : MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

        String[] projection = new String[]{
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        };

        try (Cursor cursor = context.getContentResolver().query(collection, projection, null, null, null)) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
                int bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String name = cursor.getString(nameCol);
                    int duration = cursor.getInt(durationCol);
                    long size = cursor.getLong(sizeCol);
                    String folderName = cursor.getString(bucketCol);
                    if (folderName == null) folderName = "Internal";
                    
                    Uri uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                    videoList.add(new Video(uri, name, duration, size, folderName));
                }
            }
        }
        Collections.shuffle(videoList);
        return videoList;
    }

    public static boolean isVideoAvailable(Context context, Uri uri) {
        if (uri == null) return false;
        try {
            if ("content".equals(uri.getScheme())) {
                try (android.os.ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r")) {
                    return pfd != null;
                }
            } else {
                String path = uri.getPath();
                if (path == null) return false;
                java.io.File file = new java.io.File(path);
                return file.exists();
            }
        } catch (Exception e) {
            return false;
        }
    }
}
