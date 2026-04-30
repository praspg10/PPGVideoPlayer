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
            return scanAllVideos(context);
        }

        try {
            Uri treeUri = Uri.parse(folderUriStr);
            DocumentFile root = DocumentFile.fromTreeUri(context, treeUri);
            if (root != null && root.isDirectory()) {
                scanRecursive(context, root, videoList);
            }
        } catch (Exception e) {
            return scanAllVideos(context);
        }

        Collections.shuffle(videoList);
        return videoList;
    }

    private static void scanRecursive(Context context, DocumentFile parent, List<Video> videoList) {
        DocumentFile[] files = parent.listFiles();
        for (DocumentFile file : files) {
            if (file.isDirectory()) {
                scanRecursive(context, file, videoList);
            } else {
                String name = file.getName();
                if (name != null) {
                    String lowerName = name.toLowerCase();
                    if (lowerName.endsWith(".mp4") || lowerName.endsWith(".mp3") || lowerName.endsWith(".mkv")) {
                        int duration = 0;
                        try (android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever()) {
                            retriever.setDataSource(context, file.getUri());
                            String time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                            if (time != null) duration = Integer.parseInt(time);
                        } catch (Exception ignored) {}
                        
                        videoList.add(new Video(file.getUri(), name, duration, file.length()));
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
                MediaStore.Video.Media.SIZE
        };

        try (Cursor cursor = context.getContentResolver().query(collection, projection, null, null, null)) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
                int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String name = cursor.getString(nameCol);
                    int duration = cursor.getInt(durationCol);
                    long size = cursor.getLong(sizeCol);
                    Uri uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);
                    videoList.add(new Video(uri, name, duration, size));
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
