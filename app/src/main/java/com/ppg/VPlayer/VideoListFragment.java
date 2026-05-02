package com.ppg.VPlayer;

import android.Manifest;
import android.content.pm.PackageManager;
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
import com.google.android.material.tabs.TabLayout;
import com.ppg.VPlayer.databinding.FragmentVideoListBinding;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VideoListFragment extends Fragment {

    private FragmentVideoListBinding binding;
    private VideoViewModel viewModel;
    private List<Video> allVideos = new ArrayList<>();
    private List<String> categories = new ArrayList<>();
    private String currentCategory = "All";

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
        
        binding.btnMenu.setOnClickListener(v -> {
            androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(requireContext(), v);
            popup.getMenuInflater().inflate(R.menu.menu_main, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                return requireActivity().onOptionsItemSelected(item);
            });
            popup.show();
        });

        binding.btnCloseApp.setOnClickListener(v -> {
            // Clear all cache and close the app
            SettingsManager.clearCache(requireContext());
            requireActivity().finishAndRemoveTask();
            System.exit(0);
        });

        setupTabs();
        setupRecyclerView();
        
        // RE-FIX: Populate videos and THEN update categories to ensure tabs show up immediately
        List<Video> currentVideos = viewModel.getVideos().getValue();
        if (currentVideos != null) {
            allVideos = new ArrayList<>(currentVideos);
            updateCategories();
            filterVideos(currentCategory);
            
            // Ensure the correct tab is visually selected
            for (int i = 0; i < binding.tabLayout.getTabCount(); i++) {
                TabLayout.Tab tab = binding.tabLayout.getTabAt(i);
                if (tab != null && tab.getText() != null && currentCategory.equals(tab.getText().toString())) {
                    tab.select();
                    break;
                }
            }
        }

        checkPermissionAndLoad();
    }

    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                String newCategory = tab.getText().toString();
                if (newCategory.equals("All") && !currentCategory.equals("All")) {
                    viewModel.reshuffleAll();
                }
                currentCategory = newCategory;
                filterVideos(newCategory);
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupRecyclerView() {
        int spanCount = getResources().getInteger(R.integer.grid_span_count);
        binding.recyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), spanCount));
        
        // Use a flag to avoid infinite loops during initial tab setup
        final boolean[] isInitializing = {true};

        viewModel.getVideos().observe(getViewLifecycleOwner(), videos -> {
            allVideos = (videos != null) ? videos : new ArrayList<>();
            updateCategories();
            
            boolean currentlyScanning = Boolean.TRUE.equals(viewModel.getIsScanning().getValue());
            
            if (allVideos.isEmpty()) {
                binding.recyclerView.setVisibility(View.GONE);
                if (!currentlyScanning) {
                    binding.emptyView.setVisibility(View.VISIBLE);
                } else {
                    binding.emptyView.setVisibility(View.GONE);
                }
            } else {
                binding.emptyView.setVisibility(View.GONE);
                binding.recyclerView.setVisibility(View.VISIBLE);
                
                if (isInitializing[0]) {
                    if (binding.tabLayout.getTabCount() > 0) {
                        filterVideos(currentCategory);
                    }
                    isInitializing[0] = false;
                }
            }
        });

        viewModel.getIsScanning().observe(getViewLifecycleOwner(), isScanning -> {
            binding.progressBar.setVisibility(isScanning ? View.VISIBLE : View.GONE);
            if (!isScanning && allVideos.isEmpty()) {
                binding.emptyView.setVisibility(View.VISIBLE);
            } else if (isScanning) {
                binding.emptyView.setVisibility(View.GONE);
            }
        });
    }

    private void updateCategories() {
        Set<String> folderSet = new LinkedHashSet<>();
        folderSet.add("All");
        
        if (allVideos != null && !allVideos.isEmpty()) {
            for (Video v : allVideos) {
                folderSet.add(v.getFolderName());
            }
        }
        
        List<String> newCategories = new ArrayList<>(folderSet);
        // Requirement: Limit to All + 5 folders (Total 6)
        if (newCategories.size() > 6) {
            newCategories = newCategories.subList(0, 6);
        }

        // RE-FIX: Force rebuild if the tab layout is empty (e.g. after view recreation)
        if (!newCategories.equals(categories) || binding.tabLayout.getTabCount() == 0) {
            categories = newCategories;
            binding.tabLayout.removeAllTabs();
            // If empty, only "All" will be in the set
            for (String category : categories) {
                binding.tabLayout.addTab(binding.tabLayout.newTab().setText(category));
            }
        }
    }

    private void filterVideos(String category) {
        List<Video> filtered;
        if (category.equals("All")) {
            filtered = allVideos;
        } else {
            filtered = allVideos.stream()
                    .filter(v -> v.getFolderName().equals(category))
                    .collect(Collectors.toList());
        }

        VideoAdapter adapter = new VideoAdapter(filtered, (video, position) -> {
            Bundle args = new Bundle();
            args.putString("videoUri", video.getUri().toString());
            args.putInt("videoPosition", position);
            // We'll pass the whole filtered list IDs to play in sequence
            ArrayList<String> playlist = filtered.stream()
                    .map(v -> v.getUri().toString())
                    .collect(Collectors.toCollection(ArrayList::new));
            args.putStringArrayList("playlist", playlist);
            
            Navigation.findNavController(requireView()).navigate(R.id.action_VideoListFragment_to_VideoPlayerFragment, args);
        });
        binding.recyclerView.setAdapter(adapter);
        
        binding.emptyView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        binding.recyclerView.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
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
