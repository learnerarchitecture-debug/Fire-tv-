# 🏠 Fire TV Default Home: technical note

## The problem

Fire OS 5.1.1 does not behave like a normal Android TV build when the physical **Home** button is pressed. The key is handled by `FireTVKeyPolicyManager` in `fosservices.odex`, which explicitly starts:

```text
com.amazon.tv.launcher/.ui.HomeActivity_vNext
```

This bypasses the normal `ACTION_MAIN` + `CATEGORY_HOME` resolver. As a result:

- Setting OBAA Home as the normal HOME preferred activity is not enough.
- `pm set-home-activity` is not available.
- `pm disable-user --user 0 com.amazon.tv.launcher` is denied.
- `pm hide com.amazon.tv.launcher` reports `new hidden state: false`.
- `clearPackagePersistentPreferredActivities` is system-only and cannot be called by shell.
- The Fire TV settings UI has no usable default-launcher picker.

The stock launcher therefore remains the effective Home target even when OBAA Home is the normal HOME preference.

## The workaround

OBAA Home includes an **accessibility service** that filters key events before Fire OS processes the Home key.

Component:

```text
com.aurora.home/com.aurora.home.HomeAccessibilityService
```

Configuration:

```xml
<accessibility-service
    android:accessibilityEventTypes="typeWindowStateChanged"
    android:accessibilityFeedbackType="feedbackGeneric"
    android:accessibilityFlags="flagRequestFilterKeyEvents"
    android:canRequestFilterKeyEvents="true"
    android:notificationTimeout="100"
    android:description="@string/accessibility_service_description" />
```

Behaviour:

- On `KEYCODE_HOME` down, start `HomeActivity` with `NEW_TASK | CLEAR_TOP | SINGLE_TOP`.
- Consume both Home down and up so the stock launcher does not also receive the key.
- Track Menu state; if Menu is held, pass Home through so the original Fire TV long-press Home menu still works.

This approach is modelled on **FTVLaunchX** by Erik Abele (Apache 2.0), which uses the same accessibility-service technique.

## Enable

```bash
adb shell pm grant com.aurora.home android.permission.WRITE_SECURE_SETTINGS

adb shell am broadcast \
  -a com.aurora.home.ENABLE_HOME_KEY \
  -n com.aurora.home/.HomeKeyReceiver
```

Or use **Settings → Apps → Default home → Enable now**.

The receiver writes:

```text
Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES =
  com.aurora.home/com.aurora.home.HomeAccessibilityService
Settings.Secure.ACCESSIBILITY_ENABLED = 1
```

It preserves any other accessibility services already in the list.

## Verify

```bash
adb shell settings get secure enabled_accessibility_services
adb shell settings get secure accessibility_enabled
adb shell dumpsys accessibility
```

Then open YouTube, press **Home**, and check:

```bash
adb shell dumpsys activity activities | grep mResumedActivity
```

## Recovery

The stock launcher is never disabled. To stop intercepting Home:

```bash
adb shell am broadcast \
  -a com.aurora.home.DISABLE_HOME_KEY \
  -n com.aurora.home/.HomeKeyReceiver
```

Or:

```bash
adb shell settings put secure accessibility_enabled 0
```

If the service was enabled through Fire TV accessibility settings, disable it there as well. The stock launcher remains installed and can be used immediately.

## If the service is rejected

Some Fire OS builds clear `enabled_accessibility_services` when a sideloaded service is not accepted. If that happens:

1. Confirm the service is visible in Fire TV **Settings → Accessibility**.
2. Try enabling it through the Fire TV UI instead of ADB.
3. Install **FTVLaunchX**, grant it `WRITE_SECURE_SETTINGS`, and select **OBAA Home** as its target app.
4. Document the firmware version and the result; Fire OS 6.2.7.2+ is known to have changed behaviour for FTVLaunchX.

The normal HOME preferred-activity route can still be configured with the optional `tools/HomeDefault.java` helper, but it does not change the physical Home key behaviour on Fire OS 5.1.1.

## Security and scope

- The accessibility service is limited to key filtering and starting OBAA Home.
- It does not read screen content, perform gestures, or collect data.
- The setup receiver is exported so ADB can trigger it; it only changes the two accessibility settings above.
- `WRITE_SECURE_SETTINGS` is a privileged permission and must be granted explicitly with ADB. It is not granted automatically at install time.
