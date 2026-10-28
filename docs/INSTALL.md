# 🚀 Install OBAA Home

This guide covers a normal Fire TV install, first launch, the optional Fire TV Home-key service, and recovery.

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

## 6. Optional: Fire TV Home key

Fire OS 5.1.1 sends the physical Home key directly to the stock launcher. OBAA Home includes an accessibility service that filters the Home key and opens `HomeActivity` instead.

### Grant the setup permission

```bash
adb shell pm grant com.aurora.home android.permission.WRITE_SECURE_SETTINGS
```

### Enable the service

```bash
adb shell am broadcast \
  -a com.aurora.home.ENABLE_HOME_KEY \
  -n com.aurora.home/.HomeKeyReceiver
```

Or use **Settings → Apps → Default home → Enable now** in OBAA Home.

### Verify

```bash
adb shell settings get secure enabled_accessibility_services
adb shell dumpsys accessibility
adb shell dumpsys activity activities | grep mResumedActivity
```

Expected component:

```text
com.aurora.home/com.aurora.home.HomeAccessibilityService
```

### Test

1. Open YouTube from the dock or drawer.
2. Press the physical **Home** button.
3. OBAA Home should return to the foreground.

Holding **Menu** while pressing **Home** is intentionally left to Fire OS so the original long-press Home menu remains reachable.

## 7. Restore the stock launcher

The stock launcher is never disabled. Disable the OBAA Home key service:

```bash
adb shell am broadcast \
  -a com.aurora.home.DISABLE_HOME_KEY \
  -n com.aurora.home/.HomeKeyReceiver
```

Or reset accessibility:

```bash
adb shell settings put secure accessibility_enabled 0
```

If the service was enabled manually in Fire TV accessibility settings, turn it off there as well.

## Troubleshooting

| Problem | Try |
|---|---|
| `device offline` | Wake the Fire TV, reconnect with the current IP, and run `adb devices` again. |
| App drawer does not open | Open it from the **Apps** dock item; the drawer activity is not exported and cannot be started with `am start`. |
| Home button still opens the stock launcher | Confirm `WRITE_SECURE_SETTINGS` was granted and the broadcast succeeded. Check `settings get secure enabled_accessibility_services`. Some Fire OS builds reject sideloaded accessibility services; see [FIRETV_DEFAULT_HOME.md](FIRETV_DEFAULT_HOME.md). |
| Netflix tile is a fallback | Netflix is not installed on the device; install it from the Appstore and reopen the drawer. |
| Prime Video exits immediately | The Prime app exits on the test device even from Fire OS itself; this is not an OBAA Home crash. |
| YouTube returns to the stock launcher on Back | YouTube opens in its own task. The Home-key service is the intended way back to OBAA Home. |
| Weather does not load | Check the network connection and the city in **Settings → General → Weather city**. |

## Uninstall

```bash
adb uninstall com.aurora.home
```

Before uninstalling, disable the Home-key service so accessibility settings do not retain a missing component:

```bash
adb shell am broadcast \
  -a com.aurora.home.DISABLE_HOME_KEY \
  -n com.aurora.home/.HomeKeyReceiver
```
