# Test Plan - PPGVideoPlayer

## 1. Scope
Testing covers the core functionality of video discovery, list display, and playback on both phone and tablet form factors.

## 2. Test Environments
- **Physical Devices**: Android Smartphone (Portrait/Landscape), Fire HD Tablet (Portrait/Landscape).
- **Emulators**: Pixel 7 (API 34), Pixel Tablet.

## 3. Test Cases

### 3.1 Installation and Permissions
| ID | Description | Expected Result |
|---|---|---|
| TC-01 | Install app and launch | App launches to empty list or scan prompt. |
| TC-02 | Grant media permissions | App successfully begins scanning for videos. |
| TC-03 | Deny media permissions | App displays "Permission denied" toast and empty view. |

### 3.2 Video Discovery
| ID | Description | Expected Result |
|---|---|---|
| TC-04 | Scan folder with videos | Videos appear in grid with thumbnails and duration. |
| TC-05 | Scan empty folder | App displays "No videos found" message. |
| TC-06 | Select new folder in Settings | App rescans and updates list with new content. |

### 3.3 UI and Responsiveness
| ID | Description | Expected Result |
|---|---|---|
| TC-07 | Rotate phone to Landscape | Grid changes from 2 to 3 columns. |
| TC-08 | Launch on Tablet (sw600dp) | Grid displays 3 columns in Portrait, 5 in Landscape. |
| TC-09 | Verify YTK styling | Toolbar title is in a white bubble; cards have colorful borders. |

### 3.4 Video Playback
| ID | Description | Expected Result |
|---|---|---|
| TC-10 | Start playback | Video starts; player enters immersive full-screen mode. |
| TC-11 | Controls auto-hide | Controls disappear after 3 seconds of activity. |
| TC-12 | Film strip navigation | Tapping a video in the film strip switches the current video immediately. |
| TC-13 | Exit player | System bars return; app returns to the video list. |

## 4. Bug Reporting
Bugs should be documented with:
- Steps to reproduce.
- Expected vs. Actual result.
- Device model and Android version.
- Screen recordings if UI-related.
