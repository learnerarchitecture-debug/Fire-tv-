# 🧱 Architecture

OBAA Home is a single-module Android app with a raw resource/Java build. The UI is built around D-pad focus rather than touch.

## Entry points

| Component | Responsibility |
|---|---|
| `HomeActivity` | Full-screen launcher: aurora background, clock, weather, hero recommendation, deck actions, dock, focus routing. |
| `AppDrawerActivity` | Translucent bottom-sheet app drawer with a six-column grid. Not exported; opened from the dock. |
| `SettingsActivity` | Seven-pane settings surface: General, Appearance, Network, Display & Sound, Apps, Privacy, About. |
| `HomeKeyWatcherService` | Foreground service that detects the stock launcher at foreground importance and brings `HomeActivity` back to the front. Also keeps a `CLOSE_SYSTEM_DIALOGS` fast path. |
| `HomeKeyReceiver` | Enable/disable and boot helper for the Home watcher. |
| `HomeAccessibilityService` | Legacy optional accessibility route, kept for Fire OS builds that allow third-party providers. Fire OS 5.1.1 blocks it. |

## Home screen

`HomeActivity` composes:

- `AuroraBackgroundView` — theme-aware animated gradient.
- Clock and date block — large minutes, optional 24-hour format.
- Weather chip — Open-Meteo data, cached locally.
- `SwipeDeckView` / `MovieCardView` — recommendation deck with save, pass and undo actions.
- Dock — YouTube, Netflix, Prime Video, Apps and Settings.
- Focus routing — D-pad movement is explicit so the remote experience stays predictable.

The dock app visibility is controlled by `Prefs` keys such as `show_youtube`, `show_netflix` and `show_prime`. Missing apps keep a fallback tile.

## App drawer

`AppDrawerActivity` queries the package manager and builds a list:

1. Curated main packages, when installed:
   - YouTube (`com.amazon.firetv.youtube`, fallback `com.google.android.youtube.tv`)
   - Netflix (`com.netflix.ninja`, fallback `com.netflix.mediaclient`)
   - Prime Video (`com.amazon.avod`, displayed as **Prime Video**)
2. Other launchable third-party apps.
3. OBAA Home itself and Fire OS system apps are excluded.

Main apps are pinned first; the rest are sorted alphabetically. The grid uses six columns on the 1920×1080 Fire TV layout. `AppGridAdapter` handles icon loading through `IconLoader`, focus scaling and click dispatch.

The drawer is launched with a bottom-sheet animation from the dock. **Back** closes it and returns focus to the **Apps** dock item.

## Launching apps

`AppDrawerActivity` prefers a normal launch intent and falls back to a leanback intent. It avoids `FLAG_ACTIVITY_NEW_TASK` so returning to OBAA is preferred where the target app allows it. Some Fire TV apps, including YouTube, still open in their own task; the Home-key watcher is the intended way back.

## Themes and preferences

`Prefs` stores all state in the `aurora` preference file.

| Key family | Examples |
|---|---|
| Theme | `theme` = `midnight`, `graphite`, `aurora` |
| Accent | `accent` = `amber`, `coral`, `violet`, `mint`, `blue` |
| Glass | `glass` percentage |
| Clock | `clock_24h`, `show_date` |
| Weather | city, latitude/longitude, unit, cache |
| Deck | saved movie IDs |
| Dock | show/hide YouTube, Netflix, Prime |
| Motion | reduce-motion toggle |

Default look: **Midnight / Amber / 62% glass**.

## Home-key interception

Fire OS 5.1.1 routes the physical Home key through `FireTVKeyPolicyManager`, which explicitly starts `com.amazon.tv.launcher/.ui.HomeActivity_vNext`. Normal HOME preferred-activity changes therefore do not affect the remote’s Home button.

Fire OS also sends a protected `android.intent.action.CLOSE_SYSTEM_DIALOGS` broadcast with `reason=home`. It is registered-receivers-only and requires `DISABLE_KEYGUARD`, but Fire OS 5.1.1 does not define that permission for third-party apps. The system denies the broadcast to OBAA (and to other sideloaded apps). `HomeKeyWatcherService` therefore uses a foreground-process watcher:

- runs as a lightweight foreground service;
- polls `ActivityManager.getRunningAppProcesses()` every 500 ms while the screen is on;
- looks for `com.amazon.tv.launcher` at `IMPORTANCE_FOREGROUND`;
- starts `HomeActivity` with `NEW_TASK | REORDER_TO_FRONT | SINGLE_TOP`;
- skips polling whenever `HomeActivity` is resumed;
- debounces duplicate launches for 1.2 s;
- keeps a dynamic `CLOSE_SYSTEM_DIALOGS` receiver as a faster path on builds that do define `DISABLE_KEYGUARD`;
- returns `START_STICKY` and is restarted after boot by `HomeKeyReceiver`.

Fire OS 5.1.1's `A11yAuthorizer` blocks third-party accessibility providers (`trust=false`), so accessibility is not the Fire TV route. The legacy `HomeAccessibilityService` remains for builds that permit it but is not enabled on 5.1.1.

The stock launcher is never disabled. It may remain visible for roughly 0.5–1.5 s before OBAA Home appears.

See [FIRETV_DEFAULT_HOME.md](FIRETV_DEFAULT_HOME.md) for the platform investigation and recovery path.

## Data and privacy

- No analytics or telemetry.
- Weather is requested from Open-Meteo.
- Movie artwork is drawn locally; no external image assets are fetched.
- Preferences stay on the device.
