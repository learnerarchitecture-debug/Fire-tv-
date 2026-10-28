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

Write-Host "`n[1/4] Installing OBAA Home..." -ForegroundColor Cyan
& adb @adbArgs install -r $apk

Write-Host "`n[2/4] Granting one-time setup permission..." -ForegroundColor Cyan
& adb @adbArgs shell pm grant com.aurora.home android.permission.WRITE_SECURE_SETTINGS

Write-Host "`n[3/4] Enabling the OBAA Home key service..." -ForegroundColor Cyan
& adb @adbArgs shell am broadcast -a com.aurora.home.ENABLE_HOME_KEY -n com.aurora.home/.HomeKeyReceiver

Write-Host "`n[4/4] Checking state..." -ForegroundColor Cyan
Start-Sleep -Seconds 2
Write-Host "enabled_accessibility_services:" -ForegroundColor Yellow
& adb @adbArgs shell settings get secure enabled_accessibility_services
Write-Host "accessibility_enabled:" -ForegroundColor Yellow
& adb @adbArgs shell settings get secure accessibility_enabled

Write-Host "`nIf both values look right, open YouTube and press Home." -ForegroundColor Green
Write-Host "If the service is blank, use Fire TV Settings > Accessibility > OBAA Home key." -ForegroundColor Yellow
Write-Host "See docs/FIRETV_DEFAULT_HOME.md for the fallback and recovery steps.`n" -ForegroundColor Gray
