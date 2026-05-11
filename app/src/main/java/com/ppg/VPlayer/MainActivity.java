package com.ppg.VPlayer;

import android.content.Intent;
import android.content.IntentFilter;
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
    private android.content.BroadcastReceiver screenOffReceiver;
    private boolean isLimitDialogShowing = false;

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

        videoViewModel.getTotalPlaybackSeconds().observe(this, totalSecs -> {
            if (isLimitDialogShowing) return;
            
            int limit = SettingsManager.getASTLimit(this);
            if (totalSecs / 60 >= limit) {
                // Requirement 4: Limit reached
                isLimitDialogShowing = true;
                Log.d("PPG_AST", "AST limit reached: " + limit + " mins");
                videoViewModel.setScreenTimeOver(true);
                SettingsManager.saveLastLimitTimestamp(this, System.currentTimeMillis());
                
                // Navigate back to Screen-1 if we are in Screen-3
                // The fragment observer handles this but we'll double check

                showASTOverOverlay();
            }
        });

        setupScreenOffReceiver();
    }

    private void setupScreenOffReceiver() {
        screenOffReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                    Log.d("PPG_NAV", "Screen off detected, clearing cache and finishing activity");
                    SettingsManager.clearCache(context);
                    finishAndRemoveTask();
                    System.exit(0);
                }
            }
        };
        IntentFilter filter = new android.content.IntentFilter(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenOffReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (screenOffReceiver != null) {
            unregisterReceiver(screenOffReceiver);
        }
    }

    private void hideSystemUI() {
        // Ensure navigation bar background is dark so icons show as white
        getWindow().setNavigationBarColor(android.graphics.Color.BLACK);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                
                // Clear the light appearance flag to force icons to be white
                controller.setSystemBarsAppearance(0, 
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | 
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
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
        if (checkCoolOffPeriod()) {
            // Cool off period active, app will remain blocked by overlay/dialog and then exit
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        android.os.PowerManager pm = (android.os.PowerManager) getSystemService(android.content.Context.POWER_SERVICE);
        if (pm != null && !pm.isInteractive()) {
            Log.d("PPG_NAV", "Screen is off in onPause, clearing cache and finishing");
            SettingsManager.clearCache(this);
            finishAndRemoveTask();
            System.exit(0);
        }
    }

    private void checkASTLimit() {
        // Now handled by observer in onCreate
    }

    private boolean checkCoolOffPeriod() {
        long lastReached = SettingsManager.getLastLimitTimestamp(this);
        if (lastReached == 0) return false;

        int coolOffPeriodMins = SettingsManager.getCoolOffPeriod(this);
        long diffMs = System.currentTimeMillis() - lastReached;
        long coolOffPeriodMs = (long) coolOffPeriodMins * 60 * 1000;

        if (diffMs < coolOffPeriodMs) {
            // Requirement 4e: Still under cool off period
            int remainingMins = (int) ((coolOffPeriodMs - diffMs) / 60000) + 1;
            showStillInCoolOffPeriodDialog(remainingMins);
            return true;
        } else {
            // Requirement 4f: After cool off period ends
            SettingsManager.saveLastLimitTimestamp(this, 0); // set LSTOMsg = null
            return false;
        }
    }

    private void showASTOverOverlay() {
        Log.d("PPG_AST", "showASTOverOverlay called");
        try {
            // Requirement 4c: Display popup overlay
            android.view.View overlay = getLayoutInflater().inflate(R.layout.dialog_overlay, null);
            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                    .setView(overlay)
                    .setCancelable(false)
                    .create();

            int limit = SettingsManager.getASTLimit(this);
            TextView msg1 = overlay.findViewById(R.id.txtMessage1);
            if (msg1 != null) {
                msg1.setText(limit + " mins Screen time is over");
            }

            overlay.findViewById(R.id.btnOverlayOk).setOnClickListener(v -> {
                // Requirement 4d: Exit app
                dialog.dismiss();
                finishAndRemoveTask();
                System.exit(0);
            });

            if (!isFinishing()) {
                dialog.show();
            }
        } catch (Exception e) {
            Log.e("PPG_AST", "Error showing AST overlay", e);
            // Fallback exit
            finishAndRemoveTask();
            System.exit(0);
        }
    }

    private void showStillInCoolOffPeriodDialog(int remainingMins) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cool Off Period")
                .setMessage("Still under Cool Off Period.... \nPlease wait " + remainingMins + " more minutes.")
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> {
                    finishAndRemoveTask();
                    System.exit(0);
                })
                .show();
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
        
        android.view.View btnCloseX = dialogView.findViewById(R.id.btnCloseX);
        android.widget.Button btnSelect = dialogView.findViewById(R.id.btnSelectFolder);
        android.widget.Button btnRescan = dialogView.findViewById(R.id.btnRescan);
        android.widget.Button btnSave = dialogView.findViewById(R.id.btnSave);
        
        androidx.appcompat.widget.SwitchCompat switchRecent = dialogView.findViewById(R.id.switchShowRecent);
        android.widget.EditText editAST = dialogView.findViewById(R.id.editAST);
        android.widget.EditText editCoolOffPeriod = dialogView.findViewById(R.id.editCoolOffPeriod);
        android.widget.EditText editRandom = dialogView.findViewById(R.id.editRandomThreshold);

        // Load current values
        switchRecent.setChecked(SettingsManager.isShowRecentEnabled(this));
        editAST.setText(String.valueOf(SettingsManager.getASTLimit(this)));
        editCoolOffPeriod.setText(String.valueOf(SettingsManager.getCoolOffPeriod(this)));
        editRandom.setText(String.valueOf(SettingsManager.getRandomThreshold(this)));

        // Observe changes while dialog is open
        videoViewModel.getCurrentFolderPath().observe(this, folderPath -> {
            String displayPath = "None";
            if (folderPath != null) {
                try {
                    String decoded = Uri.decode(folderPath);
                    String pathPart = decoded;
                    if (decoded.contains(":")) {
                        pathPart = decoded.substring(decoded.lastIndexOf(":") + 1);
                    }
                    String[] segments = pathPart.split("/");
                    if (segments.length >= 2) {
                        displayPath = segments[segments.length - 2] + "/" + segments[segments.length - 1];
                    } else {
                        displayPath = pathPart;
                    }
                } catch (Exception e) {
                    displayPath = folderPath;
                }
            }
            txtFolder.setText(Html.fromHtml("<b>Current Folder: </b>" + displayPath, Html.FROM_HTML_MODE_LEGACY));
        });

        videoViewModel.getIsScanning().observe(this, isScanning -> {
            progressBar.setVisibility(isScanning ? android.view.View.VISIBLE : android.view.View.GONE);
            btnSelect.setEnabled(!isScanning);
            btnRescan.setEnabled(!isScanning);
            btnSave.setEnabled(!isScanning);
        });

        videoViewModel.getVideos().observe(this, videos -> {
            int count = (videos != null ? videos.size() : 0);
            txtCount.setText(Html.fromHtml("<b>Scanned Files Count: </b>" + count, Html.FROM_HTML_MODE_LEGACY));
        });

        btnCloseX.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            try {
                SettingsManager.saveShowRecent(this, switchRecent.isChecked());
                SettingsManager.saveASTLimit(this, Integer.parseInt(editAST.getText().toString()));
                SettingsManager.saveCoolOffPeriod(this, Integer.parseInt(editCoolOffPeriod.getText().toString()));
                SettingsManager.saveRandomThreshold(this, Integer.parseInt(editRandom.getText().toString()));
                Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });

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
                .setMessage("A kid-friendly offline video player.\nVersion 2.0\n\n" +
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
