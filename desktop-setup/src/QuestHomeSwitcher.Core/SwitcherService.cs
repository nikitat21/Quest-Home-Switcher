namespace QuestHomeSwitcher.Core;

public sealed record SwitcherInstallResult(
    string ApkPath,
    string Sha256,
    PackageStatus InstalledPackage);

public sealed class SwitcherService(DeviceStatusService statusService)
{
    public const string PinnedVersion18Sha256 = "CFF3676D81209A2BC30C56A4587ECFC04789F6F0AF18733D84ADE04917362A50";

    private static readonly HashSet<string> TrustedPayloadHashes = new(StringComparer.OrdinalIgnoreCase)
    {
        PinnedVersion18Sha256
    };

    public async Task<SwitcherInstallResult> InstallAsync(
        AdbSession session,
        string apkPath,
        string? explicitlyTrustedSha256 = null,
        CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(session);
        ArgumentException.ThrowIfNullOrWhiteSpace(apkPath);
        var fullPath = Path.GetFullPath(apkPath);
        if (!File.Exists(fullPath))
        {
            throw new FileNotFoundException("The Quest Home Switcher APK was not found.", fullPath);
        }

        if (!string.Equals(Path.GetExtension(fullPath), ".apk", StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidDataException("The Switcher payload must be an .apk file.");
        }

        var actualHash = await FileHashing.Sha256Async(fullPath, cancellationToken).ConfigureAwait(false);
        if (explicitlyTrustedSha256 is null)
        {
            if (!TrustedPayloadHashes.Contains(actualHash))
            {
                throw new InvalidDataException(
                    $"Switcher APK SHA-256 {actualHash} is not pinned by this build. " +
                    "Verify the official release digest and pass it explicitly with --sha256.");
            }
        }
        else
        {
            var expectedHash = FileHashing.NormalizeSha256(explicitlyTrustedSha256);
            if (!string.Equals(actualHash, expectedHash, StringComparison.OrdinalIgnoreCase))
            {
                throw new InvalidDataException($"Switcher APK SHA-256 mismatch. Expected {expectedHash}, found {actualHash}.");
            }
        }

        var install = await session.RunAsync(["install", "-r", fullPath], cancellationToken).ConfigureAwait(false);
        if (!install.Succeeded || !install.CombinedOutput.Contains("Success", StringComparison.OrdinalIgnoreCase))
        {
            if (install.CombinedOutput.Contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE", StringComparison.OrdinalIgnoreCase))
            {
                throw new InvalidOperationException(
                    "Android rejected the update because the installed Switcher uses another signing key. " +
                    "Phase 1 never uninstalls apps automatically, so the existing installation was left unchanged.");
            }

            throw new InvalidOperationException($"Android rejected the verified Switcher APK: {install.CombinedOutput}");
        }

        var package = await statusService.GetPackageAsync(
            session,
            DeviceStatusService.SwitcherPackage,
            cancellationToken).ConfigureAwait(false);
        if (!package.Installed)
        {
            throw new InvalidOperationException("ADB reported installation success, but the expected Switcher package was not found afterward.");
        }

        return new SwitcherInstallResult(fullPath, actualHash, package);
    }

    public async Task OpenAsync(AdbSession session, CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(session);
        var result = await session.ShellAsync(
            $"am start -n {RemoteShell.Quote(DeviceStatusService.SwitcherActivity)}",
            cancellationToken).ConfigureAwait(false);
        if (!result.Succeeded ||
            result.CombinedOutput.Contains("Error", StringComparison.OrdinalIgnoreCase) ||
            result.CombinedOutput.Contains("Exception", StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidOperationException($"Quest Home Switcher could not be opened: {result.CombinedOutput}");
        }
    }
}
