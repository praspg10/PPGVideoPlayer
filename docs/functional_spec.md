# Functional Specification - PPGVideoPlayer v1.0

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
- Handles `onBackPressed` to prevent accidental app exit on Fire OS.
- Hosts a custom Settings dialog layout with "Select Folder to Scan" and "Rescan Now" links.
- Uses `launchMode="singleTop"` for navigation stability.

### 3.2 VideoListFragment
- Displays link-style folder tabs (underlined, red when selected).
- Limits view to 1 "All" link and up to 5 folder links.
- Maintains video grid state and scroll position during player transitions.

### 3.3 VideoPlayerFragment
- Implements safety checks (binding null-checks) for background tasks.
- Uses `setKeepScreenOn(true)` at the view level for Fire OS compatibility.
- Disables forced orientation changes to prevent activity recreation crashes.
- Custom SeekBar with layer-list drawable (6dp thick).

### 3.4 VideoViewModel
- Stores `sessionVideos` to ensure stable randomization during a single session.
- Provides `reshuffleAll()` logic triggered by tab navigation.
- Persists and reloads the `ScannedVideoList` (SVL) via `SettingsManager`.

### 3.5 SettingsManager
- Handles JSON serialization of the video list for offline persistence.

## 4. UI Design
- **Theme**: Material3 with `configChanges` in Manifest to handle orientation without restart.
- **Controls**: Back button icon (64dp target) and 24sp text labels.
- **Color Palette**: `ytk_primary_red` (#FF0000) for primary actions and indicators.
