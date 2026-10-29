param(
    [string]$Serial = ""
)

$ErrorActionPreference = "Stop"
$adbArgs = @()
if ($Serial -ne "") { $adbArgs = @("-s", $Serial) }

$apk = Join-Path $PSScriptRoot "..\OBAAHome.apk"
if (-not (Test-Path $apk)) {
    Write-Error "OBAAHome.apk not found at $apk"
}

Write-Host "`n[1/5] Installing OBAA Home..." -ForegroundColor Cyan
& adb @adbArgs install -r $apk

Write-Host "`n[2/5] Granting Usage access (one-time)..." -ForegroundColor Cyan
& adb @adbArgs shell appops set com.aurora.home GET_USAGE_STATS allow

Write-Host "`n[3/5] Starting OBAA Home..." -ForegroundColor Cyan
& adb @adbArgs shell am start -n com.aurora.home/.HomeActivity

Write-Host "`n[4/5] Enabling the OBAA Home watcher..." -ForegroundColor Cyan
& adb @adbArgs shell am broadcast -a com.aurora.home.ENABLE_HOME_KEY -n com.aurora.home/.HomeKeyReceiver

Write-Host "`n[5/5] Checking the Home watcher..." -ForegroundColor Cyan
Start-Sleep -Seconds 2
& adb @adbArgs shell dumpsys activity services com.aurora.home | Select-String "HomeKeyWatcherService"
$appOps = & adb @adbArgs shell dumpsys appops
$appOps | Select-String "Package com.aurora.home:" -Context 0,6

Write-Host "`nThe stock launcher may flash briefly, then OBAA Home should appear." -ForegroundColor Green
Write-Host "Open YouTube and press Home to test." -ForegroundColor Green
Write-Host "If the stock launcher remains, run:" -ForegroundColor Yellow
Write-Host "  adb logcat -s HomeKeyWatcherService HomeKeyReceiver" -ForegroundColor Gray
Write-Host "See docs/FIRETV_DEFAULT_HOME.md for details.`n" -ForegroundColor Gray
