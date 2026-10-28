# 🛠️ Build OBAA Home

OBAA Home uses a small, explicit Android build pipeline. There is no Gradle wrapper and no third-party runtime dependency.

## Toolchain

- Windows with **Git Bash** (the script uses Windows `.exe` tool names)
- **JDK 8+** with `javac`, `jar`, `keytool`
- **Android SDK Build-Tools** with `aapt2`, `d8`, `zipalign`, `apksigner`
- **Android platform 22** (`android.jar`)

## Expected workspace layout

The current `build.sh` expects the SDK and JDK to be siblings of the project:

```text
workspace/
├── aurora-home/
│   ├── AndroidManifest.xml
│   ├── build.sh
│   ├── res/
│   ├── src/
│   └── tools/
├── android-sdk/
│   ├── build-tools/
│   │   └── <version>/
│   └── platforms/
│       └── android-22/
│           └── android.jar
└── jdk-extract/
    └── <jdk>/
        └── bin/
            ├── javac.exe
            ├── java.exe
            ├── jar.exe
            └── keytool.exe
```

If your tools live elsewhere, update `BT`, `AJ` and `JDK` at the top of `build.sh`.

## Build

```bash
cd aurora-home
./build.sh
```

The script performs:

1. Generate launcher icons with `tools/GenIcon.java`.
2. Compile resources with `aapt2 compile`.
3. Link resources and generate `R.java` with `aapt2 link`.
4. Compile Java with `javac` against `android.jar`.
5. Dex with `d8` for API 22.
6. Package and align with `jar` and `zipalign`.
7. Sign with `apksigner`.

Output:

```text
OBAAHome.apk
```

## Signing

The repository includes `debug.keystore` so local builds are reproducible. It uses the standard debug password `android` and is **not suitable for production distribution**.

For a release build, create a private keystore and update the signing step in `build.sh`:

```bash
keytool -genkeypair -keystore release.keystore -alias obaa \
  -keyalg RSA -keysize 2048 -validity 10950
```

Then sign with `apksigner` using your own alias and passwords. Do not commit a production keystore or its passwords.

## Install the build

```bash
adb install -r OBAAHome.apk
```

## Common build issues

| Symptom | Cause / fix |
|---|---|
| `build-tools not found` | Put the SDK at `../android-sdk` or update `BT` in `build.sh`. |
| `android.jar not found` | Install **Android 5.1 / API 22** platform or update `AJ`. |
| `JDK not found` | Put the JDK under `../jdk-extract` or update `JDK`. |
| `aapt2` attribute error | The resource uses an attribute newer than API 22. Keep accessibility event types compatible with API 22. |
| PowerShell reports exit code 1 | `build.sh` writes progress to stderr. Confirm the final line says `BUILD OK`. |
| APK installs but Home key does not work | The accessibility service must be enabled separately; see [INSTALL.md](INSTALL.md). |

## Clean build

```bash
rm -rf build
./build.sh
```

The generated `build/` directory is ignored by Git.
