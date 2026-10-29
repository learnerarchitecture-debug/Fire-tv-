# 🧪 Testing OBAA Home

This is the manual test checklist for the Fire TV build. The UI is designed for a remote, so use **D-pad key events**, not taps.

## Test environment

| Item | Value |
|---|---|
| Device | Amazon Fire TV Stick (`AFTT`) |
| OS | Fire OS 5.1.1 / Android 5.1.1 (API 22) |
| Resolution | 1920 × 1080 |
| Package | `com.aurora.home` |
| Version | 2.0 |
| ADB | `adb connect <fire-tv-ip>:5555` |

## Verified behaviour

- [x] Home screen renders the Midnight / Amber / 62% glass default.
- [x] Clock, date and weather chip are visible and readable from the couch.
- [x] Recommendation deck receives focus and supports save, pass and undo.
- [x] Settings opens from the dock and all seven panes render.
- [x] Themes (Midnight, Graphite, Aurora) and accents apply immediately.
- [x] Glass slider updates the preview and home surface.
- [x] App drawer opens from the **Apps** dock item.
- [x] First drawer item receives focus; the grid scrolls with the D-pad.
- [x] **Back** closes the drawer and returns focus to the **Apps** dock item.
- [x] Drawer shows six apps on the test device: YouTube, Prime Video, JioHotstar, Send Files To TV, SmartTube and Xstream Play.
- [x] YouTube launches from the drawer.
- [x] No OBAA Home or AppDrawer crash in logcat.
- [x] Stock launcher remains installed and enabled.

## D-pad path: open the app drawer

Starting from the default hero focus:

```text
Down → Down → Right → Right → OK
```

Expected result: the translucent bottom sheet opens; **YouTube** is focused first.

To close:

```text
Back
```

Expected result: `HomeActivity` is resumed and the **Apps** dock item is focused.

## Home-key watcher checklist

Fire OS 5.1.1 blocks third-party accessibility providers, does not define `DISABLE_KEYGUARD`, and filters `getRunningAppProcesses()` to OBAA's UID. The Home route therefore uses `UsageStatsManager`, which needs a one-time app-op grant. Verify:

1. Grant Usage access and open OBAA Home once. Opening starts the watcher automatically:

   ```bash
   adb shell appops set com.aurora.home GET_USAGE_STATS allow
   ```

2. Confirm the service is running and the grant is present:

   ```bash
   adb shell dumpsys activity services com.aurora.home | grep HomeKeyWatcherService
   adb shell appops get com.aurora.home GET_USAGE_STATS
   ```

3. Start a live log:

   ```bash
   adb logcat -s HomeKeyWatcherService HomeKeyReceiver
   ```

4. Open YouTube, press the physical **Home** button, and inspect the resumed activity:

   ```bash
   adb shell dumpsys activity activities | grep mResumedActivity
   ```

   Expected:

   ```text
   mResumedActivity: ActivityRecord{... com.aurora.home/.HomeActivity ...}
   ```

   Expected log:

   ```text
   I/HomeKeyWatcherService: poll: ... usage access=true, usage foreground=com.amazon.tv.launcher
   I/HomeKeyWatcherService: stock launcher foreground (usage), importance=-1, usage=com.amazon.tv.launcher
   I/HomeKeyWatcherService: launching OBAA Home (poll-usage)
   ```

   `importance=-1` is expected: the process list is filtered to OBAA's own UID, so detection comes from usage stats.

5. The stock launcher may remain visible for roughly 0.5–1.5 s before OBAA Home appears. That is expected on Fire OS 5.1.1.

6. Disable the watcher and confirm the stock launcher returns:

   ```bash
   adb shell am broadcast \
     -a com.aurora.home.DISABLE_HOME_KEY \
     -n com.aurora.home/.HomeKeyReceiver
   ```

7. Re-enable it before leaving the test device in the desired state:

   ```bash
   adb shell am broadcast \
     -a com.aurora.home.ENABLE_HOME_KEY \
     -n com.aurora.home/.HomeKeyReceiver
   ```

8. Revoke Usage access and confirm the watcher logs the missing-grant warning once:

   ```bash
   adb shell appops set com.aurora.home GET_USAGE_STATS deny
   adb logcat -s HomeKeyWatcherService
   # usage access missing; run: adb shell appops set com.aurora.home GET_USAGE_STATS allow
   ```

   Restore it with `allow` afterwards.

> Fire OS 5.1.1's `A11yAuthorizer` rejects sideloaded accessibility providers (`trust=false`). Do not expect FTVLaunchX or `HomeAccessibilityService` to bind on this firmware. See [FIRETV_DEFAULT_HOME.md](FIRETV_DEFAULT_HOME.md).

## Useful ADB checks

```bash
# Current foreground
adb shell dumpsys activity activities | grep mResumedActivity

# Package and version
adb shell dumpsys package com.aurora.home | grep -E 'versionName|versionCode'

# Home watcher service and Usage access
adb shell dumpsys activity services com.aurora.home | grep HomeKeyWatcherService
adb shell appops get com.aurora.home GET_USAGE_STATS

# Recent app logs
adb logcat -d | grep -E 'aurora|AppDrawer|HomeKey|AndroidRuntime'
```

## Known issues

| Issue | Status |
|---|---|
| Physical Home key is hardcoded to the stock launcher on Fire OS 5.1.1 | Workaround included: usage-stats watcher plus one-time app-op grant. The stock launcher may remain visible for ~0.5–1.5 s. |
| `DISABLE_KEYGUARD` broadcast is denied on Fire OS 5.1.1 | The permission is undefined on this firmware; the watcher uses usage stats instead. |
| `getRunningAppProcesses()` is filtered on Fire OS 5.1.1 | The process list only contains OBAA's UID; detection uses `UsageStatsManager`. |
| Netflix is not installed on the test device | Dock fallback tile remains; drawer entry appears after installation. |
| Prime Video exits immediately on the test device | Reproduced from Fire OS itself; not an OBAA Home crash. |
| YouTube opens in its own task | Back returns to the stock launcher; press Home to return to OBAA Home. |
| Fire OS 5.1.1 accessibility providers | Blocked by `A11yAuthorizer` (`trust=false`); the watcher does not use accessibility. |
| Fire OS 6+ Home behavior | Not verified in this repository. |
| Android TV | Deferred to a separate **OBAA Smart TV** build. |

## Regression checklist for future builds

- [ ] Home screen starts without a crash.
- [ ] D-pad focus never gets lost after Back.
- [ ] Dock Apps item opens the drawer.
- [ ] Drawer excludes OBAA Home and system apps.
- [ ] Main apps stay pinned before alphabetical apps.
- [ ] Prime Video label is shown for `com.amazon.avod`.
- [ ] Settings opens and closes with Back.
- [ ] Theme and accent changes survive a restart.
- [ ] Home-key watcher can be enabled and disabled without disabling the stock launcher.
