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
    private int sessionActiveMins = 0;
    private final android.os.Handler astHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable astRunnable = new Runnable() {
        @Override
        public void run() {
            sessionActiveMins++;
            checkASTLimit();
            astHandler.postDelayed(this, 60000); // every minute
        }
    };

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
        if (checkCoolTime()) {
            // Cool time active, app will close or show message
        } else {
            astHandler.removeCallbacks(astRunnable);
            astHandler.postDelayed(astRunnable, 60000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        astHandler.removeCallbacks(astRunnable);
        android.os.PowerManager pm = (android.os.PowerManager) getSystemService(android.content.Context.POWER_SERVICE);
        if (pm != null && !pm.isInteractive()) {
            Log.d("PPG_NAV", "Screen is off in onPause, clearing cache and finishing");
            SettingsManager.clearCache(this);
            finishAndRemoveTask();
            System.exit(0);
        }
    }

    private void checkASTLimit() {
        int limit = SettingsManager.getASTLimit(this);
        if (sessionActiveMins >= limit) {
            SettingsManager.saveLastLimitTimestamp(this, System.currentTimeMillis());
            showASTOverDialog();
        }
    }

    private boolean checkCoolTime() {
        long lastReached = SettingsManager.getLastLimitTimestamp(this);
        if (lastReached == 0) return false;

        int coolTimeMins = SettingsManager.getCoolTime(this);
        long diff = System.currentTimeMillis() - lastReached;
        if (diff < (long) coolTimeMins * 60 * 1000) {
            showCoolTimeDialog((int) ((coolTimeMins * 60 * 1000 - diff) / 60000) + 1);
            return true;
        }
        return false;
    }

    private void showASTOverDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Screen Time Over")
                .setMessage("Your active screen time is over. Please take a break!")
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> {
                    finishAndRemoveTask();
                    System.exit(0);
                })
                .show();
    }

    private void showCoolTimeDialog(int remainingMins) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cool Off Time")
                .setMessage("Please wait " + remainingMins + " more minutes before playing again.")
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
        android.widget.EditText editCoolTime = dialogView.findViewById(R.id.editCoolTime);
        android.widget.EditText editRandom = dialogView.findViewById(R.id.editRandomThreshold);

        // Load current values
        switchRecent.setChecked(SettingsManager.isShowRecentEnabled(this));
        editAST.setText(String.valueOf(SettingsManager.getASTLimit(this)));
        editCoolTime.setText(String.valueOf(SettingsManager.getCoolTime(this)));
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
                SettingsManager.saveCoolTime(this, Integer.parseInt(editCoolTime.getText().toString()));
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
