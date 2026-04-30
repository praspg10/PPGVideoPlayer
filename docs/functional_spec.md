# Functional Specification - PPGVideoPlayer

## 1. Introduction
This document describes the functional behavior and technical architecture of PPGVideoPlayer.

## 2. Application Flow
1. **Startup**: The app opens to the `VideoListFragment`.
2. **Scanning**: On the first run, the app requests permission to access media. It then scans the default or selected folder.
3. **Browsing**: Users browse videos in a colorful grid.
4. **Playback**: Tapping a video navigates to `VideoPlayerFragment`.
5. **Immersive Playback**: The player enters immersive mode, hiding system bars. Controls auto-hide after a delay.
6. **Switching**: Users can tap videos in the film strip to change the playing video without leaving the player.

## 3. Component Details
### 3.1 MainActivity
- Hosts the `NavHostFragment`.
- Manages the `MaterialToolbar` visibility and style.
- Handles folder selection via `ActivityResultLauncher`.

### 3.2 VideoListFragment
- Uses a `RecyclerView` with a `GridLayoutManager`.
- Span count is dynamically determined by resource qualifiers (`values/integers.xml`, `values-land/integers.xml`, etc.).
- Observes `VideoViewModel` for data updates.

### 3.3 VideoPlayerFragment
- Uses `ExoPlayer` (Media3) for playback.
- Manages system UI visibility (Immersive Mode).
- Contains a secondary `RecyclerView` (Film Strip) for quick navigation.

### 3.4 VideoViewModel
- Uses `AndroidViewModel` to survive configuration changes.
- Loads and verifies video availability in background threads.

### 3.5 SettingsManager
- Handles persistent storage of folder paths and video metadata using `SharedPreferences` and `Gson`.

## 4. UI Design
- **Theme**: Based on `Material3.Light`, customized with YTK colors.
- **Components**: `MaterialCardView` with thick colorful strokes.
- **Responsiveness**: Qualified resources for different screen widths (sw600dp for tablets).
