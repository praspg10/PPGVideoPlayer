# Functional Specification - PPGVideoPlayer v3.2

## 1. Introduction
This document describes the technical architecture and enhanced features of PPGVideoPlayer v3.2, optimized for Amazon Fire HD tablets.

## 2. Technical Architecture
### 2.1 Precision AST Tracking
The Active Screen Time (AST) system uses millisecond deltas to prevent timer drift.
- **Trigger**: Accrual begins only when `onIsPlayingChanged(true)` is received.
- **Calculation**: Real-time delta = `System.currentTimeMillis() - lastAccrualTimestamp`.
- **Sync**: A final accrual occurs on `pause()` or `stop()` to ensure zero-loss tracking.

### 2.2 Gesture Detection Model
Screen-2 (Player) is split into three transparent logical zones using a ConstraintLayout overlay:
- **Seek Relative**: `Left 25%` and `Right 25%` zones.
- **Control Toggle**: `Middle 50%` zone.
- **Debouncing**: Multi-tap logic waits 400ms for sequence completion. A proactive `isSeeking` flag blocks `showControls()` during side-taps to maintain 100% full-screen scale.

### 2.3 Layout Management
- **Screen-1 Header**: Uses vertical guidelines at 18%, 28%, and 90% to maintain a proportional grid across different screen sizes.
- **Dynamic Tabs**: `VideoListFragment` rebuilds tabs based on a unique folder set, limited to the first 5 discovered folders.

### 2.4 Folder Exclusion Logic
The scanning process in `VideoLibrary.java` implements a configurable recursive skip rule:
- **Setting**: Managed via `SettingsManager.isSkipScanEnabled()`.
- **Rule**: If enabled and a folder name contains the case-insensitive string `"-skipscan"`, the scanner aborts processing for that directory.
- **Effect**: No videos from that folder or any of its subdirectories are added to the SVL.

### 2.5 Navigation Menu
- **Header Icon**: Hamburger menu (3 lines) `ic_hamburger_menu.xml`.
- **Menu Items**: Added `ic_settings_gear.xml` icon to the "Settings" item using reflection to force icon visibility in `PopupMenu`.

## 3. Component Details
### 3.1 MainActivity
- **Lockout Enforcement**: Observes `isScreenTimeOver` LiveData. Displays non-dismissible `dialog_overlay.xml`.
- **Cool Off Logic**: Checks `lastLimitTimestamp` in `onResume`. Compares `System.currentTimeMillis() - lastLimit >= coolOffMs`.
- **Memory Safety**: Implements explicit `removeObserver()` logic for the Settings dialog to prevent observer leaks and frequent restarts.

### 3.2 VideoViewModel
- **VPC Management**: Manages `playCount` per video object. Play counts are persistent but reset upon a manual folder "RESCAN".
- **Transformations**: Exposes `totalPlaybackSeconds` as a mapped LiveData from millisecond raw data for clean UI updates.

### 3.3 SettingsManager
- **Persistence**: Added SharedPreferences keys for `ast_limit`, `cool_off_period`, `random_threshold`, and `show_recent`.
- **Default Values**: AST (30m), Cool Off (15m), Random Threshold (5 repeats).

### 3.4 VideoPlayerFragment
- **Smart Position Logic**: If `playCount >= threshold` AND `duration >= 5m`, calls `player.seekTo(randomPosition)` on start.
- **Visual Feedback**: `txtSeekFeedback` (36sp bold) is elevated to top Z-order to ensure visibility over the ExoPlayer surface.

## 4. UI Design Standards
- **Font Scale**: 
    - Header Branding: 26sp bold.
    - AST Counter: 16sp bold.
    - Lockout Msg1: 44sp bold.
    - Lockout Msg2: 54sp bold.
- **Color Palette**: `ytk_primary_red` (#FF0000) for indicators and play count badges.
- **Dimensions**:
    - Settings Width: 792dp (approx. 80% screen width).
    - Lockout Overlay: 85% screen width.
    - VPC Badge: 32dp diameter.
