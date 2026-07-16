using System.Text.RegularExpressions;

namespace QuestHomeSwitcher.Core;

public sealed record AdbDevice(string Serial, string State, string Detail)
{
    public bool IsReady => string.Equals(State, "device", StringComparison.Ordinal);
}

public interface IAdbClient
{
    string ExecutablePath { get; }

    Task<CommandResult> RunAsync(
        IReadOnlyList<string> arguments,
        CancellationToken cancellationToken = default);
}

public sealed class AdbClient(string executablePath, ICommandRunner commandRunner) : IAdbClient
{
    public string ExecutablePath { get; } = executablePath ?? throw new ArgumentNullException(nameof(executablePath));

    public Task<CommandResult> RunAsync(
        IReadOnlyList<string> arguments,
        CancellationToken cancellationToken = default) =>
        commandRunner.RunAsync(ExecutablePath, arguments, cancellationToken);
}

public static class AdbLocator
{
    public static string? Find(string? explicitPath = null)
    {
        var host = PlatformDetector.Detect();
        if (!string.IsNullOrWhiteSpace(explicitPath))
        {
            return ResolveRequired(explicitPath, host.AdbExecutableName);
        }

        var comparer = OperatingSystem.IsWindows() ? StringComparer.OrdinalIgnoreCase : StringComparer.Ordinal;
        var candidates = new List<string>();
        var seen = new HashSet<string>(comparer);

        AddCandidate(candidates, seen, Environment.GetEnvironmentVariable("QHS_ADB"));
        AddCandidate(candidates, seen, Environment.GetEnvironmentVariable("ADB"));

        foreach (var sdkVariable in new[] { "ANDROID_SDK_ROOT", "ANDROID_HOME" })
        {
            var sdkRoot = Environment.GetEnvironmentVariable(sdkVariable);
            if (!string.IsNullOrWhiteSpace(sdkRoot))
            {
                AddCandidate(candidates, seen, Path.Combine(sdkRoot, "platform-tools", host.AdbExecutableName));
            }
        }

        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        if (OperatingSystem.IsWindows())
        {
            AddCandidate(candidates, seen, Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                "Android", "Sdk", "platform-tools", host.AdbExecutableName));
            foreach (var programFiles in new[]
                     {
                         Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles),
                         Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86)
                     }.Where(value => !string.IsNullOrWhiteSpace(value)))
            {
                AddCandidate(candidates, seen, Path.Combine(
                    programFiles,
                    "SideQuest", "resources", "app.asar.unpacked", "build", "platform-tools", host.AdbExecutableName));
            }
        }
        else if (OperatingSystem.IsMacOS())
        {
            AddCandidate(candidates, seen, Path.Combine(home, "Library", "Android", "sdk", "platform-tools", host.AdbExecutableName));
            AddCandidate(candidates, seen, Path.Combine(
                Path.DirectorySeparatorChar.ToString(),
                "Applications", "SideQuest.app", "Contents", "Resources", "app.asar.unpacked", "build", "platform-tools", host.AdbExecutableName));
        }
        else
        {
            AddCandidate(candidates, seen, Path.Combine(home, "Android", "Sdk", "platform-tools", host.AdbExecutableName));
            AddCandidate(candidates, seen, Path.Combine(home, "Android", "sdk", "platform-tools", host.AdbExecutableName));
        }

        var pathValue = Environment.GetEnvironmentVariable("PATH") ?? string.Empty;
        foreach (var directory in pathValue.Split(Path.PathSeparator, StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
        {
            AddCandidate(candidates, seen, Path.Combine(directory, host.AdbExecutableName));
        }

        return candidates.FirstOrDefault(File.Exists);
    }

    private static string ResolveRequired(string path, string executableName)
    {
        var expanded = Environment.ExpandEnvironmentVariables(path.Trim().Trim('"'));
        if (expanded.StartsWith('~'))
        {
            expanded = Path.Combine(
                Environment.GetFolderPath(Environment.SpecialFolder.UserProfile),
                expanded[1..].TrimStart(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar));
        }

        if (Directory.Exists(expanded))
        {
            expanded = Path.Combine(expanded, executableName);
        }

        var fullPath = Path.GetFullPath(expanded);
        if (!File.Exists(fullPath))
        {
            throw new FileNotFoundException("The requested ADB executable was not found.", fullPath);
        }

        return fullPath;
    }

    private static void AddCandidate(ICollection<string> candidates, ISet<string> seen, string? candidate)
    {
        if (string.IsNullOrWhiteSpace(candidate))
        {
            return;
        }

        try
        {
            var expanded = Environment.ExpandEnvironmentVariables(candidate.Trim().Trim('"'));
            var fullPath = Path.GetFullPath(expanded);
            if (seen.Add(fullPath))
            {
                candidates.Add(fullPath);
            }
        }
        catch
        {
            // A malformed optional candidate must not hide a later valid installation.
        }
    }
}

public sealed class AdbDeviceService(IAdbClient adbClient)
{
    private static readonly Regex DeviceRow = new(
        @"^(?<serial>\S+)\s+(?<state>device|unauthorized|offline|no permissions)\b(?<detail>.*)$",
        RegexOptions.Compiled | RegexOptions.CultureInvariant);

