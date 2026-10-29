# 🚀 Install OBAA Home

This guide covers a normal Fire TV install, first launch, the built-in Fire TV Home watcher, and recovery.

## Requirements

- Amazon Fire TV with ADB debugging enabled
- Fire OS 5.1.1 / Android 5.1 (API 22) is the verified target
- A computer with `adb`
- The `OBAAHome.apk` from this repository

## 1. Connect ADB

```bash
adb connect <fire-tv-ip>:5555
adb devices
```

If the device shows `offline`, wake the Fire TV, confirm the IP address in **Settings → My Fire TV → About → Network**, and reconnect.

## 2. Install

```bash
adb install -r OBAAHome.apk
```

The package is `com.aurora.home` and the version is `2.0`.

## 3. Launch

```bash
adb shell monkey -p com.aurora.home -c android.intent.category.LAUNCHER 1
```

Or select **OBAA Home** from the Fire TV app list.

## 4. First-run tour

- The home screen opens on the hero recommendation.
- Move with the D-pad; press **OK** to select.
- The dock contains **YouTube**, **Netflix**, **Prime Video**, **Apps** and **Settings**.
- The **Apps** dock item opens the bottom app drawer.
- **Back** closes the drawer and returns to the home screen.

## 5. Open the app drawer

From the default hero focus:

```text
Down → Down → Right → Right → OK
```

The drawer uses six columns. Main apps are pinned first, followed by other launchable apps in alphabetical order. OBAA Home and Fire OS system apps are excluded.

Press **Back** to close the drawer.

## 6. Fire TV Home key

Fire OS 5.1.1 sends the physical Home key directly to the stock launcher, then emits a protected `CLOSE_SYSTEM_DIALOGS` broadcast with `reason=home`. The firmware does not define `DISABLE_KEYGUARD`, so third-party apps cannot receive that broadcast. OBAA Home therefore runs a small foreground watcher that checks whether the stock launcher is the foreground process and brings OBAA Home back to the front. No accessibility service or root is required.

The stock launcher may remain visible for roughly 0.5–1.5 s before OBAA Home appears. It is never disabled.

### Enable

Opening OBAA Home starts the watcher automatically. To enable it explicitly:

```bash
adb shell am broadcast \
  -a com.aurora.home.ENABLE_HOME_KEY \
  -n com.aurora.home/.HomeKeyReceiver
```

No `WRITE_SECURE_SETTINGS` grant is needed for the watcher.

Or use the included helper from the repository root:

```bat
tools\setup-firetv-home.bat
```

```powershell
.\tools\setup-firetv-home.ps1
```

You can also use **Settings → Apps → Default home → Enable now** in OBAA Home.

### Verify

```bash
# Watcher service is running
adb shell dumpsys activity services com.aurora.home | grep HomeKeyWatcherService

# Live watcher log
adb logcat -s HomeKeyWatcherService HomeKeyReceiver

# Resumed activity after pressing Home
adb shell dumpsys activity activities | grep mResumedActivity
```

Expected resumed activity:

```text
com.aurora.home/.HomeActivity
```

### Test

1. Open YouTube from the dock or drawer.
2. Press the physical **Home** button.
3. OBAA Home should return to the foreground after a short stock-launcher flash. Look for `stock launcher foreground` and `launching OBAA Home (poll)` in logcat.

## 7. Restore the stock launcher

The stock launcher is never disabled. Disable the Home watcher:

```bash
adb shell am broadcast \
  -a com.aurora.home.DISABLE_HOME_KEY \
  -n com.aurora.home/.HomeKeyReceiver
```

Or use **OBAA Home → Settings → Apps → Default home → Disable**. The next Home press goes to Fire OS normally.

## Troubleshooting

| Problem | Try |
|---|---|
| `device offline` | Wake the Fire TV, reconnect with the current IP, and run `adb devices` again. |
| App drawer does not open | Open it from the **Apps** dock item; the drawer activity is not exported and cannot be started with `am start`. |
| Home button still opens the stock launcher | Reinstall the latest APK, open OBAA Home once, and confirm `HomeKeyWatcherService` is running. Check `adb logcat -s HomeKeyWatcherService` for `stock launcher foreground` and `launching OBAA Home (poll)`. See [FIRETV_DEFAULT_HOME.md](FIRETV_DEFAULT_HOME.md). |
| Netflix tile is a fallback | Netflix is not installed on the device; install it from the Appstore and reopen the drawer. |
| Prime Video exits immediately | The Prime app exits on the test device even from Fire OS itself; this is not an OBAA Home crash. |
| YouTube returns to the stock launcher on Back | YouTube opens in its own task. Press Home to return to OBAA Home. |
| Weather does not load | Check the network connection and the city in **Settings → General → Weather city**. |

## Uninstall

```bash
adb uninstall com.aurora.home
```

Before uninstalling, disable the Home watcher:

```bash
adb shell am broadcast \
  -a com.aurora.home.DISABLE_HOME_KEY \
  -n com.aurora.home/.HomeKeyReceiver
```
