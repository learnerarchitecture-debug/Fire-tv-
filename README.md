<div align="center">

# 🌌 OBAA Home

### A calm, glass-first launcher for Fire TV

**Midnight glass. Amber light. Every app one press away.**

[![Fire TV](https://img.shields.io/badge/Fire%20TV-5.1.1%20%C2%B7%20API%2022-FF9900?style=for-the-badge&logo=amazonfiretv&logoColor=white)](https://www.amazon.com/fire-tv)
[![Version](https://img.shields.io/badge/version-2.0-FFB45E?style=for-the-badge)](https://github.com/learnerarchitecture-debug/Fire-tv-/releases)
[![Package](https://img.shields.io/badge/package-com.aurora.home-7FB7FF?style=for-the-badge&logo=android&logoColor=white)](https://github.com/learnerarchitecture-debug/Fire-tv-)
[![Build](https://img.shields.io/badge/build-aapt2%20%C2%B7%20javac%20%C2%B7%20d8%20%C2%B7%20apksigner-5FE3C0?style=for-the-badge)](docs/BUILD.md)
[![License](https://img.shields.io/badge/license-MIT-A99BFF?style=for-the-badge)](LICENSE)

[Features](#-highlights) · [Screenshots](#-gallery) · [Install](#-quick-start) · [Fire TV Home](#-make-it-the-fire-tv-home) · [Build](#-build-from-source) · [Docs](#-documentation)

</div>

---

> **OBAA Home** is a full-screen Apple-TV-inspired launcher built for the Amazon Fire TV. It pairs a soft glass interface with a fast D-pad flow, a curated app dock, a Tinder-style recommendation deck, and a bottom app drawer that keeps every launchable app close without exposing Fire OS system apps.

![OBAA Home on Fire TV](docs/images/home-firetv.png)

---

## ✨ Highlights

| | |
|---|---|
| 🧊 **Glass-first design** | Midnight, Graphite and Aurora themes with five accent colours and adjustable glass strength. The default is **Midnight / Amber / 62%**. |
| 🕰️ **Beautiful clock** | Large minutes, optional 24-hour time, date and a live weather chip. |
| 🎬 **Recommendation deck** | Swipe or use the D-pad to save, pass and undo movie picks. Saved picks stay in a local watchlist. |
| 🚀 **Curated dock** | YouTube, Netflix and Prime Video in the footer. Missing apps keep a graceful fallback tile. |
| 📱 **Bottom app drawer** | A translucent six-column sheet with launchable third-party apps and curated main apps. Main apps are pinned first; Fire OS system apps are hidden. |
| 🎛️ **Full settings** | General, Appearance, Network, Display & Sound, Apps, Privacy and About panes, all reachable from the D-pad. |
| 🏠 **Fire TV Home support** | An optional accessibility service can bring OBAA Home to the front when the physical Home button is pressed. The stock launcher remains installed and recoverable. |
| 🎨 **On-device artwork** | Icons, posters and the aurora background are drawn locally. No artwork download is required. |
| 🔒 **Local by default** | No analytics. Weather comes directly from Open-Meteo. Preferences live in the `aurora` preference file. |

---

## 📸 Gallery

| Home | App drawer |
|:---:|:---:|
| ![Home screen](docs/images/home-firetv.png) | ![App drawer](docs/images/app-drawer.png) |

| Settings | Graphite theme |
|:---:|:---:|
| ![Settings](docs/images/settings.png) | ![Graphite theme](docs/images/theme-graphite.png) |

<div align="center">

**Recommendation deck**

![Recommendation deck](docs/images/recommendation-deck.png)

</div>

---

## 🚀 Quick Start

### Requirements

- Amazon Fire TV on **Fire OS 5.1.1 / Android 5.1 (API 22)** or a compatible Fire OS build
- ADB access to the Fire TV
- The prebuilt **`OBAAHome.apk`** from this repository

### Install

```bash
adb connect <fire-tv-ip>:5555
adb install -r OBAAHome.apk
```

The package ID is **`com.aurora.home`** and the app name is **OBAA Home**.

### First launch

```bash
adb shell monkey -p com.aurora.home -c android.intent.category.LAUNCHER 1
```

Or open **OBAA Home** from the Fire TV app list.

> The app drawer is intentionally not exported. Open it from the home screen dock: **Down → Down → Right → Right → OK**. Press **Back** to close it.

---

## 🏠 Make it the Fire TV Home

Fire OS 5.1.1 does **not** provide a normal “default launcher” picker. It also sends the physical Home key directly to the stock launcher, so changing the normal HOME preferred activity is not enough.

OBAA Home includes two pieces for this:

- **`HomeAccessibilityService`** — filters the physical Home key and opens `HomeActivity`.
- **`HomeKeyReceiver`** — a one-time setup helper that writes the accessibility component into secure settings.

### Enable the Home key service

> **Important:** installing the APK does not change the Fire TV Home button by itself. Fire OS 5.1.1 ignores normal default-launcher changes, so the accessibility service below must be enabled once.

```bash
# 1. Grant the one-time setup permission
adb shell pm grant com.aurora.home android.permission.WRITE_SECURE_SETTINGS

# 2. Enable OBAA Home's accessibility service
adb shell am broadcast \
  -a com.aurora.home.ENABLE_HOME_KEY \
  -n com.aurora.home/.HomeKeyReceiver

# 3. Verify
adb shell settings get secure enabled_accessibility_services
adb shell dumpsys accessibility
```

Or run the included helper from the repository root:

```bat
tools\setup-firetv-home.bat
```

On PowerShell:

```powershell
.\tools\setup-firetv-home.ps1
```

You can also open **OBAA Home → Settings → Apps → Default home → Enable now** after granting the permission, or enable **OBAA Home key** directly in **Fire TV Settings → Accessibility**.

### Verify

1. Open YouTube from the dock or the app drawer.
2. Press the physical **Home** button.
3. OBAA Home should come to the front.

```bash
adb shell dumpsys activity activities | grep mResumedActivity
# Expected: com.aurora.home/.HomeActivity
```

### Recover the stock launcher

The stock launcher is **never disabled**. Disable the OBAA Home key service at any time:

```bash
adb shell am broadcast \
  -a com.aurora.home.DISABLE_HOME_KEY \
  -n com.aurora.home/.HomeKeyReceiver
```

Or restore the accessibility setting manually:

```bash
adb shell settings put secure accessibility_enabled 0
```

> **Compatibility note:** Some Fire OS builds reject sideloaded accessibility services. If the service does not stay enabled on your device, install **FTVLaunchX**, grant it `WRITE_SECURE_SETTINGS`, and select **OBAA Home** as the target app. See [docs/FIRETV_DEFAULT_HOME.md](docs/FIRETV_DEFAULT_HOME.md) for the full technical note.

---

## 🎮 Controls

| Action | D-pad |
|---|---|
| Move focus | **Up / Down / Left / Right** |
| Select | **OK** |
| Back | **Back** |
| Open app drawer | From the default hero focus: **Down → Down → Right → Right → OK** |
| Close app drawer | **Back** |
| Open settings | Select the **Settings** dock item |
| Save a recommendation | Focus the **Save** action and press **OK** |
| Pass a recommendation | Focus the **Pass** action and press **OK** |
| Undo | Focus the **Undo** action and press **OK** |

The UI is designed for a remote, not a touchscreen. Focus rings, scale changes and the dock highlight make every step visible from across the room.

---

## 🎨 Personalization

Open **Settings → Appearance** to change:

| Setting | Options |
|---|---|
| Theme | **Midnight**, **Graphite**, **Aurora** |
| Accent | **Amber**, **Coral**, **Violet**, **Mint**, **Blue** |
| Glass intensity | Adjustable; default **62%** |
| Clock | 12/24-hour, date, weather chip |
| Motion | Reduce-motion toggle |
| Weather | City, Celsius/Fahrenheit, refresh |
| Dock apps | Show or hide YouTube, Netflix and Prime Video |
| Privacy | Clear weather cache, clear watchlist, reset settings |

Preferences are stored locally in the **`aurora`** preference file. No account is required.

---

## 🧱 Project Structure

```text
aurora-home/
├── AndroidManifest.xml          # Launcher, drawer, accessibility service, receiver
├── build.sh                     # aapt2 → javac → d8 → zipalign → apksigner
├── OBAAHome.apk                 # Prebuilt signed debug APK
├── debug.keystore               # Debug signing key (not for production)
├── res/
│   ├── drawable/                # Glass panels, chips, buttons, focus states
│   ├── layout/                  # Home, app drawer and settings layouts
│   ├── values/                  # Colours, dimensions, strings, styles
│   └── xml/                     # Accessibility service configuration
├── src/com/aurora/home/
│   ├── HomeActivity.java        # Home screen, clock, deck, dock, focus flow
│   ├── AppDrawerActivity.java   # Bottom-sheet app drawer
│   ├── AppGridAdapter.java      # Six-column app grid and focus animation
│   ├── SettingsActivity.java    # Seven-pane settings experience
│   ├── HomeAccessibilityService.java
│   ├── HomeKeyReceiver.java
│   ├── SwipeDeckView.java       # Recommendation deck gestures and D-pad
│   ├── MovieCatalog.java        # Local movie data
│   ├── Weather.java             # Open-Meteo client
│   ├── AuroraBackgroundView.java
│   ├── IconView.java
│   ├── IconLoader.java
│   └── ...
├── tools/
│   ├── GenIcon.java             # Generates launcher icons
│   ├── HomeDefault.java         # Optional shell helper for preferred activities
│   ├── setup-firetv-home.bat    # One-command Home-key setup (Windows)
│   └── setup-firetv-home.ps1    # One-command Home-key setup (PowerShell)
└── docs/
    ├── BUILD.md
    ├── INSTALL.md
    ├── ARCHITECTURE.md
    ├── TESTING.md
    ├── FIRETV_DEFAULT_HOME.md
    └── CHANGELOG.md
```

---

## 🛠️ Build from Source

The project intentionally avoids Gradle. It builds directly with the Android SDK tools:

```text
aapt2 compile → aapt2 link → javac → d8 → jar → zipalign → apksigner
```

### Windows / Git Bash layout

The build script expects the Android SDK and a JDK to be siblings of the project:

```text
workspace/
├── aurora-home/       ← this repository
├── android-sdk/
│   ├── build-tools/
│   └── platforms/android-22/android.jar
└── jdk-extract/
    └── <jdk>/bin/javac.exe
```

### Build

```bash
cd aurora-home
./build.sh
```

The signed APK is written to **`OBAAHome.apk`**. The script uses `debug.keystore` for local testing; replace it with your own release key before distributing a production build.

See [docs/BUILD.md](docs/BUILD.md) for prerequisites, environment details and troubleshooting.

---

## 🧪 Verification

Verified on a **Fire TV Stick (AFTT), Fire OS 5.1.1, Android 5.1.1 (API 22), 1920×1080**:

- ✅ Glass home screen, clock, weather chip, dock and recommendation deck
- ✅ Settings panes, themes, accents and glass slider
- ✅ App drawer opens from the dock, focuses the first app and closes with Back
- ✅ Six-column drawer with YouTube, Prime Video, JioHotstar, Send Files To TV, SmartTube and Xstream Play
- ✅ YouTube launches from the drawer
- ✅ No OBAA/AppDrawer crash in logcat
- ✅ Stock launcher remains installed and recoverable

**Known limitations**

- Fire OS 5.1.1 hardcodes the stock launcher for the physical Home key. The included accessibility service is the workaround; availability depends on the Fire OS build.
- Netflix is not installed on the test device; its dock tile remains a fallback.
- Prime Video is listed in the drawer, but the Prime app exits immediately on the test device even when launched directly from Fire OS.
- Back from YouTube returns to the stock launcher because YouTube opens in its own task; the Home key service is intended to bring OBAA Home back.
- Android TV is deferred to a separate **OBAA Smart TV** build with Android TV mappings.

See [docs/TESTING.md](docs/TESTING.md) for the full manual test checklist.

---

## 🗺️ Roadmap

- [x] Glass home screen with themes and accents
- [x] D-pad-first focus and navigation
- [x] Recommendation deck with save/pass/undo
- [x] Bottom app drawer with curated and third-party apps
- [x] Fire TV Home-key accessibility route
- [ ] Verify Home-key setup across more Fire OS versions
- [ ] Optional long-press Home menu integration
- [ ] Android TV variant: **OBAA Smart TV**
- [ ] Release signing and GitHub release assets

---

## 📚 Documentation

| Document | What it covers |
|---|---|
| [docs/INSTALL.md](docs/INSTALL.md) | Install, first run, Home-key setup and recovery |
| [docs/BUILD.md](docs/BUILD.md) | Toolchain, build pipeline and signing |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Activities, adapters, services and data flow |
| [docs/TESTING.md](docs/TESTING.md) | D-pad test checklist and verified behaviour |
| [docs/FIRETV_DEFAULT_HOME.md](docs/FIRETV_DEFAULT_HOME.md) | Why Fire OS ignores normal HOME changes, and the accessibility workaround |
| [docs/CHANGELOG.md](docs/CHANGELOG.md) | Version history and known issues |

---

## 🤝 Credits

- **OBAA Home** — design and implementation for Fire TV.
- **FTVLaunchX** by Erik Abele — the accessibility-service approach for intercepting the Fire TV Home key. FTVLaunchX is licensed under Apache 2.0.
- **Open-Meteo** — free weather data.
- Movie titles and poster artwork are represented by locally drawn placeholder art; no third-party poster files are bundled.
- Built with the Android SDK build tools and a raw `aapt2`/`javac`/`d8` pipeline.

---

<div align="center">

### OBAA Home

**Midnight glass. Amber light. Your Fire TV, calmer.**

[![GitHub](https://img.shields.io/badge/GitHub-learnerarchitecture--debug-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/learnerarchitecture-debug/Fire-tv-)

</div>
