# Test Plan - PPGVideoPlayer v1.0

## 1. Scope
Testing covers the core functionality of manual video discovery, persistent randomization, immersive playback, and Fire HD tablet stability.

## 2. Test Environments
- **Physical Devices**: Fire HD 10 Tablet (Fire OS 8.3.3.8).
- **Secondary Devices**: Android Smartphone (API 24+).

## 3. Test Cases

### 3.1 Initialization & Manual Scan
| ID | Description | Expected Result |
|---|---|---|
| TC-01 | First Launch (No config) | Displays "All" link and "No videos found" message. No auto-scan. |
| TC-02 | Select Folder and Rescan | Popup displays "Scanned Files Count" updating in real-time. Grid populates with random order. |
| TC-03 | Relaunch App | SVL reloads instantly with the same random order as previous session. |
| TC-04 | Folder Navigation | Link-style tabs change color to red when selected. List filtered by folder. |
| TC-05 | Return to "All" | List is reshuffled with a new random sequence. |

### 3.2 Screen-3 UI & UX
| ID | Description | Expected Result |
|---|---|---|
| TC-06 | Verify Filename | Filename displayed in 24sp bold text without ".mp4" or other extensions. |
| TC-07 | SeekBar Styling | Track is white and 6dp thick; progress is red. |
| TC-08 | Film Strip Click | Selected video plays in 100% full screen immediately; strip disappears. |
| TC-09 | Play/Pause Bias | Button is centered at 50% vertical bias (not too high). |

### 3.3 Immersive Experience & Stability
| ID | Description | Expected Result |
|---|---|---|
| TC-10 | Immersive Mode | Status and Navigation bars are invisible on all screens. |
| TC-11 | Stay Awake | Screen stays at full brightness for > 1 minute without dimming or locking. |
| TC-12 | Fire OS Back Button | Pressing "<-" in Player returns to Screen-1 with state preserved. App does NOT exit. |
| TC-13 | Orientation Safety | Holding tablet in standing position (Portrait) does not crash the player on start or exit. |

### 3.4 Data Integrity
| ID | Description | Expected Result |
|---|---|---|
| TC-14 | Corrupted Duration | Video with invalid metadata shows "00:00" instead of 8-digit numbers. |
| TC-15 | Rescan Update | Adding a file to storage and clicking "Rescan Now" updates the Scanned Files Count. |

## 4. Bug Reporting
Bugs should be documented with:
- Logcat entries (filtered by `tag:PPG_NAV`).
- Steps to reproduce.
- Expected vs. Actual result.
- Device model and Fire OS version.
