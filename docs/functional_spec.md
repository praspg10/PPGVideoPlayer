# Functional Specification - PPGVideoPlayer v2.0

## 1. Introduction
This document describes the functional behavior and technical architecture of PPGVideoPlayer, optimized for Amazon Fire HD tablets.

## 2. Application Flow
1. **Startup**: Opens to `VideoListFragment`. Checks for saved SVL.
2. **Initial State**: If no folder is configured, displays "No videos" message and only the "All" tab link.
3. **Manual Setup**: User selects a folder and clicks "Rescan" in the custom Settings dialog.
4. **Browsing**: Videos are presented in a persistent random order. Navigation between folders updates the filter; returning to "All" triggers a reshuffle.
5. **Playback**: Navigates to `VideoPlayerFragment` (Screen-2/3).
6. **Immersive Playback**: App enters global Sticky Immersive Mode. Status and Navigation bars are hidden but accessible via swipe.
7. **Back Navigation**: Explicit `popBackStack` to ensure return to the maintained list state without app closure.

## 3. Component Details
### 3.1 MainActivity
- Manages global system UI visibility (Immersive Mode).
- **System Navigation Styling**: Forces navigation bar to black background with white icons (clears `APPEARANCE_LIGHT_NAVIGATION_BARS`) for visibility on Fire OS.
- Handles `onBackPressed` to prevent accidental app exit on Fire OS.
- **AST Monitoring**: Implements a background timer to track active playback duration. Triggers a lockout and saves `last_limit_timestamp` when minutes exceed the threshold.
- **Cool Off Enforcement**: Checks `last_limit_timestamp` on startup/resume and prevents access if the difference is less than the configured `cool_time`.
- Hosts an enhanced Settings dialog with row-based configurations and a dedicated "SAVE" button.
- **Screen Off Handling**: Detects `ACTION_SCREEN_OFF`, clears temporary application cache, and closes the app immediately.

### 3.2 VideoListFragment
- Displays link-style folder tabs (underlined, red when selected).
- Limits view to 1 "All" link and up to 5 folder links.
- Maintains video grid state and scroll position during player transitions.
- **Header Actions**: Features a Settings icon (replacing 3-dots) and a Close (X) button that clears the application cache and finishes the activity.

### 3.3 VideoPlayerFragment
- Implements safety checks (binding null-checks) for background tasks.
- Uses `setKeepScreenOn(true)` at the view level for Fire OS compatibility.
- **Smart Playback Positioning**:
    - Consults `SettingsManager` for `random_threshold`.
    - If VPC >= threshold and video duration > 300,000ms, calculates a random seek position within the first 80% of content.
    - Otherwise, resets playback to 0:00.
- **Gesture Control System**: Implements a three-zone transparent overlay for player interaction:
    - **Zones**: Defined using `app:layout_constraintWidth_percent` (Left: 0.3, Middle: 0.4, Right: 0.3).
    - **Multi-tap Logic**: Uses a `Handler` with a 400ms window to count consecutive taps.
    - **Seeking**: Relative seek logic (`player.getCurrentPosition() + delta`) with boundary checks.
    - **Feedback**: A dynamic `TextView` (`txtSeekFeedback`) that uses `ViewPropertyAnimator` for smooth fade-out after seeks.
- **Screen Transitions**: Handles the transition from 100% full-screen (Screen-2) to the shrunken 75% view with film strip (Screen-3) via the middle-zone single tap.
- Disables forced orientation changes to prevent activity recreation crashes.
- Custom SeekBar with layer-list drawable (6dp thick).

### 3.4 VideoViewModel
- Stores `sessionVideos` to ensure stable randomization during a single session.
- Provides `reshuffleAll()` logic triggered by tab navigation.
- Persists and reloads the `ScannedVideoList` (SVL) via `SettingsManager`.

### 3.5 SettingsManager
- Handles JSON serialization of the video list for offline persistence.
- **Persistent States**: Manages storage for `avpt_limit`, `cool_time`, `random_threshold`, and `show_recent` toggle.
- **VPC Persistence**: Saves updated `playCount` for every video after each playback session.
- **Folder Path Truncation**: Provides logic to extract and display only the immediate parent and the selected folder name for UI display.
- **Cache Management**: Provides `clearCache()` to recursively delete temporary files in the app's cache directory while preserving the persistent SVL.

## 4. UI Design
- **Theme**: Material3 with `configChanges` in Manifest to handle orientation without restart.
- **Controls**: Back button icon (64dp target) and 24sp text labels.
- **Color Palette**: `ytk_primary_red` (#FF0000) for primary actions and indicators.