    public async Task<IReadOnlyList<AdbDevice>> ListAsync(CancellationToken cancellationToken = default)
    {
        var result = await adbClient.RunAsync(["devices", "-l"], cancellationToken).ConfigureAwait(false);
        if (!result.Succeeded)
        {
            throw new InvalidOperationException($"ADB could not list devices: {result.CombinedOutput}");
        }

        return ParseDeviceList(result.StandardOutput);
    }

    public async Task<AdbSession> GetReadySessionAsync(
        string? requestedSerial = null,
        CancellationToken cancellationToken = default)
    {
        var devices = await ListAsync(cancellationToken).ConfigureAwait(false);
        AdbDevice? selected;
        if (!string.IsNullOrWhiteSpace(requestedSerial))
        {
            selected = devices.SingleOrDefault(device => string.Equals(device.Serial, requestedSerial, StringComparison.Ordinal));
            if (selected is null)
            {
                throw new InvalidOperationException($"ADB device '{requestedSerial}' was not found.");
            }
        }
        else
        {
            var ready = devices.Where(device => device.IsReady).ToArray();
            if (ready.Length > 1)
            {
                throw new InvalidOperationException("More than one authorized Android device is connected. Use --serial to select the Quest.");
            }

            selected = ready.SingleOrDefault() ?? devices.SingleOrDefault();
        }

        if (selected is null)
        {
            throw new InvalidOperationException("No Quest was found. Connect it over USB and enable USB debugging.");
        }

        if (!selected.IsReady)
        {
            var guidance = selected.State switch
            {
                "unauthorized" => "Put on the Quest and approve the USB debugging prompt.",
                "offline" => "Reconnect the USB cable and restart ADB if needed.",
                "no permissions" => "Install Android udev rules and verify plugdev membership.",
                _ => "Reconnect the Quest and check USB debugging."
            };
            throw new InvalidOperationException($"Quest '{selected.Serial}' is {selected.State}. {guidance}");
        }

        var session = new AdbSession(adbClient, selected.Serial);
        await VerifyQuestIdentityAsync(session, cancellationToken).ConfigureAwait(false);
        return session;
    }

    private static async Task VerifyQuestIdentityAsync(
        AdbSession session,
        CancellationToken cancellationToken)
    {
        var manufacturerTask = session.RunAsync(
            ["shell", "getprop", "ro.product.manufacturer"],
            cancellationToken);
        var modelTask = session.RunAsync(
            ["shell", "getprop", "ro.product.model"],
            cancellationToken);
        var shellPackageTask = session.RunAsync(
            ["shell", "pm", "path", "com.oculus.vrshell"],
            cancellationToken);
        await Task.WhenAll(manufacturerTask, modelTask, shellPackageTask).ConfigureAwait(false);

        var manufacturer = (await manufacturerTask.ConfigureAwait(false)).StandardOutput.Trim();
        var model = (await modelTask.ConfigureAwait(false)).StandardOutput.Trim();
        var shellPackage = await shellPackageTask.ConfigureAwait(false);
        var vendorMatches = manufacturer.Contains("Oculus", StringComparison.OrdinalIgnoreCase) ||
                            manufacturer.Contains("Meta", StringComparison.OrdinalIgnoreCase) ||
                            model.Contains("Quest", StringComparison.OrdinalIgnoreCase);
        var shellMatches = shellPackage.Succeeded &&
                           shellPackage.StandardOutput
                               .Split(['\r', '\n'], StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries)
                               .Any(line => line.StartsWith("package:", StringComparison.Ordinal));
        if (!vendorMatches || !shellMatches)
        {
            throw new InvalidOperationException(
                $"Authorized Android device '{session.Serial}' was not verified as a Meta Quest. " +
                "Disconnect phones or select the Quest explicitly with --serial.");
        }
    }

    public static IReadOnlyList<AdbDevice> ParseDeviceList(string output)
    {
        var devices = new List<AdbDevice>();
        foreach (var rawLine in output.Split(['\r', '\n'], StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
        {
            if (rawLine.StartsWith("List of devices", StringComparison.OrdinalIgnoreCase) || rawLine.StartsWith('*'))
            {
                continue;
            }

            var match = DeviceRow.Match(rawLine);
            if (match.Success)
            {
                devices.Add(new AdbDevice(
                    match.Groups["serial"].Value,
                    match.Groups["state"].Value,
                    match.Groups["detail"].Value.Trim()));
            }
        }

        return devices;
    }
}

public sealed class AdbSession(IAdbClient adbClient, string serial)
{
    public string Serial { get; } = string.IsNullOrWhiteSpace(serial)
        ? throw new ArgumentException("A device serial is required.", nameof(serial))
        : serial;

    public Task<CommandResult> RunAsync(
        IReadOnlyList<string> arguments,
        CancellationToken cancellationToken = default)
    {
        var scoped = new List<string>(arguments.Count + 2) { "-s", Serial };
        scoped.AddRange(arguments);
        return adbClient.RunAsync(scoped, cancellationToken);
    }

    public Task<CommandResult> ShellAsync(string command, CancellationToken cancellationToken = default) =>
        RunAsync(["shell", "sh", "-c", command], cancellationToken);
}

public static class RemoteShell
{
    public static string Quote(string value)
    {
        ArgumentNullException.ThrowIfNull(value);
        return $"'{value.Replace("'", "'\\''", StringComparison.Ordinal)}'";
    }
}
