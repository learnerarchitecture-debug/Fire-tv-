#!/bin/bash
# Builds OBAAHome.apk with the raw Android toolchain (no Gradle):
# aapt2 -> javac -> d8 -> zipalign -> apksigner
set -e
cd "$(dirname "$0")"
ROOT="$PWD"
WS="$(cd .. && pwd)"
BT="$(ls -d "$WS"/android-sdk/build-tools/*/ 2>/dev/null | head -1)"
AJ="$WS/android-sdk/platforms/android-22/android.jar"
JDK="$(ls -d "$WS"/jdk-extract/*/ 2>/dev/null | head -1)"
JBIN="$JDK/bin"

[ -n "$BT" ] || { echo "ERROR: build-tools not found in $WS/android-sdk"; exit 1; }
[ -f "$AJ" ] || { echo "ERROR: android.jar not found at $AJ"; exit 1; }
[ -x "$JBIN/javac.exe" ] || { echo "ERROR: JDK not found in $WS/jdk-extract"; exit 1; }

rm -rf build
mkdir -p build/gen build/obj build/tools

echo "[1/7] generating icons"
"$JBIN/javac.exe" -encoding UTF-8 -d build/tools tools/GenIcon.java
"$JBIN/java.exe" -cp build/tools GenIcon "$ROOT/res"

echo "[2/7] compiling resources (aapt2 compile)"
"$BT/aapt2.exe" compile --dir res -o build/res.zip

echo "[3/7] linking resources (aapt2 link)"
"$BT/aapt2.exe" link -o build/app.unsigned.apk -I "$AJ" \
  --manifest AndroidManifest.xml --java build/gen \
  --min-sdk-version 22 --target-sdk-version 22 \
  --version-code 2 --version-name 2.0 \
  build/res.zip

echo "[4/7] compiling java (javac)"
find src build/gen -name "*.java" > build/sources.txt
"$JBIN/javac.exe" -source 1.8 -target 1.8 -nowarn -encoding UTF-8 \
  -bootclasspath "$AJ" -d build/obj @build/sources.txt

echo "[5/7] dexing (d8)"
CLASSES=$(find build/obj -name "*.class")
"$JBIN/java.exe" -cp "$BT/lib/d8.jar" com.android.tools.r8.D8 \
  --release --min-api 22 --lib "$AJ" --output build $CLASSES

echo "[6/7] packaging (jar + zipalign)"
(cd build && "$JBIN/jar.exe" uf app.unsigned.apk classes.dex)
"$BT/zipalign.exe" -f 4 build/app.unsigned.apk build/app.aligned.apk

echo "[7/7] signing (apksigner)"
if [ ! -f debug.keystore ]; then
  "$JBIN/keytool.exe" -genkeypair -keystore debug.keystore -alias aurora \
    -keyalg RSA -keysize 2048 -validity 10950 \
    -storepass android -keypass android \
    -dname "CN=Aurora Home,O=Aurora,C=US" >/dev/null 2>&1
fi
"$JBIN/java.exe" -cp "$BT/lib/apksigner.jar" com.android.apksigner.ApkSignerTool sign \
  --ks debug.keystore --ks-key-alias aurora \
  --ks-pass pass:android --key-pass pass:android \
  --out OBAAHome.apk build/app.aligned.apk

ls -la OBAAHome.apk
echo "BUILD OK: $ROOT/OBAAHome.apk"
