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
- Optional Fire TV Home-key accessibility service and ADB setup helper.
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

- Fire OS 5.1.1 hardcodes the stock launcher for the physical Home key; the accessibility service is the workaround and depends on firmware acceptance.
- Netflix is not installed on the test Fire TV, so its drawer entry is absent until installed.
- Prime Video exits immediately on the test device even from Fire OS.
- YouTube opens in its own task; Back returns to the stock launcher.
- Android TV is deferred to a separate **OBAA Smart TV** package.

## Repository snapshot

This repository is the first public snapshot of the Fire TV build. Source, resources, build script, selected screenshots and the signed debug APK are included.

### Compatibility update

- Broadened the accessibility service event mask to `typeAllMask` so Fire OS accepts the service on more builds.
- Added `tools/setup-firetv-home.bat` and `tools/setup-firetv-home.ps1` for one-command installation and Home-key setup.
- Added clearer Home-key troubleshooting: the APK alone cannot override the Fire OS 5.1.1 Home button; the service must be enabled.
