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
    private int currentPlaybackSecs = 0;

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
        android.util.Log.d("PPG_NAV", "onCreate started");
        try {
            EdgeToEdge.enable(this);
        } catch (Exception ignored) {}

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        hideSystemUI();

        videoViewModel = new ViewModelProvider(this).get(VideoViewModel.class);

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

        videoViewModel.getIsScreenTimeOver().observe(this, over -> {
            if (over && !isLimitDialogShowing) {
                isLimitDialogShowing = true;
                Log.d("PPG_AST", "AST limit reached signal received");
                SettingsManager.saveLastLimitTimestamp(this, System.currentTimeMillis());
                showASTOverOverlay();
            }
        });

        videoViewModel.getTotalPlaybackSeconds().observe(this, totalSecs -> {
            currentPlaybackSecs = totalSecs;
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
        getWindow().setNavigationBarColor(android.graphics.Color.BLACK);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.setSystemBarsAppearance(0, 
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | 
                        WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        } else {
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
        android.util.Log.d("PPG_NAV", "onResume called");
        hideSystemUI();
        if (checkCoolOffPeriod()) {
            android.util.Log.d("PPG_NAV", "Access blocked: cool off period active");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        android.util.Log.d("PPG_NAV", "onPause called");
        android.os.PowerManager pm = (android.os.PowerManager) getSystemService(android.content.Context.POWER_SERVICE);
        if (pm != null && !pm.isInteractive()) {
            android.util.Log.d("PPG_NAV", "Non-interactive pause (screen off?), clearing cache and exiting");
            SettingsManager.clearCache(this);
            finishAndRemoveTask();
            System.exit(0);
        }
    }

    private boolean checkCoolOffPeriod() {
        long lastReached = SettingsManager.getLastLimitTimestamp(this);
        if (lastReached == 0) return false;

        int coolOffPeriodMins = SettingsManager.getCoolOffPeriod(this);
        long diffMs = System.currentTimeMillis() - lastReached;
        long coolOffPeriodMs = (long) coolOffPeriodMins * 60 * 1000;

        if (diffMs < coolOffPeriodMs) {
            int remainingMins = (int) ((coolOffPeriodMs - diffMs) / 60000) + 1;
            showStillInCoolOffPeriodDialog(remainingMins);
            return true;
        } else {
            SettingsManager.saveLastLimitTimestamp(this, 0);
            return false;
        }
    }

    private void showASTOverOverlay() {
        try {
            android.view.View overlay = getLayoutInflater().inflate(R.layout.dialog_overlay, null);
            android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setContentView(overlay);
            dialog.setCancelable(false);

            int limit = SettingsManager.getASTLimit(this);
            TextView msg1 = overlay.findViewById(R.id.txtMessage1);
            TextView msg2 = overlay.findViewById(R.id.txtMessage2);
            if (msg1 != null) {
                String msg = "Screen Time " + limit + " mins reached";
                msg1.setText(msg);
            }
            if (msg2 != null) {
                msg2.setText("Please play with other toys");
            }

            overlay.findViewById(R.id.btnOverlayOk).setOnClickListener(v -> {
                isLimitDialogShowing = false;
                SettingsManager.clearCache(this);
                finishAndRemoveTask();
                System.exit(0);
            });
            if (!isFinishing()) dialog.show();
        } catch (Exception ignored) {}
    }

    private void showStillInCoolOffPeriodDialog(int remainingMins) {
        try {
            android.view.View overlay = getLayoutInflater().inflate(R.layout.dialog_overlay, null);
            android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setContentView(overlay);
            dialog.setCancelable(false);

            TextView msg1 = overlay.findViewById(R.id.txtMessage1);
            TextView msg2 = overlay.findViewById(R.id.txtMessage2);
            if (msg1 != null) {
                msg1.setText("Still under Cool Off Period");
            }
            if (msg2 != null) {
                String msg = "Please wait " + remainingMins + " more minutes";
                msg2.setText(msg);
            }

            overlay.findViewById(R.id.btnOverlayOk).setOnClickListener(v -> {
                finishAndRemoveTask();
                System.exit(0);
            });
            if (!isFinishing()) dialog.show();
        } catch (Exception ignored) {}
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
        android.util.Log.d("PPG_NAV", "showSettingsDialog called");
        final android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        final androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        final TextView txtFolder = dialogView.findViewById(R.id.txtCurrentFolder);
        final TextView txtCount = dialogView.findViewById(R.id.txtVideoCount);
        final ProgressBar progressBar = dialogView.findViewById(R.id.dialogProgressBar);
        final android.view.View btnCloseX = dialogView.findViewById(R.id.btnCloseX);
        final android.widget.Button btnSelect = dialogView.findViewById(R.id.btnSelectFolder);
        final android.widget.Button btnRescan = dialogView.findViewById(R.id.btnRescan);
        final android.widget.Button btnSave = dialogView.findViewById(R.id.btnSave);
        final androidx.appcompat.widget.SwitchCompat switchRecent = dialogView.findViewById(R.id.switchShowRecent);
        final android.widget.EditText editAST = dialogView.findViewById(R.id.editAST);
        final android.widget.EditText editCoolOffPeriod = dialogView.findViewById(R.id.editCoolOffPeriod);
        final android.widget.EditText editRandom = dialogView.findViewById(R.id.editRandomThreshold);

        // Load current values
        switchRecent.setChecked(SettingsManager.isShowRecentEnabled(this));
        editAST.setText(String.valueOf(SettingsManager.getASTLimit(this)));
        editCoolOffPeriod.setText(String.valueOf(SettingsManager.getCoolOffPeriod(this)));
        editRandom.setText(String.valueOf(SettingsManager.getRandomThreshold(this)));

        // Scope observers to activity but only update if view is attached
        final androidx.lifecycle.Observer<String> folderObserver = folderPath -> {
            if (txtFolder == null) return;
            String displayPath = "None";
            if (folderPath != null) {
                try {
                    String decoded = Uri.decode(folderPath);
                    String pathPart = decoded.contains(":") ? decoded.substring(decoded.lastIndexOf(":") + 1) : decoded;
                    String[] segments = pathPart.split("/");
                    if (segments.length >= 2) displayPath = segments[segments.length - 2] + "/" + segments[segments.length - 1];
                    else displayPath = pathPart;
                } catch (Exception e) { displayPath = folderPath; }
            }
            txtFolder.setText(Html.fromHtml("<b>Current Folder: </b>" + displayPath, Html.FROM_HTML_MODE_LEGACY));
        };

        final androidx.lifecycle.Observer<Boolean> scanningObserver = isScanning -> {
            if (progressBar != null) progressBar.setVisibility(isScanning ? View.VISIBLE : View.GONE);
            if (btnSelect != null) btnSelect.setEnabled(!isScanning);
            if (btnRescan != null) btnRescan.setEnabled(!isScanning);
            if (btnSave != null) btnSave.setEnabled(!isScanning);
        };

        final androidx.lifecycle.Observer<java.util.List<Video>> videosObserver = videos -> {
            if (txtCount != null) {
                int count = (videos != null ? videos.size() : 0);
                txtCount.setText(Html.fromHtml("<b>Scanned Files Count: </b>" + count, Html.FROM_HTML_MODE_LEGACY));
            }
        };

        videoViewModel.getCurrentFolderPath().observe(this, folderObserver);
        videoViewModel.getIsScanning().observe(this, scanningObserver);
        videoViewModel.getVideos().observe(this, videosObserver);

        dialog.setOnDismissListener(d -> {
            videoViewModel.getCurrentFolderPath().removeObserver(folderObserver);
            videoViewModel.getIsScanning().removeObserver(scanningObserver);
            videoViewModel.getVideos().removeObserver(videosObserver);
        });

        btnCloseX.setOnClickListener(v -> dialog.dismiss());
        btnSave.setOnClickListener(v -> {
            try {
                SettingsManager.saveShowRecent(this, switchRecent.isChecked());
                SettingsManager.saveASTLimit(this, Integer.parseInt(editAST.getText().toString()));
                SettingsManager.saveCoolOffPeriod(this, Integer.parseInt(editCoolOffPeriod.getText().toString()));
                SettingsManager.saveRandomThreshold(this, Integer.parseInt(editRandom.getText().toString()));
                Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {}
            dialog.dismiss();
        });

        btnSelect.setOnClickListener(v -> folderPickerLauncher.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)));
        btnRescan.setOnClickListener(v -> {
            String currentPath = SettingsManager.getFolderPath(this);
            if (currentPath != null) videoViewModel.loadVideos(currentPath);
            else Toast.makeText(this, "Select a folder first", Toast.LENGTH_SHORT).show();
        });
        dialog.show();
    }

    private void showAboutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("About v3.0")
                .setMessage("A kid-friendly offline video player.\n\n" +
                        "New in v3.0: Active Screen Time (AST), Gesture Zones, and Recent tab.\n\n" +
                        "Note: Each time the app is installed or new videos are added, " +
                        "you must manually 'RESCAN' in Settings to update your list.")
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        androidx.navigation.fragment.NavHostFragment navHostFragment = (androidx.navigation.fragment.NavHostFragment) 
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
        }
        return super.onSupportNavigateUp();
    }
}