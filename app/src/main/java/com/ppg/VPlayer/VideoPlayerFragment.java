package com.ppg.VPlayer;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.ppg.VPlayer.databinding.FragmentVideoPlayerBinding;

public class VideoPlayerFragment extends Fragment {

    private FragmentVideoPlayerBinding binding;
    private ExoPlayer player;
    private VideoViewModel viewModel;
    private final Handler hideHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideControlsRunnable = () -> hideControls();

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

        String videoUriStr = getArguments() != null ? getArguments().getString("videoUri") : null;
        if (videoUriStr != null) {
            playVideo(Uri.parse(videoUriStr));
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
        
        // Use a touch listener or just the playerView's click
        binding.playerView.setOnClickListener(v -> toggleControls());
        binding.controlsContainer.setOnClickListener(v -> hideControls());
        
        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                binding.btnPlayPause.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                binding.btnPlayPause.setColorFilter(getResources().getColor(R.color.ytk_cyan, null));
                if (isPlaying) {
                    startHideTimer();
                } else {
                    cancelHideTimer();
                    showControls();
                }
            }
        });
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
            if (videos != null && !videos.isEmpty()) {
                // Shuffle and limit to 15 random videos for the film strip
                java.util.List<Video> filmStripVideos = new java.util.ArrayList<>(videos);
                java.util.Collections.shuffle(filmStripVideos);
                if (filmStripVideos.size() > 15) {
                    filmStripVideos = filmStripVideos.subList(0, 15);
                }

                VideoAdapter adapter = new VideoAdapter(filmStripVideos, video -> {
                    playVideo(video.getUri());
                    showControls();
                }, true);
                binding.filmStrip.setAdapter(adapter);
            }
        });
    }

    private void setupControls() {
        binding.btnPlayPause.setColorFilter(getResources().getColor(R.color.ytk_cyan, null));
        binding.btnBack.setColorFilter(getResources().getColor(R.color.ytk_cyan, null));

        binding.btnPlayPause.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
            } else {
                player.play();
            }
            startHideTimer();
        });

        binding.btnBack.setOnClickListener(v -> Navigation.findNavController(v).navigateUp());
    }

    private void playVideo(Uri uri) {
        MediaItem mediaItem = MediaItem.fromUri(uri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
    }

    private void showControls() {
        binding.controlsContainer.setVisibility(View.VISIBLE);
        startHideTimer();
    }

    private void hideControls() {
        binding.controlsContainer.setVisibility(View.GONE);
    }

    private void startHideTimer() {
        cancelHideTimer();
        hideHandler.postDelayed(hideControlsRunnable, 3000);
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
