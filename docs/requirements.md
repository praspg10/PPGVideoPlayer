# Requirements Document - PPGVideoPlayer v2.0

## 1. Project Overview
PPGVideoPlayer is a kid-friendly video player designed for offline use on Android devices and tablets (specifically Fire HD). It focuses on providing a safe, simple, and engaging interface for children to watch local video content.

## 2. Target Audience
- Children (primary users)
- Parents (administrators)

## 3. Functional Requirements
### 3.1 Video Discovery & Management (SVL)
- **Initial State**: On first launch with no folder configured, the app displays only the "All" link and a "No videos found" message.
- **Manual Scanning**: No background auto-scanning. Users must manually select a folder and click "Rescan" in Settings to build the library.
- **Scanned Video List (SVL)**: The app saves the list of scanned videos. On subsequent launches, it reloads this list without re-accessing the storage.
- **Manual Refresh**: Users must manually "RESCAN" if new videos are added to the storage or if the app is re-installed.
- **Recent Tab**: A "Recent" tab displays videos that have been played at least once. This list is reset upon manual "RESCAN".
- **Video Played Count (VPC)**: The app increments a play count for each video. In the "Recent" tab, a small circular badge in the top-right corner of each thumbnail displays its VPC.

### 3.2 User Interface
- **Screen-1 (Video List)**:
    - Navigation: Folder links (tabs) appear as underlined text links.
    - Limits: Displays "All", "Recent" (if enabled), plus up to 5 folder links.
    - Colors: Selected links use `ytk_primary_red` with an underline indicator.
    - Header: Gear Settings Icon replaces 3-dots menu. Added an (X) Close button to clear cache and exit.
- **Settings Popup**:
    - Layout: Increased size (approx. 30% wider) with large, readable fonts (18sp).
    - Rows:
        1. Recently Played Videos (Enable/Disable toggle).
        2. Active Screen Time (AST) limit in minutes.
        3. AST CoolOffPeriod in minutes.
        4. Smart Random Playback threshold (PlayCount).
    - Bottom Info: "Current Folder" (showing 1-level parent and folder name) and "Scanned Files Count" are positioned just above buttons.
    - Buttons: "VIDEO SOURCE" (select folder), "RESCAN" (manual update), and "SAVE" (persist settings).
    - Navigation: "X" icon in top-right corner for dismissal without saving.
- **Screen-3 (Video Player)**:
    - Filenames: Full filename displayed without extensions (e.g., no .mp4).
    - Controls: Play/Pause button positioned at 50% vertical bias for accessibility.
    - Film Strip: Displays random videos (excluding currently playing); reduced gaps and increased thumbnail width (253dp).
    - Immersive: Global Sticky Immersive Mode (no status or navigation bars). System navigation icons (triangle, circle, square) are forced to be white on a black background for maximum visibility.
    - Stay Awake: Screen stays at full brightness and never locks during playback.
    - Power Button: App stops playback, clears temporary cache, and closes immediately when the screen is turned off (Power button pressed).

### 3.3 Video Playback & Controls
- **SeekBar**: Custom style with white unplayed area (6dp thick) and red played area.
- **Navigation**: Selecting a video from the film strip immediately transitions to 100% full-screen playback.
- **Smart Playback Position**:
    - If VPC < threshold (default 5): Start from 0:00.
    - If VPC >= threshold and duration < 5 mins: Start from 0:00.
    - If VPC >= threshold and duration >= 5 mins: Start from a random position.
- **Active Screen Time (AST)**: App monitors active playback duration. When the limit (default 30m) is reached, it displays an overlay and closes.
- **Cool Off Period**: After reaching the AST limit, a cool-off period (default 15m) is enforced before the app allows further use.
- **Gesture Zones (Full Screen)**: The screen is divided into three horizontal zones:
    - **Left (30%)**: Double-tap to rewind -10s, triple-tap for -20s, quadruple-tap for -30s. Video remains full-screen.
    - **Middle (40%)**: Single-tap to show PAUSE button, shrink video to 75%, and display film strip (Screen-3).
    - **Right (30%)**: Double-tap to fast-forward +10s, triple-tap for +20s, quadruple-tap for +30s. Video remains full-screen.
- **Visual Feedback**: Seeking via gestures displays a temporary message at the bottom (e.g., "+10 sec" or "-20 sec") which fades out automatically.
- **Stability**: Robust back button functionality (<-) that returns to Screen-1 with the same list state, optimized for Fire OS 8.

## 4. Non-Functional Requirements
### 4.1 Performance
- Instant loading of the persistent SVL.
- Smooth transitions without activity recreation during orientation changes.

### 4.2 Usability
- Large touch targets (64dp back button).
- Minimum 24sp font size for critical labels in the player.

### 4.3 Reliability
- Fire OS 8 compatibility (handles aggressive system back gestures and orientation race conditions).
- Corrupted metadata handling (durations > 24h reset to 00:00).
