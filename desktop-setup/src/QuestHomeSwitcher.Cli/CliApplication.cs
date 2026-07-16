using QuestHomeSwitcher.Core;

namespace QuestHomeSwitcher.Cli;

internal sealed class CliApplication(TextWriter output, TextWriter error)
{
    public async Task<int> RunAsync(CliArguments arguments, CancellationToken cancellationToken)
    {
        if (arguments.HelpRequested || arguments.Command == "help")
        {
            WriteHelp();
            return 0;
        }

        return arguments.Command switch
        {
            "doctor" => await DoctorAsync(arguments, cancellationToken).ConfigureAwait(false),
            "status" => await StatusAsync(arguments, cancellationToken).ConfigureAwait(false),
            "install-switcher" => await InstallSwitcherAsync(arguments, cancellationToken).ConfigureAwait(false),
            "open" => await OpenAsync(arguments, cancellationToken).ConfigureAwait(false),
            "import" => await ImportAsync(arguments, cancellationToken).ConfigureAwait(false),
            _ => throw new ArgumentException($"Unknown command '{arguments.Command}'. Run 'quest-home-switcher help'.")
        };
    }

    private async Task<int> DoctorAsync(CliArguments arguments, CancellationToken cancellationToken)
    {
        RequireNoPositionals(arguments);
        RejectOptionsExcept(arguments, "--adb");
        var host = PlatformDetector.Detect();
        output.WriteLine($"Host: {host.Platform} {host.Architecture}");
        output.WriteLine($"OS:   {host.Description}");
        foreach (var prerequisite in host.Prerequisites)
        {
            output.WriteLine($"  - {prerequisite}");
        }

        var adbPath = AdbLocator.Find(arguments.GetOption("--adb"));
        if (adbPath is null)
        {
            error.WriteLine("ADB: not found");
            error.WriteLine($"Official Platform Tools: {host.PlatformToolsDownload}");
            return 2;
        }

        output.WriteLine($"ADB:  {adbPath}");
        var adb = new AdbClient(adbPath, new SystemCommandRunner());
        var version = await adb.RunAsync(["version"], cancellationToken).ConfigureAwait(false);
        if (!version.Succeeded)
        {
            error.WriteLine($"ADB version check failed: {version.CombinedOutput}");
            return 2;
        }

        output.WriteLine(version.StandardOutput.Trim());
        var devices = await new AdbDeviceService(adb).ListAsync(cancellationToken).ConfigureAwait(false);
        if (devices.Count == 0)
        {
            error.WriteLine("Quest: not detected");
            return 2;
        }

        foreach (var device in devices)
        {
            output.WriteLine($"Device: {device.Serial} [{device.State}] {device.Detail}".TrimEnd());
        }

        return devices.Any(device => device.IsReady) ? 0 : 2;
    }

    private async Task<int> StatusAsync(CliArguments arguments, CancellationToken cancellationToken)
    {
        RequireNoPositionals(arguments);
        RejectOptionsExcept(arguments, "--adb", "--serial");
        var session = await GetSessionAsync(arguments, cancellationToken).ConfigureAwait(false);
        var status = await new DeviceStatusService().GetAsync(session, cancellationToken).ConfigureAwait(false);
        output.WriteLine($"Quest:    {status.Serial} (authorized)");
        output.WriteLine(status.Switcher.Installed
            ? $"Switcher: installed {status.Switcher.VersionName ?? "unknown"} (code {status.Switcher.VersionCode?.ToString() ?? "unknown"})"
            : "Switcher: not installed");
        output.WriteLine(status.Shizuku.Installed
            ? $"Shizuku:  {(status.Shizuku.RunningVerified ? "running and verified" : "not verified running")} {status.Shizuku.VersionName ?? "unknown"}"
            : "Shizuku:  not installed");
        output.WriteLine($"           {status.Shizuku.Detail}");
        return 0;
    }

    private async Task<int> InstallSwitcherAsync(CliArguments arguments, CancellationToken cancellationToken)
    {
        RequirePositionals(arguments, exactly: 1, "install-switcher requires exactly one APK path.");
        RejectOptionsExcept(arguments, "--adb", "--serial", "--sha256");
        var session = await GetSessionAsync(arguments, cancellationToken).ConfigureAwait(false);
        var statusService = new DeviceStatusService();
        var service = new SwitcherService(statusService);
        var result = await service.InstallAsync(
            session,
            arguments.Positionals[0],
            arguments.GetOption("--sha256"),
            cancellationToken).ConfigureAwait(false);
        output.WriteLine("Switcher installation verified.");
        output.WriteLine($"SHA-256: {result.Sha256}");
        output.WriteLine($"Package:  {result.InstalledPackage.PackageName}");
        output.WriteLine($"Version:  {result.InstalledPackage.VersionName ?? "unknown"}");
        return 0;
    }

