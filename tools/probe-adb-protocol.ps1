# Minimal ADB protocol handshake over WinUSB — tests whether the phone's
# adbd daemon is actually alive, bypassing the adb binary entirely.
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
using System.Text;
public class AdbHandshake {
    [DllImport("kernel32.dll", CharSet = CharSet.Auto, SetLastError = true)]
    static extern IntPtr CreateFile(string fn, uint access, uint share, IntPtr sec, uint disp, uint flags, IntPtr template);
    [DllImport("kernel32.dll")] static extern bool CloseHandle(IntPtr h);
    [DllImport("winusb.dll", SetLastError = true)]
    static extern bool WinUsb_Initialize(IntPtr deviceHandle, out IntPtr interfaceHandle);
    [DllImport("winusb.dll", SetLastError = true)]
    static extern bool WinUsb_QueryInterfaceSettings(IntPtr interfaceHandle, byte alternateInterfaceNumber, ref USB_INTERFACE_DESCRIPTOR ifaceDescriptor);
    [DllImport("winusb.dll", SetLastError = true)]
    static extern bool WinUsb_QueryPipe(IntPtr interfaceHandle, byte alternateInterfaceNumber, byte pipeIndex, ref WINUSB_PIPE_INFORMATION pipeInfo);
    [DllImport("winusb.dll", SetLastError = true)]
    static extern bool WinUsb_WritePipe(IntPtr interfaceHandle, byte pipeId, byte[] buffer, uint length, out uint lengthWritten, IntPtr overlapped);
    [DllImport("winusb.dll", SetLastError = true)]
    static extern bool WinUsb_ReadPipe(IntPtr interfaceHandle, byte pipeId, byte[] buffer, uint length, out uint lengthRead, IntPtr overlapped);

    [StructLayout(LayoutKind.Sequential)]
    public struct USB_INTERFACE_DESCRIPTOR { public byte bLength, bDescriptorType; public byte bInterfaceNumber, bAlternateSetting; public byte bNumEndpoints; public byte bInterfaceClass, bInterfaceSubClass, bInterfaceProtocol; public byte iInterface; }
    [StructLayout(LayoutKind.Sequential)]
    public struct WINUSB_PIPE_INFORMATION { public int PipeType; public byte PipeId; public short MaximumPacketSize; public byte Interval; }

    public static string Test(string path) {
        var sb = new StringBuilder();
        IntPtr h = CreateFile(path, 0x80000000u | 0x40000000u, 3u, IntPtr.Zero, 3u, 0x40000000u, IntPtr.Zero);
        if (h == (IntPtr)(-1)) return "CreateFile FAIL err=" + Marshal.GetLastWin32Error();
        IntPtr usb;
        if (!WinUsb_Initialize(h, out usb)) { CloseHandle(h); return "WinUsb_Initialize FAIL err=" + Marshal.GetLastWin32Error(); }
        sb.AppendLine("WinUSB initialized OK");

        // Find bulk in/out pipes.
        byte bulkIn = 0, bulkOut = 0;
        for (byte i = 0; i < 16; i++) {
            var pi = new WINUSB_PIPE_INFORMATION();
            if (!WinUsb_QueryPipe(usb, 0, i, ref pi)) break;
            sb.AppendLine("pipe id=0x" + pi.PipeId.ToString("X2") + " type=" + pi.PipeType + " maxpkt=" + pi.MaximumPacketSize);
            if (pi.PipeType == 2) { // UsbPipeType.Bulk
                if ((pi.PipeId & 0x80) != 0) bulkIn = pi.PipeId; else bulkOut = pi.PipeId;
            }
        }
        if (bulkIn == 0 || bulkOut == 0) { sb.AppendLine("no bulk pipes found"); return sb.ToString(); }

        // Build ADB CNXN packet: command | arg0 | arg1 | data_length | data_crc | magic
        string sysId = "host::features=shell_raw,v2,cmd,stat_v2";
        byte[] data = Encoding.ASCII.GetBytes(sysId);
        byte[] pkt = new byte[24 + data.Length];
        Array.Copy(Encoding.ASCII.GetBytes("CNXN"), 0, pkt, 0, 4);
        Array.Copy(BitConverter.GetBytes(0x01000000), 0, pkt, 4, 4);   // A_VERSION
        Array.Copy(BitConverter.GetBytes(4096), 0, pkt, 8, 4);          // MAX_PAYLOAD
        Array.Copy(BitConverter.GetBytes(data.Length), 0, pkt, 12, 4);
        Array.Copy(BitConverter.GetBytes(0), 0, pkt, 16, 4);            // checksum (0 ok)
        Array.Copy(BitConverter.GetBytes(0x01000000 ^ 0xFFFFFFFF), 0, pkt, 20, 4); // magic
        Array.Copy(data, 0, pkt, 24, data.Length);

        uint written;
        if (!WinUsb_WritePipe(usb, bulkOut, pkt, (uint)pkt.Length, out written, IntPtr.Zero)) {
            sb.AppendLine("WRITE FAIL err=" + Marshal.GetLastWin32Error());
            return sb.ToString();
        }
        sb.AppendLine("CNXN sent (" + written + " bytes)");

        var resp = new byte[4096];
        uint read;
        if (!WinUsb_ReadPipe(usb, bulkIn, resp, (uint)resp.Length, out read, IntPtr.Zero)) {
            sb.AppendLine("READ FAIL err=" + Marshal.GetLastWin32Error());
        } else if (read >= 4) {
            string cmd = Encoding.ASCII.GetString(resp, 0, 4);
            sb.AppendLine("RESPONSE: " + cmd + " (" + read + " bytes)");
            if (read > 24) sb.AppendLine("payload: " + Encoding.ASCII.GetString(resp, 24, (int)Math.Min(read - 24, 100)));
        } else {
            sb.AppendLine("empty read");
        }
        return sb.ToString();
    }
}
"@
[UsbProbe.AdbHandshake]::Test("\\?\usb#vid_12d1&pid_107d&mi_02#6&426e795&0&0002#{88bae032-5a81-49f0-bc3d-a4ff138216d6}") 2>$null
if (-not $?) { [AdbHandshake]::Test("\\?\usb#vid_12d1&pid_107d&mi_02#6&426e795&0&0002#{88bae032-5a81-49f0-bc3d-a4ff138216d6}") }
