package com.ppg.VPlayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import java.util.Locale;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private final List<Video> videos;
    private final OnVideoClickListener listener;
    private final boolean isSmall;

    public interface OnVideoClickListener {
        void onVideoClick(Video video);
    }

    public VideoAdapter(List<Video> videos, OnVideoClickListener listener) {
        this(videos, listener, false);
    }

    public VideoAdapter(List<Video> videos, OnVideoClickListener listener, boolean isSmall) {
        this.videos = videos;
        this.listener = listener;
        this.isSmall = isSmall;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = isSmall ? R.layout.item_video_small : R.layout.item_video;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        Video video = videos.get(position);
        holder.title.setText(video.getName());
        if (holder.duration != null) {
            holder.duration.setText(formatDuration(video.getDuration()));
        }

        int[] colors = {R.color.ytk_cyan, R.color.ytk_orange, R.color.ytk_green, R.color.ytk_purple};
        int color = holder.itemView.getContext().getResources().getColor(colors[position % colors.length], null);
        
        if (holder.itemView instanceof com.google.android.material.card.MaterialCardView) {
            ((com.google.android.material.card.MaterialCardView) holder.itemView).setStrokeColor(color);
            ((com.google.android.material.card.MaterialCardView) holder.itemView).setStrokeWidth(8);
        }

        int placeholder = video.getName().toLowerCase().endsWith(".mp3") 
                ? android.R.drawable.ic_lock_silent_mode_off 
                : android.R.drawable.ic_menu_gallery;

        Glide.with(holder.thumbnail.getContext())
                .load(video.getUri())
                .placeholder(placeholder)
                .into(holder.thumbnail);

        holder.itemView.setOnClickListener(v -> listener.onVideoClick(video));
    }

    @Override
    public int getItemCount() {
        return videos.size();
    }

    private String formatDuration(int durationMs) {
        long seconds = (durationMs / 1000) % 60;
        long minutes = (durationMs / (1000 * 60)) % 60;
        long hours = (durationMs / (1000 * 60 * 60)) % 24;

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    static class VideoViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail;
        TextView title;
        TextView duration;

        VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            title = itemView.findViewById(R.id.title);
            duration = itemView.findViewById(R.id.duration);
        }
    }
}
