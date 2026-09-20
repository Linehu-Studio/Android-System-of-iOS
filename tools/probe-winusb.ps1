# Probe the Huawei ADB WinUSB interface: is it registered, and can it be opened?
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
using System.Text;
public class UsbProbe4 {
    [DllImport("setupapi.dll", CharSet = CharSet.Auto)]
    public static extern IntPtr SetupDiGetClassDevs(ref Guid ClassGuid, IntPtr Enumerator, IntPtr hWndParent, uint Flags);
    [DllImport("setupapi.dll", CharSet = CharSet.Auto, SetLastError = true)]
    public static extern bool SetupDiEnumDeviceInterfaces(IntPtr hDevInfo, IntPtr devInfoData, ref Guid interfaceClassGuid, uint memberIndex, ref SP_DEVICE_INTERFACE_DATA data);
    [DllImport("setupapi.dll", CharSet = CharSet.Auto, SetLastError = true)]
    public static extern bool SetupDiGetDeviceInterfaceDetail(IntPtr hDevInfo, ref SP_DEVICE_INTERFACE_DATA data, IntPtr detailData, uint detailSize, ref uint requiredSize, IntPtr deviceInfoData);
    [DllImport("kernel32.dll", CharSet = CharSet.Auto, SetLastError = true)]
    public static extern IntPtr CreateFile(string fn, uint access, uint share, IntPtr sec, uint disp, uint flags, IntPtr template);
    [DllImport("kernel32.dll")] public static extern bool CloseHandle(IntPtr h);
    [DllImport("setupapi.dll")] public static extern bool SetupDiDestroyDeviceInfoList(IntPtr h);
    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Auto)]
    public struct SP_DEVICE_INTERFACE_DATA { public int cbSize; public Guid InterfaceClassGuid; public int Flags; public IntPtr Reserved; }
    public static string Probe() {
        var sb = new StringBuilder();
        Guid g = new Guid("88bae032-5a81-49f0-bc3d-a4ff138216d6");
        IntPtr h = SetupDiGetClassDevs(ref g, IntPtr.Zero, IntPtr.Zero, 0x12);
        var data = new SP_DEVICE_INTERFACE_DATA(); data.cbSize = Marshal.SizeOf(data);
        uint i = 0;
        int total = 0, huawei = 0;
        while (SetupDiEnumDeviceInterfaces(h, IntPtr.Zero, ref g, i, ref data)) {
            uint need = 0;
            SetupDiGetDeviceInterfaceDetail(h, ref data, IntPtr.Zero, 0, ref need, IntPtr.Zero);
            IntPtr buf = Marshal.AllocHGlobal((int)need);
            Marshal.WriteInt32(buf, IntPtr.Size == 8 ? 8 : 6);
            string path = "";
            if (SetupDiGetDeviceInterfaceDetail(h, ref data, buf, need, ref need, IntPtr.Zero)) {
                path = Marshal.PtrToStringUni(IntPtr.Add(buf, IntPtr.Size == 8 ? 8 : 6));
            }
            Marshal.FreeHGlobal(buf);
            total++;
            if (path.ToLower().Contains("vid_12d1")) {
                huawei++;
                IntPtr fh = CreateFile(path, 0x80000000 | 0x40000000, 3, IntPtr.Zero, 3, 0, IntPtr.Zero);
                int err = Marshal.GetLastWin32Error();
                if (fh != (IntPtr)(-1)) { sb.AppendLine("HUAWEI-INTERFACE OPEN-OK: " + path); CloseHandle(fh); }
                else { sb.AppendLine("HUAWEI-INTERFACE OPEN-FAIL err=" + err + ": " + path); }
            }
            i++;
        }
        SetupDiDestroyDeviceInfoList(h);
        sb.AppendLine("total-interfaces=" + total + " huawei=" + huawei);
        return sb.ToString();
    }
}
"@
[UsbProbe4]::Probe()
