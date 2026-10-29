# 🏠 Fire TV Default Home: technical note

## The problem

Fire OS 5.1.1 does not behave like a normal Android TV build when the physical **Home** button is pressed. The key is handled by `FireTVKeyPolicyManager` in `fosservices.odex`, which starts the stock launcher directly:

```text
com.amazon.tv.launcher/.ui.HomeActivity_vNext
```

This bypasses the normal `ACTION_MAIN` + `CATEGORY_HOME` resolver. As a result:

- Setting OBAA Home as the normal HOME preferred activity is not enough.
- `pm set-home-activity` is not available.
- `pm disable-user --user 0 com.amazon.tv.launcher` is denied.
- `pm hide com.amazon.tv.launcher` reports `new hidden state: false`.
- `clearPackagePersistentPreferredActivities` is system-only.
- The Fire TV settings UI has no usable default-launcher picker.

The stock launcher therefore remains the effective Home target unless another component reacts to the Home press.

## What Fire OS actually sends

When Home is pressed, the system log shows `FireTVKeyPolicyManager` sending a protected broadcast:

```text
act=android.intent.action.CLOSE_SYSTEM_DIALOGS flg=0x50000010 (has extras)
```

The extras include `reason=home`. The stock launcher receives it and starts. The broadcast is:

- **registered-receivers-only** (`FLAG_RECEIVER_REGISTERED_ONLY`), so a manifest receiver cannot receive it;
- protected by `android.permission.DISABLE_KEYGUARD` as the receiver permission.

## Why the broadcast is not enough on Fire OS 5.1.1

The permission check fails for every third-party receiver. Fire OS 5.1.1 does **not define** `android.permission.DISABLE_KEYGUARD`; `pm list permissions` only exposes `CONTROL_KEYGUARD` and `ACCESS_KEYGUARD_SECURE_STORAGE`. The system log shows:

```text
W/BroadcastQueue: Permission Denial: receiving Intent {
  act=android.intent.action.CLOSE_SYSTEM_DIALOGS flg=0x50000010 (has extras)
} to ProcessRecord{... 8907:com.aurora.home/u0a4} ... requires
  android.permission.DISABLE_KEYGUARD due to sender null (uid 1000)
```

The same denial appears for `com.amazon.venezia` and `com.amazon.aca`. Only system-UID processes bypass the check. Adding `DISABLE_KEYGUARD` to the manifest does not help because the permission is undefined; `dumpsys package com.aurora.home` never lists it as granted.

The broadcast receiver is retained in `HomeKeyWatcherService` as a fast path for Fire OS builds that do define the permission, but it is not the Fire TV 5.1.1 route.

## Why accessibility is not used

Fire OS 5.1.1 includes an `A11yAuthorizer` that rejects third-party accessibility providers. The system log reports:

```text
E/A11yAuthorizer: com.android.server.accessibility.AmazonA11yAuthorizerCallback
  .isSignedByValidCertificates(...)
E/A11yAuthorizer: ... shouldBlockProvider(...)
I/Vlog: FireOSFwk:A11yAuthorizer ... trust=false package=com.aurora.home
```

Writing `enabled_accessibility_services` may appear to succeed, but `accessibility_enabled` is reset to `0` and `dumpsys accessibility` shows no bound services. FTVLaunchX and similar accessibility hooks cannot bind on this firmware for the same reason.

The legacy `HomeAccessibilityService` remains in the source for Fire OS builds that allow third-party providers, but it is **not** the Fire TV 5.1.1 route.

## The OBAA Home watcher

`HomeKeyWatcherService` is a lightweight foreground service. It uses the foreground-process list, which is available to normal apps on API 22 without a special permission:

- It polls `ActivityManager.getRunningAppProcesses()` every **500 ms** while the screen is on.
- It looks for `com.amazon.tv.launcher` with `importance == IMPORTANCE_FOREGROUND`.
- When found, it starts `HomeActivity` with `NEW_TASK | REORDER_TO_FRONT | SINGLE_TOP`.
- Polling is skipped whenever `HomeActivity` is already resumed.
- Launches are debounced for **1.2 s** to avoid duplicate starts.
- On builds that define `DISABLE_KEYGUARD`, the dynamic `CLOSE_SYSTEM_DIALOGS` receiver provides a faster path.
- It returns `START_STICKY`, and `HomeKeyReceiver` starts it after boot.

