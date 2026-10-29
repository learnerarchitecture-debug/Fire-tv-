@echo off
setlocal
rem OBAA Home - Fire TV Home-key setup
rem Usage: setup-firetv-home.bat [device-serial]
rem Run from the repository root or double-click. Requires adb on PATH.

set "SERIAL=%~1"
set "ADB=adb"
if not "%SERIAL%"=="" set "ADB=adb -s %SERIAL%"

set "APK=%~dp0..\OBAAHome.apk"
if not exist "%APK%" (
  echo ERROR: OBAAHome.apk not found at "%APK%"
  pause
  exit /b 1
)

echo.
echo [1/5] Installing OBAA Home...
%ADB% install -r "%APK%"
if errorlevel 1 goto :failed

echo.
echo [2/5] Granting Usage access (one-time)...
%ADB% shell appops set com.aurora.home GET_USAGE_STATS allow

echo.
echo [3/5] Starting OBAA Home...
%ADB% shell am start -n com.aurora.home/.HomeActivity

echo.
echo [4/5] Enabling the OBAA Home watcher...
%ADB% shell am broadcast -a com.aurora.home.ENABLE_HOME_KEY -n com.aurora.home/.HomeKeyReceiver

echo.
echo [5/5] Checking the Home watcher...
timeout /t 2 /nobreak >nul
%ADB% shell dumpsys activity services com.aurora.home | findstr /i "HomeKeyWatcherService"
%ADB% shell dumpsys appops | findstr /i /c:"Package com.aurora.home" /c:"GET_USAGE_STATS"
echo.
echo The stock launcher may flash briefly, then OBAA Home should appear.
echo Open YouTube and press Home to test. If the stock launcher remains,
echo run: adb logcat -s HomeKeyWatcherService HomeKeyReceiver
echo See docs\FIRETV_DEFAULT_HOME.md for details.
pause
exit /b 0

:failed
echo.
echo Install failed. Check that the Fire TV is awake, ADB is authorized,
echo and the device is reachable with: adb devices
pause
exit /b 1
