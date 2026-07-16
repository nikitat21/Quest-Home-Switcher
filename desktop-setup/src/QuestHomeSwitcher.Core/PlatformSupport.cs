using System.Runtime.InteropServices;

namespace QuestHomeSwitcher.Core;

public enum HostPlatform
{
    Windows,
    MacOS,
    Linux,
    Unsupported
}
public sealed record HostEnvironment(
    HostPlatform Platform,
    Architecture Architecture,
    string Description,
    string AdbExecutableName,
    Uri PlatformToolsDownload,
    IReadOnlyList<string> Prerequisites);

public static class PlatformDetector
{
    private static readonly Uri WindowsTools = new("https://dl.google.com/android/repository/platform-tools-latest-windows.zip");
    private static readonly Uri MacTools = new("https://dl.google.com/android/repository/platform-tools-latest-darwin.zip");
    private static readonly Uri LinuxTools = new("https://dl.google.com/android/repository/platform-tools-latest-linux.zip");

    public static HostEnvironment Detect()
    {
        var platform = OperatingSystem.IsWindows()
            ? HostPlatform.Windows
            : OperatingSystem.IsMacOS()
                ? HostPlatform.MacOS
                : OperatingSystem.IsLinux()
                    ? HostPlatform.Linux
                    : HostPlatform.Unsupported;

        return platform switch
        {
            HostPlatform.Windows => new(
                platform,
                RuntimeInformation.OSArchitecture,
                RuntimeInformation.OSDescription,
                "adb.exe",
                WindowsTools,
                ["Enable developer mode and USB debugging on the Quest.", "Approve the USB debugging prompt inside the headset."]),
            HostPlatform.MacOS => new(
                platform,
                RuntimeInformation.OSArchitecture,
                RuntimeInformation.OSDescription,
                "adb",
                MacTools,
                ["Enable developer mode and USB debugging on the Quest.", "Approve the USB debugging prompt inside the headset.", "macOS normally needs no additional ADB USB driver."]),
            HostPlatform.Linux => new(
                platform,
                RuntimeInformation.OSArchitecture,
                RuntimeInformation.OSDescription,
                "adb",
                LinuxTools,
                ["Enable developer mode and USB debugging on the Quest.", "Approve the USB debugging prompt inside the headset.", "Ensure your user is in the plugdev group where required.", "Install udev rules for Android devices (for Debian/Ubuntu: android-sdk-platform-tools-common)."]),
            _ => new(
                platform,
                RuntimeInformation.OSArchitecture,
                RuntimeInformation.OSDescription,
                "adb",
                LinuxTools,
                ["This host operating system is not supported by the Phase 1 desktop setup."])
        };
    }
}
