package com.ppg.VPlayer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.ppg.VPlayer.databinding.FragmentVideoListBinding;
import java.util.List;

public class VideoListFragment extends Fragment {

    private FragmentVideoListBinding binding;
    private VideoAdapter adapter;
    private VideoViewModel viewModel;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    viewModel.loadVideos(SettingsManager.getFolderPath(requireContext()));
                } else {
                    Toast.makeText(getContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVideoListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(VideoViewModel.class);
        
        setupRecyclerView();
        checkPermissionAndLoad();
    }

    private void setupRecyclerView() {
        int spanCount = getResources().getInteger(R.integer.grid_span_count);
        binding.recyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), spanCount));
        
        viewModel.getIsScanning().observe(getViewLifecycleOwner(), isScanning -> {
            binding.progressBar.setVisibility(isScanning ? View.VISIBLE : View.GONE);
        });

        viewModel.getVideos().observe(getViewLifecycleOwner(), videos -> {
            if (videos == null || videos.isEmpty()) {
                if (Boolean.FALSE.equals(viewModel.getIsScanning().getValue())) {
                    binding.emptyView.setVisibility(View.VISIBLE);
                    binding.recyclerView.setVisibility(View.GONE);
                }
            } else {
                binding.emptyView.setVisibility(View.GONE);
                binding.recyclerView.setVisibility(View.VISIBLE);
                adapter = new VideoAdapter(videos, video -> {
                    Bundle args = new Bundle();
                    args.putString("videoUri", video.getUri().toString());
                    Navigation.findNavController(requireView()).navigate(R.id.action_VideoListFragment_to_VideoPlayerFragment, args);
                });
                binding.recyclerView.setAdapter(adapter);
            }
        });
    }

    private void checkPermissionAndLoad() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU 
                ? Manifest.permission.READ_MEDIA_VIDEO 
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(permission);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
