package com.ppg.VPlayer;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VideoPlayerFragment extends Fragment {

    private FragmentVideoPlayerBinding binding;
    private ExoPlayer player;
    private VideoViewModel viewModel;
    private final Handler hideHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideControlsRunnable = () -> { if (binding != null) hideControls(); };
    
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
        
        viewModel = new ViewModelProvider(requireActivity()).get(VideoViewModel.class);
        
        // Use View-level keeping awake instead of Window-level to avoid Fire OS crashes
        binding.playerView.setKeepScreenOn(true);

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
                if (binding == null) return;
                binding.btnPlayPause.setImageResource(isPlaying ? R.drawable.ic_pause_white_outlined : R.drawable.ic_play_white_outlined);
                if (isPlaying) {
                    startHideTimer();
                    startProgressUpdate();
                } else {
                    cancelHideTimer();
                    stopProgressUpdate();
                    if (!isSwitchingVideo) showControls();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    playNext();
                } else if (playbackState == Player.STATE_READY) {
                    if (binding != null) {
                        binding.seekBar.setMax((int) player.getDuration());
                        binding.txtTotalTime.setText(formatTime(player.getDuration()));
                    }
                }
            }
        });
    }

    private final Runnable progressUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying() && binding != null) {
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
        if (ms <= 0) return "00:00";
        // Sanity check: if duration is longer than 24 hours, it's likely a metadata error
        if (ms > 86400000) {
            return "00:00";
        }
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
            showControls();
        }
    }

    private void toggleControls() {
        if (binding == null) return;
        if (binding.controlsContainer.getVisibility() == View.VISIBLE) {
            hideControls();
        } else {
            showControls();
        }
    }

    private void setupFilmStrip() {
        binding.filmStrip.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        viewModel.getVideos().observe(getViewLifecycleOwner(), videos -> refreshFilmStrip());
    }

    private void refreshFilmStrip() {
        if (binding == null) return;
        List<Video> allVideos = viewModel.getVideos().getValue();
        if (allVideos != null && !allVideos.isEmpty()) {
            List<Video> filmStripVideos = new ArrayList<>(allVideos);
            Uri currentUri = (player != null && player.getCurrentMediaItem() != null && player.getCurrentMediaItem().localConfiguration != null) 
                    ? player.getCurrentMediaItem().localConfiguration.uri : null;
            if (currentUri != null) filmStripVideos.removeIf(v -> v.getUri().equals(currentUri));
            Collections.shuffle(filmStripVideos);
            if (filmStripVideos.size() > 15) filmStripVideos = filmStripVideos.subList(0, 15);

            VideoAdapter adapter = new VideoAdapter(filmStripVideos, (video, pos) -> playVideo(video.getUri()), true);
            binding.filmStrip.setAdapter(adapter);
        }
    }

    private void setupControls() {
        binding.btnPlayPause.setOnClickListener(v -> {
            if (player == null) return;
            if (player.isPlaying()) player.pause();
            else player.play();
            startHideTimer();
        });

        binding.btnBack.setOnClickListener(v -> performBackNavigation());
    }

    private void performBackNavigation() {
        Log.d("PPG_NAV", "performBackNavigation executed");
        stopProgressUpdate();
        cancelHideTimer();
        if (player != null) player.stop();
        
        try {
            Navigation.findNavController(requireView()).popBackStack();
        } catch (Exception e) {
            if (getActivity() != null) getActivity().onBackPressed();
        }
    }

    private void playVideo(Uri uri) {
        if (player == null) return;
        isSwitchingVideo = true;
        MediaItem mediaItem = MediaItem.fromUri(uri);
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();
        isSwitchingVideo = false;

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
        if (name == null) name = uri.getLastPathSegment();
        if (name != null && binding != null) {
            name = name.replaceAll("(?i)\\.(mp4|mkv|avi|mov|wmv|flv|webm|m4v|3gp|mp3)$", "");
            binding.txtVideoTitle.setText(name);
        }
        refreshFilmStrip();
        hideControls();
    }

    private void showControls() {
        if (binding == null) return;
        binding.controlsContainer.setVisibility(View.VISIBLE);
        refreshFilmStrip();
        binding.playerView.post(() -> {
            if (binding != null) {
                binding.playerView.setPivotX(binding.playerView.getWidth() / 2f);
                binding.playerView.setPivotY(0f);
                binding.playerView.animate().scaleX(0.75f).scaleY(0.75f).setDuration(300).start();
            }
        });
        startHideTimer();
        startProgressUpdate();
    }

    private void hideControls() {
        if (binding == null) return;
        binding.controlsContainer.setVisibility(View.GONE);
        binding.playerView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
        stopProgressUpdate();
    }

    private void startHideTimer() {
        cancelHideTimer();
        hideHandler.postDelayed(hideControlsRunnable, 5000);
    }

    private void cancelHideTimer() {
        hideHandler.removeCallbacks(hideControlsRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        stopProgressUpdate();
        cancelHideTimer();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopProgressUpdate();
        cancelHideTimer();
        if (player != null) {
            player.release();
            player = null;
        }
        binding = null;
    }
}
