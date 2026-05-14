# Test Plan - PPGVideoPlayer v3.1

## 1. Scope
Testing covers precision parental controls, multi-tap gesture zones, persistent play count tracking, large-scale UI elements, and folder exclusion rules.

## 2. Test Cases

### 3.1 Folder Exclusion
| ID | Description | Expected Result |
|---|---|---|
| TC-18 | Folder Exclusion (-skipscan) | Create a folder named "Private-skipscan" containing videos. Perform RESCAN. Videos from this folder should NOT appear in the list. |
| TC-19 | Subfolder Exclusion | Create a subfolder with "-skipscan" inside a normal folder. Perform RESCAN. Normal folder videos show, subfolder videos are hidden. |

### 3.2 Parental Controls (AST & Cool Off)
| ID | Description | Expected Result |
|---|---|---|
| TC-21 | AST Counter Accuracy | Header timer syncs with real video time. 1 min playback = 01:00 on counter. |
| TC-22 | AST Limit Reach | Video stops immediately. Overlay Msg1 "Screen Time XX mins reached" appears in 44sp bold. |
| TC-23 | Cool Off Block | Relaunching app during cool-off shows Msg1 "Still under Cool Off Period" and Msg2 "Please wait XX more minutes" in 54sp. |
| TC-24 | Settings Configuration | Changing AST limit to 5 mins and saving correctly updates the lockout threshold. |

### 3.2 Gesture Zones & Multi-Tap
| ID | Description | Expected Result |
|---|---|---|
| TC-25 | Double Tap Left | Video rewinds 10s. Text "-10 sec" pops up at bottom. Screen stays 100% full scale. |
| TC-26 | Triple Tap Right | Video fast-forwards 20s. Text "+20 sec" pops up. Screen stays 100% full scale. |
| TC-27 | Middle Single Tap | Video area shrinks to 75% (Screen-3). Film strip and controls appear. |
| TC-28 | Side-Seek Scaling | Multi-tapping left/right while in 75% mode hides controls and returns to 100% scale immediately. |

### 3.3 Recent Tab & Smart Playback
| ID | Description | Expected Result |
|---|---|---|
| TC-29 | VPC Badge Visibility | Played videos show a 32dp red circle with the play count number. |
| TC-30 | Recent Tab Filtering | "Recent" tab appears only after first playback. Displays videos sorted by highest play count. |
| TC-31 | Smart Position Trigger | Video with 5+ plays and 5m+ length starts from a random middle position. |
| TC-32 | VPC Reset | Clicking "RESCAN" in settings resets all Video Played Counts and hides the "Recent" tab. |

### 3.4 UI Layout & Grid
| ID | Description | Expected Result |
|---|---|---|
| TC-33 | Header Slot Alignment | Branding (18%), Timer (10%), Tabs (62%), and Actions (10%) are correctly spaced. |
| TC-34 | Folder Tab Limit | Storage with 10 folders only displays the first 5 folders discovered as tabs. |
| TC-35 | Settings Dimensions | Settings dialog is large (792dp) and fits all 4 config rows without scrolling. |

### 3.5 System Stability
| ID | Description | Expected Result |
|---|---|---|
| TC-36 | Settings Restart Loop | Rapidly opening/closing Settings dialog 10 times does not cause the app to restart or crash. |
| TC-37 | Power Button Behavior | Turning screen off during playback accrues the final partial second of time and exits app. |

## 4. Bug Reporting
Bugs must include:
- Logcat filtered by `tag:PPG_NAV` (Navigation) or `tag:PPG_AST` (Timer/Lockout).
- Screenshot of the standard 44sp/54sp overlays if layout issues occur.
