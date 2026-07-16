using System.Text.RegularExpressions;

namespace QuestHomeSwitcher.Core;

public sealed record PackageStatus(
    string PackageName,
    bool Installed,
    string? VersionName,
    long? VersionCode,
    string? PackagePath);

public sealed record ShizukuStatus(
    bool Installed,
    bool RunningVerified,
    string? VersionName,
    string Detail);

public sealed record QuestStatus(
    string Serial,
    PackageStatus Switcher,
    ShizukuStatus Shizuku);

public sealed class DeviceStatusService
{
    public const string SwitcherPackage = "io.github.nikitat21.questhomeswitcher";
    public const string SwitcherActivity = "io.github.nikitat21.questhomeswitcher/.MainActivity";
    public const string ShizukuPackage = "moe.shizuku.privileged.api";

    private static readonly Regex VersionName = new(
        @"(?m)^\s*versionName=(?<value>[^\r\n]+)",
        RegexOptions.Compiled | RegexOptions.CultureInvariant);
    private static readonly Regex VersionCode = new(
        @"(?m)^\s*versionCode=(?<value>\d+)",
        RegexOptions.Compiled | RegexOptions.CultureInvariant);
    private static readonly Regex ShellUid = new(
        @"(?m)^Uid:\s+2000(?:\s|$)",
        RegexOptions.Compiled | RegexOptions.CultureInvariant);

    public async Task<QuestStatus> GetAsync(
        AdbSession session,
        CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(session);
        var switcherTask = GetPackageAsync(session, SwitcherPackage, cancellationToken);
        var shizukuTask = GetShizukuAsync(session, cancellationToken);
        await Task.WhenAll(switcherTask, shizukuTask).ConfigureAwait(false);
        return new QuestStatus(session.Serial, await switcherTask, await shizukuTask);
    }

    public async Task<PackageStatus> GetPackageAsync(
        AdbSession session,
        string packageName,
        CancellationToken cancellationToken = default)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(packageName);
        var pathResult = await session.ShellAsync(
            $"pm path {RemoteShell.Quote(packageName)}",
            cancellationToken).ConfigureAwait(false);
        var packagePath = pathResult.StandardOutput
            .Split(['\r', '\n'], StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .FirstOrDefault(line => line.StartsWith("package:", StringComparison.Ordinal));
        if (!pathResult.Succeeded || packagePath is null)
        {
            return new PackageStatus(packageName, false, null, null, null);
        }

        var dump = await session.ShellAsync(
            $"dumpsys package {RemoteShell.Quote(packageName)}",
            cancellationToken).ConfigureAwait(false);
        var versionName = dump.Succeeded ? MatchValue(VersionName, dump.StandardOutput) : null;
        var versionCodeText = dump.Succeeded ? MatchValue(VersionCode, dump.StandardOutput) : null;
        var parsedVersionCode = long.TryParse(versionCodeText, out var code) ? code : (long?)null;
        return new PackageStatus(
            packageName,
            true,
            versionName,
            parsedVersionCode,
            packagePath["package:".Length..]);
    }

    private async Task<ShizukuStatus> GetShizukuAsync(
        AdbSession session,
        CancellationToken cancellationToken)
    {
        var package = await GetPackageAsync(session, ShizukuPackage, cancellationToken).ConfigureAwait(false);
        if (!package.Installed)
        {
            return new ShizukuStatus(false, false, null, "Not installed.");
        }

        var pidResult = await session.ShellAsync("pidof shizuku_server", cancellationToken).ConfigureAwait(false);
        var pid = pidResult.StandardOutput
            .Split((char[]?)null, StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
            .FirstOrDefault(value => value.All(char.IsDigit));
        if (!pidResult.Succeeded || pid is null)
        {
            return new ShizukuStatus(true, false, package.VersionName, "Installed, but the server is not running.");
        }

        var commandLine = await session.ShellAsync(
            $"cat /proc/{pid}/cmdline",
            cancellationToken).ConfigureAwait(false);
        var processStatus = await session.ShellAsync(
            $"cat /proc/{pid}/status",
            cancellationToken).ConfigureAwait(false);
        var nameVerified = commandLine.Succeeded && commandLine.StandardOutput
            .Split('\0', StringSplitOptions.RemoveEmptyEntries)
            .Any(value => string.Equals(value, "shizuku_server", StringComparison.Ordinal));
        var uidVerified = processStatus.Succeeded && ShellUid.IsMatch(processStatus.StandardOutput);
        return nameVerified && uidVerified
            ? new ShizukuStatus(true, true, package.VersionName, $"Verified shizuku_server PID {pid}, Android shell UID 2000.")
            : new ShizukuStatus(true, false, package.VersionName, "A Shizuku-like process exists, but its name and shell UID could not both be verified.");
    }

    private static string? MatchValue(Regex regex, string input)
    {
        var match = regex.Match(input);
        return match.Success ? match.Groups["value"].Value.Trim() : null;
    }
}
