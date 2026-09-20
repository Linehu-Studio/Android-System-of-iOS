# ASI Device Setup — one-shot ADB initialization
# ------------------------------------------------
# Grants ASI every permission that ADB can grant without root, enables
# global freeform windowing, and reports device capabilities.
#
# Usage:  powershell -ExecutionPolicy Bypass -File .\setup-device.ps1
# Requires: USB debugging enabled on the phone, device connected.

$ErrorActionPreference = "Continue"
$pkg = "com.linehu.asi"

function Step($msg) { Write-Host "==> $msg" -ForegroundColor Cyan }

# --- locate adb -------------------------------------------------------------
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    $adb = (Get-Command adb -ErrorAction SilentlyContinue)?.Source
}
if (-not $adb) {
    Write-Host "[X] adb not found. Install Android platform-tools first." -ForegroundColor Red
    exit 1
}

# --- device check ------------------------------------------------------------
Step "Waiting for device..."
& $adb wait-for-device
$serial = (& $adb devices | Select-String "device$" | Select-Object -First 1).ToString().Split("`t")[0]
Write-Host "    Device: $serial"

$androidVer = (& $adb shell getprop ro.build.version.release).Trim()
$model = (& $adb shell getprop ro.product.model).Trim()
Write-Host "    Model:  $model  (Android $androidVer)"

# --- install ------------------------------------------------------------------
$apk = "$PSScriptRoot\..\app\build\outputs\apk\debug\app-debug.apk"
if (Test-Path $apk) {
    Step "Installing ASI ($apk)..."
    & $adb install -r $apk
} else {
    Write-Host "    [!] APK not found at $apk - build it first: .\gradlew.bat assembleDebug" -ForegroundColor Yellow
}

# --- runtime permissions -------------------------------------------------------
Step "Granting runtime permissions..."
& $adb shell pm grant $pkg android.permission.POST_NOTIFICATIONS 2>$null
& $adb shell pm grant $pkg android.permission.READ_MEDIA_IMAGES 2>$null
& $adb shell pm grant $pkg android.permission.READ_MEDIA_VISUAL_USER_SELECTED 2>$null
& $adb shell pm grant $pkg android.permission.READ_EXTERNAL_STORAGE 2>$null

# --- special app-ops permissions (not grantable from inside the app) ------------
Step "Granting special app-ops..."
& $adb shell appops set $pkg WRITE_SETTINGS allow
& $adb shell appops set $pkg PACKAGE_USAGE_STATS allow

# --- notification listener -------------------------------------------------------
Step "Enabling notification listener service..."
& $adb shell cmd notification allow_listener "$pkg/com.linehu.asi.service.AsiNotificationListenerService"

# --- freeform multi-window --------------------------------------------------------
Step "Enabling global freeform window support..."
& $adb shell settings put global enable_freeform_support 1

# --- capability report ---------------------------------------------------------------
Step "Device capabilities:"
$root = (& $adb shell "which su" 2>$null)
if ($root -and $root.Trim() -ne "") {
    Write-Host "    Root: AVAILABLE (su found) - Magisk module path is open" -ForegroundColor Green
} else {
    Write-Host "    Root: not available (normal)" -ForegroundColor DarkGray
}
$cutout = (& $adb shell "dumpsys window | grep -i mDisplayCutout" 2>$null)
if ($cutout) { Write-Host "    Display cutout info found (Dynamic Island can hug the camera)" }

Write-Host ""
Write-Host "Done. Reboot the phone (or restart ASI) so all grants take effect." -ForegroundColor Green