    private async Task<int> OpenAsync(CliArguments arguments, CancellationToken cancellationToken)
    {
        RequireNoPositionals(arguments);
        RejectOptionsExcept(arguments, "--adb", "--serial");
        var session = await GetSessionAsync(arguments, cancellationToken).ConfigureAwait(false);
        await new SwitcherService(new DeviceStatusService()).OpenAsync(session, cancellationToken).ConfigureAwait(false);
        output.WriteLine("Quest Home Switcher opened on the Quest.");
        return 0;
    }

    private async Task<int> ImportAsync(CliArguments arguments, CancellationToken cancellationToken)
    {
        if (arguments.Positionals.Count == 0)
        {
            throw new ArgumentException("import requires at least one Home APK path.");
        }

        RejectOptionsExcept(arguments, "--adb", "--serial", "--name");
        var requestedName = arguments.GetOption("--name");
        if (requestedName is not null && arguments.Positionals.Count != 1)
        {
            throw new ArgumentException("--name can only be used when importing one APK.");
        }

        var session = await GetSessionAsync(arguments, cancellationToken).ConfigureAwait(false);
        var importer = new HomeImportService(new HomeApkValidator());
        var failures = 0;
        foreach (var path in arguments.Positionals)
        {
            try
            {
                var result = await importer.ImportAsync(
                    session,
                    path,
                    requestedName,
                    cancellationToken).ConfigureAwait(false);
                output.WriteLine(result.Disposition == HomeImportDisposition.Uploaded
                    ? $"IMPORTED        {Path.GetFileName(path)} -> {result.RemotePath}"
                    : $"ALREADY PRESENT {Path.GetFileName(path)} -> {result.RemotePath}");
                output.WriteLine($"  SHA-256 {result.FileSha256}");
            }
            catch (OperationCanceledException)
            {
                throw;
            }
            catch (Exception exception)
            {
                failures++;
                error.WriteLine($"FAILED          {Path.GetFileName(path)}: {exception.Message}");
            }
        }

        return failures == 0 ? 0 : 1;
    }

    private static async Task<AdbSession> GetSessionAsync(
        CliArguments arguments,
        CancellationToken cancellationToken)
    {
        var adbPath = AdbLocator.Find(arguments.GetOption("--adb"));
        if (adbPath is null)
        {
            throw new FileNotFoundException(
                $"ADB was not found. Install Google Platform Tools from {PlatformDetector.Detect().PlatformToolsDownload} or pass --adb PATH.");
        }

        var adb = new AdbClient(adbPath, new SystemCommandRunner());
        return await new AdbDeviceService(adb)
            .GetReadySessionAsync(arguments.GetOption("--serial"), cancellationToken)
            .ConfigureAwait(false);
    }

    private static void RequireNoPositionals(CliArguments arguments)
    {
        if (arguments.Positionals.Count != 0)
        {
            throw new ArgumentException($"Command '{arguments.Command}' does not accept positional arguments.");
        }
    }

    private static void RequirePositionals(CliArguments arguments, int exactly, string message)
    {
        if (arguments.Positionals.Count != exactly)
        {
            throw new ArgumentException(message);
        }
    }

    private static void RejectOptionsExcept(CliArguments arguments, params string[] allowed)
    {
        var allowedSet = new HashSet<string>(allowed, StringComparer.Ordinal);
        var rejected = arguments.Options.Keys.FirstOrDefault(option => !allowedSet.Contains(option));
        if (rejected is not null)
        {
            throw new ArgumentException($"Option '{rejected}' is not valid for command '{arguments.Command}'.");
        }
    }

    private void WriteHelp()
    {
        output.WriteLine("Quest Home Switcher cross-platform setup CLI (Phase 1)");
        output.WriteLine();
        output.WriteLine("Usage:");
        output.WriteLine("  quest-home-switcher doctor [--adb PATH]");
        output.WriteLine("  quest-home-switcher status [--adb PATH] [--serial SERIAL]");
        output.WriteLine("  quest-home-switcher install-switcher SWITCHER.apk [--sha256 HASH] [--adb PATH] [--serial SERIAL]");
        output.WriteLine("  quest-home-switcher open [--adb PATH] [--serial SERIAL]");
        output.WriteLine("  quest-home-switcher import HOME.apk [MORE.apk ...] [--name NAME] [--adb PATH] [--serial SERIAL]");
        output.WriteLine();
        output.WriteLine("Safety:");
        output.WriteLine("  - The pinned v1.8 Switcher APK is accepted automatically; later official payloads require an explicit SHA-256.");
        output.WriteLine("  - Signature conflicts never trigger an automatic uninstall.");
        output.WriteLine("  - Home imports use verified temporary uploads and no-clobber finalization.");
        output.WriteLine("  - Existing personal Home APKs are never deleted or overwritten.");
    }
}
