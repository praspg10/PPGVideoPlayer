package com.ppg.VPlayer;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.ppg.VPlayer.databinding.FragmentVideoPlayerBinding;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VideoPlayerFragment extends Fragment {

    private FragmentVideoPlayerBinding binding;
    private ExoPlayer player;
    private VideoViewModel viewModel;
    private final Handler hideHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideControlsRunnable = () -> hideControls();
    
    private List<String> playlistUris = new ArrayList<>();
    private int currentPosition = 0;
    private boolean isSwitchingVideo = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVideoPlayerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        hideSystemUI();
        viewModel = new ViewModelProvider(requireActivity()).get(VideoViewModel.class);
        
        setupPlayer();
        setupFilmStrip();
        setupControls();

        if (getArguments() != null) {
            playlistUris = getArguments().getStringArrayList("playlist");
            currentPosition = getArguments().getInt("videoPosition", 0);
            
            if (playlistUris != null && !playlistUris.isEmpty()) {
                playVideo(Uri.parse(playlistUris.get(currentPosition)));
            }
        }
    }

    private void hideSystemUI() {
        View decorView = requireActivity().getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void showSystemUI() {
        View decorView = requireActivity().getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private void setupPlayer() {
        player = new ExoPlayer.Builder(requireContext()).build();
        binding.playerView.setPlayer(player);
        
        binding.playerView.setOnClickListener(v -> toggleControls());
        binding.controlsContainer.setOnClickListener(v -> hideControls());
        
        binding.seekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    player.seekTo(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) { cancelHideTimer(); }
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) { startHideTimer(); }
        });

        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                binding.btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause_white_outlined : R.drawable.ic_play_white_outlined);
                binding.btnPlayPause.setColorFilter(null);
                if (isPlaying) {
                    startHideTimer();
                    startProgressUpdate();
                } else {
                    cancelHideTimer();
                    stopProgressUpdate();
                    if (!isSwitchingVideo) {
                        showControls();
                    }
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    playNext();
                } else if (playbackState == Player.STATE_READY) {
                    binding.seekBar.setMax((int) player.getDuration());
                    binding.txtTotalTime.setText(formatTime(player.getDuration()));
                }
            }
        });
    }

    private final Runnable progressUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                long current = player.getCurrentPosition();
                binding.seekBar.setProgress((int) current);
                binding.txtCurrentTime.setText(formatTime(current));
                hideHandler.postDelayed(this, 1000);
            }
        }
    };

    private void startProgressUpdate() {
        hideHandler.removeCallbacks(progressUpdateRunnable);
        hideHandler.post(progressUpdateRunnable);
    }

    private void stopProgressUpdate() {
        hideHandler.removeCallbacks(progressUpdateRunnable);
    }

    private String formatTime(long ms) {
        int totalSeconds = (int) (ms / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void playNext() {
        if (playlistUris != null && currentPosition + 1 < playlistUris.size()) {
            currentPosition++;
            playVideo(Uri.parse(playlistUris.get(currentPosition)));
        } else {
            // End of playlist, maybe go back or restart? For now, just show controls
            showControls();
        }
    }

    private void toggleControls() {
        if (binding.controlsContainer.getVisibility() == View.VISIBLE) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void setupFilmStrip() {
        binding.filmStrip.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        viewModel.getVideos().observe(getViewLifecycleOwner(), videos -> {
            refreshFilmStrip();
        });
    }

    private void refreshFilmStrip() {
        List<Video> allVideos = viewModel.getVideos().getValue();
        if (allVideos != null && !allVideos.isEmpty()) {
            List<Video> filmStripVideos = new ArrayList<>(allVideos);
            Collections.shuffle(filmStripVideos);
            if (filmStripVideos.size() > 15) {
                filmStripVideos = filmStripVideos.subList(0, 15);
            }

            VideoAdapter adapter = new VideoAdapter(filmStripVideos, (video, pos) -> {
                // When clicking film strip, we play that video and immediately hide controls to go to S-2
                playVideo(video.getUri());
            }, true);
            binding.filmStrip.setAdapter(adapter);
        }
    }

    private void setupControls() {
        binding.btnPlayPause.setColorFilter(null);
        
        binding.btnPlayPause.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
            } else {
                player.play();
            }
            startHideTimer();
        });

        binding.btnBack.setOnClickListener(v -> {
            androidx.navigation.Navigation.findNavController(v).navigateUp();
        });
    }

    private void playVideo(Uri uri) {
        isSwitchingVideo = true;
        MediaItem mediaItem = MediaItem.fromUri(uri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
        isSwitchingVideo = false;

        // Update title in S-3 header - find full name from viewModel
        String name = null;
        List<Video> videos = viewModel.getVideos().getValue();
        if (videos != null) {
            for (Video v : videos) {
                if (v.getUri().equals(uri)) {
                    name = v.getName();
                    break;
                }
            }
        }

        if (name == null) {
            name = uri.getLastPathSegment();
        }

        if (name != null) {
            // Remove extensions to show clean file name
            name = name.replaceAll("(?i)\\.(mp4|mkv|avi|mov|wmv|flv|webm|m4v|3gp|mp3)$", "");
            binding.txtVideoTitle.setText(name);
        }

        refreshFilmStrip();
        hideControls();
    }

    private void showControls() {
        binding.controlsContainer.setVisibility(View.VISIBLE);
        refreshFilmStrip();
        // Reduce video size to 75% and move to top-centered
        binding.playerView.post(() -> {
            binding.playerView.setPivotX(binding.playerView.getWidth() / 2f);
            binding.playerView.setPivotY(0f);
            binding.playerView.animate().scaleX(0.75f).scaleY(0.75f).setDuration(300).start();
        });
        startHideTimer();
        startProgressUpdate();
    }

    private void hideControls() {
        binding.controlsContainer.setVisibility(View.GONE);
        // Restore to 100% full screen
        binding.playerView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
        stopProgressUpdate();
    }

    private void startHideTimer() {
        cancelHideTimer();
        hideHandler.postDelayed(hideControlsRunnable, 5000); // 5 seconds for kids
    }

    private void cancelHideTimer() {
        hideHandler.removeCallbacks(hideControlsRunnable);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        requireActivity().setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        showSystemUI();
        if (player != null) {
            player.release();
            player = null;
        }
        binding = null;
    }
}
