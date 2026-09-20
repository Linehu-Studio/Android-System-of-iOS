# Fix: Huawei ADB interface (WinUSB) registers no device-interface GUID,
# so adb cannot discover the device. Add the WinUSB device interface GUID
# and restart the interface device. Run elevated.
$ErrorActionPreference = "Stop"

$key = "HKLM:\SYSTEM\CurrentControlSet\Enum\USB\VID_12D1&PID_107D&MI_02\6&426E795&0&0002\Device Parameters"

# WinUSB device interface GUID — what adb enumerates.
New-ItemProperty -Path $key -Name "DeviceInterfaceGUIDs" -PropertyType MultiString `
    -Value @("{88bae032-5a81-49f0-bc3d-a4ff138216d6}") -Force | Out-Null
New-ItemProperty -Path $key -Name "DeviceInterfaceGUID" -PropertyType String `
    -Value "{88bae032-5a81-49f0-bc3d-a4ff138216d6}" -Force | Out-Null

Write-Output "GUID values written."

# Restart the ADB interface so WinUSB re-registers with the GUID.
pnputil /restart-device "USB\VID_12D1&PID_107D&MI_02\6&426E795&0&0002"
Write-Output "Device restarted. Done."
