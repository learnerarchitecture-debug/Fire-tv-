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

## Home-key service checklist

The accessibility service is included in the build. Verify it on your Fire OS version:

1. Grant the setup permission:

   ```bash
   adb shell pm grant com.aurora.home android.permission.WRITE_SECURE_SETTINGS
   ```

2. Enable the service:

   ```bash
   adb shell am broadcast \
     -a com.aurora.home.ENABLE_HOME_KEY \
     -n com.aurora.home/.HomeKeyReceiver
   ```

3. Confirm the component is present:

   ```bash
   adb shell settings get secure enabled_accessibility_services
   ```

   Expected:

   ```text
   com.aurora.home/com.aurora.home.HomeAccessibilityService
   ```

4. Confirm accessibility is enabled:

   ```bash
   adb shell settings get secure accessibility_enabled
   ```

   Expected: `1`

5. Open YouTube, press the physical **Home** button, and inspect the resumed activity:

   ```bash
   adb shell dumpsys activity activities | grep mResumedActivity
   ```

   Expected:

   ```text
   mResumedActivity: ActivityRecord{... com.aurora.home/.HomeActivity ...}
   ```

6. Press **Menu + Home**. The stock Fire TV long-press Home behaviour should remain available because the service passes Home through while Menu is held.

7. Disable the service and confirm the stock launcher returns:

   ```bash
   adb shell am broadcast \
     -a com.aurora.home.DISABLE_HOME_KEY \
     -n com.aurora.home/.HomeKeyReceiver
   ```

> Some Fire OS builds reject sideloaded accessibility services. If the setting does not persist, use FTVLaunchX or document the limitation for your firmware. See [FIRETV_DEFAULT_HOME.md](FIRETV_DEFAULT_HOME.md).

## Useful ADB checks

```bash
# Current foreground
adb shell dumpsys activity activities | grep mResumedActivity

# Package and version
adb shell dumpsys package com.aurora.home | grep -E 'versionName|versionCode'

# Accessibility state
adb shell dumpsys accessibility

# Recent app logs
adb logcat -d | grep -E 'aurora|AppDrawer|HomeKey|AndroidRuntime'
```

## Known issues

| Issue | Status |
|---|---|
| Physical Home key is hardcoded to the stock launcher on Fire OS 5.1.1 | Workaround included: accessibility service. Availability depends on firmware. |
| Netflix is not installed on the test device | Dock fallback tile remains; drawer entry appears after installation. |
| Prime Video exits immediately on the test device | Reproduced from Fire OS itself; not an OBAA Home crash. |
| YouTube opens in its own task | Back returns to the stock launcher; Home-key service is the intended return path. |
| Fire OS 6+ accessibility behaviour | Not verified in this repository. |
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
- [ ] Home-key service can be enabled and disabled without disabling the stock launcher.