The stock launcher is never disabled. It may remain visible for roughly **0.5–1.5 s** while the poll detects it and OBAA Home starts. On builds where the broadcast is allowed, the delay is shorter.

## Manifest pieces

```xml
<uses-permission android:name="android.permission.DISABLE_KEYGUARD"/>
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>

<service
    android:name=".HomeKeyWatcherService"
    android:exported="false"
    android:stopWithTask="false"/>

<receiver
    android:name=".HomeKeyReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="com.aurora.home.ENABLE_HOME_KEY"/>
        <action android:name="com.aurora.home.DISABLE_HOME_KEY"/>
        <action android:name="android.intent.action.BOOT_COMPLETED"/>
    </intent-filter>
</receiver>
```

`HomeActivity` also calls `HomeKeyWatcherService.ensureStarted(this)` so opening OBAA Home once starts the watcher.

## Enable

The watcher is enabled by default. Opening OBAA Home starts it. To enable it explicitly:

```bash
adb shell am broadcast \
  -a com.aurora.home.ENABLE_HOME_KEY \
  -n com.aurora.home/.HomeKeyReceiver
```

Or use **OBAA Home → Settings → Apps → Default home → Enable now**.

No `WRITE_SECURE_SETTINGS` grant is required for the watcher.

## Verify

```bash
# The service should be listed
adb shell dumpsys activity services com.aurora.home | grep HomeKeyWatcherService

# Watch the watcher
adb logcat -s HomeKeyWatcherService HomeKeyReceiver

# After pressing Home from YouTube or another app
adb shell dumpsys activity activities | grep mResumedActivity
# Expected: com.aurora.home/.HomeActivity
```

Expected log lines:

```text
I/HomeKeyWatcherService: watching for Home (broadcast + foreground poll)
I/HomeKeyWatcherService: poll: 142 processes, launcher importance=300
I/HomeKeyWatcherService: stock launcher foreground, importance=100
I/HomeKeyWatcherService: launching OBAA Home (poll)
```

`launcher importance=100` is `IMPORTANCE_FOREGROUND`; `300` is a background service. The exact process count varies by device.

## Recovery

The stock launcher is never modified. Disable the watcher:

```bash
adb shell am broadcast \
  -a com.aurora.home.DISABLE_HOME_KEY \
  -n com.aurora.home/.HomeKeyReceiver
```

Or use **OBAA Home → Settings → Apps → Default home → Disable**. The next Home press goes to Fire OS normally. Uninstalling OBAA Home also removes the watcher.

## Troubleshooting

| Symptom | Check |
|---|---|
| Home still opens the stock launcher | Confirm `HomeKeyWatcherService` is running: `adb shell dumpsys activity services com.aurora.home \| grep HomeKeyWatcherService`. Then watch `adb logcat -s HomeKeyWatcherService`. A `poll:` line with `launcher importance=100` means detection is working. |
| `poll:` never appears | The watcher is not running, the screen is off, or `getRunningAppProcesses()` is filtered on your firmware. Open OBAA Home once and retry. |
| `launcher importance=-1` | The stock launcher process is not in the returned list. This firmware may restrict the process list; try the broadcast fast path or document the firmware. |
| `DISABLE_KEYGUARD` is missing from `dumpsys package` | Expected on Fire OS 5.1.1. The permission is undefined, which is why the broadcast path is not used. |
| Stock launcher flashes | Expected on Fire OS 5.1.1; the poll interval and launch time determine the flash length. |
| No `reason=home` log | Expected on Fire OS 5.1.1; the receiver cannot pass the permission check. Use the `poll:` log instead. |
| Accessibility route is needed | Do not use it on Fire OS 5.1.1; `A11yAuthorizer` blocks third-party providers. Use the foreground watcher. |

## Security and scope

- The watcher does not use accessibility and does not read screen content.
- It only checks process names and foreground importance; it does not inspect app data.
- Polling runs only while the screen is on and stops when OBAA Home is resumed.
- It collects no data and sends nothing off-device.
- The stock launcher remains installed and recoverable at all times.
