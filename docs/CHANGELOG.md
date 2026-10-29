# 📝 Changelog

## 2.0 — Glass launcher

### Added

- Full-screen glass home screen with Midnight, Graphite and Aurora themes.
- Amber, Coral, Violet, Mint and Blue accent colours.
- Adjustable glass intensity, default 62%.
- Large-minute clock with optional 24-hour time, date and weather chip.
- Tinder-style recommendation deck with save, pass and undo.
- Local watchlist.
- Curated footer dock: YouTube, Netflix and Prime Video.
- Bottom-sheet **Apps** drawer with a six-column grid.
- Main apps pinned first; other launchable apps sorted alphabetically.
- Fire OS system apps and OBAA Home excluded from the drawer.
- Prime Video label override for `com.amazon.avod`.
- Full settings surface: General, Appearance, Network, Display & Sound, Apps, Privacy, About.
- Fire TV Home watcher using a foreground-process check, with the protected `CLOSE_SYSTEM_DIALOGS` broadcast as a fast path on compatible builds; no accessibility service required.
- Optional legacy Fire TV accessibility service for builds that permit third-party providers.
- On-device generated icons, posters and aurora backgrounds.
- Raw `aapt2` → `javac` → `d8` → `zipalign` → `apksigner` build pipeline.

### Changed

- Package name: `com.aurora.home`.
- App name: **OBAA Home**.
- Version: `2.0`.
- Preference file: `aurora`.
- App launches prefer returning to OBAA; `FLAG_ACTIVITY_NEW_TASK` is omitted from drawer launches.
- Settings > Apps note now points to the bottom app drawer.

### Known issues

- Fire OS 5.1.1 hardcodes the stock launcher for the physical Home key; the foreground watcher brings OBAA Home back after a brief stock-launcher flash. Third-party accessibility providers are blocked by `A11yAuthorizer`, and `DISABLE_KEYGUARD` is undefined on this firmware.
- Netflix is not installed on the test Fire TV, so its drawer entry is absent until installed.
- Prime Video exits immediately on the test device even from Fire OS.
- YouTube opens in its own task; Back returns to the stock launcher.
- Android TV is deferred to a separate **OBAA Smart TV** package.

## Repository snapshot

This repository is the first public snapshot of the Fire TV build. Source, resources, build script, selected screenshots and the signed debug APK are included.

### Compatibility update

- Reworked the accessibility service to use the Fire TV Glass/FTVLaunchX configuration: `typeWindowStateChanged` filtered to `com.amazon.tv.launcher`, plus a window-event fallback that brings OBAA Home back when the stock launcher appears.
- Added `tools/setup-firetv-home.bat` and `tools/setup-firetv-home.ps1` for one-command installation and Home-key setup.
- Added clearer Home-key troubleshooting: the APK alone cannot override the Fire OS 5.1.1 Home button; the watcher starts automatically when OBAA Home opens.

### Home watcher update

- Confirmed Fire OS 5.1.1 blocks third-party accessibility providers in `A11yAuthorizer` (`trust=false`).
- Confirmed Fire OS 5.1.1 does not define `DISABLE_KEYGUARD`, so the protected `CLOSE_SYSTEM_DIALOGS` broadcast with `reason=home` is denied to OBAA and other sideloaded apps.
- Added `HomeKeyWatcherService`, a foreground service that polls `getRunningAppProcesses()` every 500 ms while the screen is on and detects `com.amazon.tv.launcher` at `IMPORTANCE_FOREGROUND`.
- Kept a dynamic `CLOSE_SYSTEM_DIALOGS` receiver as a fast path on builds that do define `DISABLE_KEYGUARD`.
- Added `DISABLE_KEYGUARD` and `RECEIVE_BOOT_COMPLETED` permissions.
- Polling is skipped when `HomeActivity` is resumed; launches are debounced for 1.2 s.
- Added boot restart through `HomeKeyReceiver`.
- Removed the `WRITE_SECURE_SETTINGS` requirement from the Home-key setup path.
- Updated the setup scripts, settings dialog and documentation for the foreground watcher.
