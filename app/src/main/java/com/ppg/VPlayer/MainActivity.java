package com.ppg.VPlayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.ppg.VPlayer.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private VideoViewModel videoViewModel;

    private final ActivityResultLauncher<Intent> folderPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri treeUri = result.getData().getData();
                    if (treeUri != null) {
                        getContentResolver().takePersistableUriPermission(treeUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        
                        SettingsManager.saveFolderPath(this, treeUri.toString());
                        videoViewModel.loadVideos(treeUri.toString());
                        Toast.makeText(this, "Folder selected", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            EdgeToEdge.enable(this);
        } catch (Exception ignored) {}

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        videoViewModel = new ViewModelProvider(this).get(VideoViewModel.class);
        videoViewModel.loadVideos(SettingsManager.getFolderPath(this));

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        androidx.navigation.fragment.NavHostFragment navHostFragment = (androidx.navigation.fragment.NavHostFragment) 
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        }

        videoViewModel.getIsScanning().observe(this, isScanning -> {
            if (isScanning) {
                Toast.makeText(this, "Scanning videos...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            showSettingsDialog();
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showSettingsDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Settings");
        
        String currentPath = SettingsManager.getFolderPath(this);
        builder.setMessage("Current Folder: " + (currentPath != null ? currentPath : "None selected") + 
                "\n\nWould you like to select a new video folder or rescan the current one?");
        
        builder.setPositiveButton("Select Folder", (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            folderPickerLauncher.launch(intent);
        });
        
        builder.setNeutralButton("Rescan", (dialog, which) -> {
            if (currentPath != null) {
                videoViewModel.loadVideos(currentPath);
                Toast.makeText(this, "Rescan started...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No folder selected", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAboutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("About PPG-YouTubeKids")
                .setMessage("A kid-friendly offline video player for offline use.\nVersion 1.0")
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        androidx.navigation.fragment.NavHostFragment navHostFragment = (androidx.navigation.fragment.NavHostFragment) 
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            return NavigationUI.navigateUp(navController, appBarConfiguration)
                    || super.onSupportNavigateUp();
        }
        return super.onSupportNavigateUp();
    }
}
