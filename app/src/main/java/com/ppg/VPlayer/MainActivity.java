package com.ppg.VPlayer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Html;
import android.util.Log;

import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
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
                        
                        videoViewModel.updateFolderPath(treeUri.toString());
                        Toast.makeText(this, "Folder selected. Scanning...", Toast.LENGTH_SHORT).show();
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

        hideSystemUI();

        videoViewModel = new ViewModelProvider(this).get(VideoViewModel.class);
        // Removed auto-loadVideos from here as per new requirement

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

    private void hideSystemUI() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // Legacy fallback
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
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
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        TextView txtFolder = dialogView.findViewById(R.id.txtCurrentFolder);
        TextView txtCount = dialogView.findViewById(R.id.txtVideoCount);
        ProgressBar progressBar = dialogView.findViewById(R.id.dialogProgressBar);
        android.view.View btnClose = dialogView.findViewById(R.id.btnClose);
        android.widget.Button btnSelect = dialogView.findViewById(R.id.btnSelectFolder);
        android.widget.Button btnRescan = dialogView.findViewById(R.id.btnRescan);

        // Observe changes while dialog is open
        videoViewModel.getCurrentFolderPath().observe(this, folderPath -> {
            String path = (folderPath != null ? folderPath : "None");
            txtFolder.setText(Html.fromHtml("<b>Current Folder: </b>" + path, Html.FROM_HTML_MODE_LEGACY));
        });

        videoViewModel.getIsScanning().observe(this, isScanning -> {
            progressBar.setVisibility(isScanning ? android.view.View.VISIBLE : android.view.View.GONE);
            btnSelect.setEnabled(!isScanning);
            btnRescan.setEnabled(!isScanning);
        });

        videoViewModel.getVideos().observe(this, videos -> {
            int count = (videos != null ? videos.size() : 0);
            txtCount.setText(Html.fromHtml("<b>Scanned Files Count: </b>" + count, Html.FROM_HTML_MODE_LEGACY));
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        btnSelect.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            folderPickerLauncher.launch(intent);
        });

        btnRescan.setOnClickListener(v -> {
            String currentPath = SettingsManager.getFolderPath(this);
            if (currentPath != null) {
                videoViewModel.loadVideos(currentPath);
            } else {
                Toast.makeText(this, "Select a folder first", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void showAboutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("About PPG-YouTubeKids")
                .setMessage("A kid-friendly offline video player.\nVersion 1.0\n\n" +
                        "Note: Each time the app is installed or new videos are added, " +
                        "you must manually 'Rescan' in Settings to update your list.")
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
