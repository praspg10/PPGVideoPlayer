# Requirements Document - PPGVideoPlayer v3.0

## 1. Project Overview
PPGVideoPlayer is a kid-friendly video player designed for offline use on Android devices and tablets (specifically Fire HD). Version 3.0 introduces robust parental controls (Active Screen Time), intuitive multi-tap gesture zones, a "Recent" videos tab with play count badges, and a reorganized high-precision header layout.

## 2. Target Audience
- Children (primary users)
- Parents (administrators)

## 3. Functional Requirements
### 3.1 Video Discovery & Management (SVL)
- **Manual Scanning**: No background auto-scanning. Users must manually select a folder and click "RESCAN" in Settings.
- **Scanned Video List (SVL)**: Persists across sessions. Manual rescan resets Video Played Counts (VPC).
- **Tab Limits**: Displays "All", "Recent" (if enabled), and exactly the first 5 unique folders discovered. Maximum 7 tabs total.

### 3.2 Parental Controls (AST & Cool Off)
- **Active Screen Time (AST)**: Tracks cumulative video playback time with millisecond precision. Idle UI time, paused time, and browsing time are excluded.
- **Header Timer**: Displays current session playback time in "MM:SS" format (centered in a dedicated 10% header slot).
- **Lockout Enforcement**: Reaching the AST limit (default 30m) triggers a proactive stop in the player and a non-dismissible full-screen overlay.
- **Cool Off Period**: Access to the app is blocked for a configurable duration (default 15m) after the AST limit is reached.
- **Overlay UI**: High-visibility standardized popups with 44sp/54sp bold fonts for status messages.

### 3.3 Smart Playback & Recent Tab
- **Recent Tab**: Displays played videos sorted by play count.
- **VPC Badges**: 32dp red circle badges with 16sp white font displaying the "Video Played Count" on thumbnails.
- **Smart Position**: Videos exceeding the play count threshold (default 5) and duration (5m) start at a random position (between 0% and 80% of length).

### 3.4 User Interface
- **Header Layout**: Structured via guidelines: 18% Branding ("PPG Kids"), 10% AST Timer, 62% Tabs, 10% Actions (Settings/Close).
- **Settings Dialog**: 792dp wide layout with row-based configuration for AST, Cool Off, Recent Tab toggle, and Random Threshold. Buttons renamed to: VIDEO SOURCE, RESCAN, SAVE.
- **Screen-2/3 Gestures**:
    - **Left 25%**: Multi-tap (2/3/4+) to rewind 10/20/30 seconds.
    - **Right 25%**: Multi-tap (2/3/4+) to fast-forward 10/20/30 seconds.
    - **Middle 50%**: Single-tap to toggle controls and shrink video to 75% (Screen-3).
    - **Visual Feedback**: Large "+/- XX sec" message (36sp bold) appears at screen bottom during side-seeks.

### 3.5 Playback Stability
- **SeekBar**: 6dp thick custom drawable (Red/White).
- **Full-Screen Lock**: Multi-tap side-seeking proactively blocks the "shrink" logic to ensure video stays 100% full-screen during interaction.
- **Power Handling**: Immediate stop, cache clear, and exit when screen is turned off.

## 4. Non-Functional Requirements
### 4.1 Precision
- Millisecond-accurate playback accrual to prevent session time drift.

### 4.2 Accessibility
- Large touch targets (48dp icons, 64dp back button).
- Standardized high-contrast lockout overlays (85% screen width).

### 4.3 Reliability
- Fire OS 8 optimized. Handles orientation race conditions and aggressive system gestures.
- Memory leak prevention: Automatic cleanup of LiveData observers when dialogs close.
