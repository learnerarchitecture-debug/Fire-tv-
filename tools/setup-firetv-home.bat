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
echo [1/4] Installing OBAA Home...
%ADB% install -r "%APK%"
if errorlevel 1 goto :failed

echo.
echo [2/4] Granting one-time setup permission...
%ADB% shell pm grant com.aurora.home android.permission.WRITE_SECURE_SETTINGS

echo.
echo [3/4] Enabling the OBAA Home key service...
%ADB% shell am broadcast -a com.aurora.home.ENABLE_HOME_KEY -n com.aurora.home/.HomeKeyReceiver

echo.
echo [4/4] Checking state...
timeout /t 2 /nobreak >nul
echo enabled_accessibility_services:
%ADB% shell settings get secure enabled_accessibility_services
echo accessibility_enabled:
%ADB% shell settings get secure accessibility_enabled
echo.
echo If both values look right, open YouTube and press Home.
echo If the service is blank or still opens the stock launcher, use
echo Fire TV Settings ^> Accessibility ^> OBAA Home key, or see docs\FIRETV_DEFAULT_HOME.md.
pause
exit /b 0

:failed
echo.
echo Install failed. Check that the Fire TV is awake, ADB is authorized,
echo and the device is reachable with: adb devices
pause
exit /b 1
