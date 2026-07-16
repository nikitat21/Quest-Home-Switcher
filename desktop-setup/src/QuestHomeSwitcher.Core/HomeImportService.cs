using System.Globalization;
using System.Text.RegularExpressions;

namespace QuestHomeSwitcher.Core;

public enum HomeImportDisposition
{
    Uploaded,
    AlreadyPresent
}

public sealed record HomeImportResult(
    HomeImportDisposition Disposition,
    string LocalPath,
    string RemotePath,
    string FileSha256,
    string SceneSha256);

public sealed class HomeImportService(HomeApkValidator validator)
{
    public const string RemoteImportDirectory = "/sdcard/Download/Quest Homes";
    private const int MaximumCollisionAttempts = 10_000;
    private static readonly Regex Sha256Line = new(
        @"(?im)^\s*(?<hash>[0-9a-f]{64})(?:\s|$)",
        RegexOptions.Compiled | RegexOptions.CultureInvariant);

    public async Task<HomeImportResult> ImportAsync(
        AdbSession session,
        string localPath,
        string? requestedName = null,
        CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(session);
        var home = await validator.ValidateAsync(localPath, cancellationToken).ConfigureAwait(false);
        var safeName = HomeFileNamePolicy.Create(home.Path, requestedName);
        await EnsureDirectoryAsync(session, cancellationToken).ConfigureAwait(false);

        var temporaryPath = $"{RemoteImportDirectory}/.qhs-upload-{Guid.NewGuid():N}.part";
        // ADB can create a partial remote file even when push returns failure or is cancelled.
        // The path is uniquely owned by this operation, so cleanup is always safe to attempt.
        var temporaryMayExist = true;
        try
        {
            var push = await session.RunAsync(["push", home.Path, temporaryPath], cancellationToken).ConfigureAwait(false);
            if (!push.Succeeded)
            {
                throw new IOException($"ADB upload failed: {push.CombinedOutput}");
            }

            var temporarySize = await GetSizeAsync(session, temporaryPath, cancellationToken).ConfigureAwait(false);
            if (temporarySize != home.Size)
            {
                throw new IOException($"ADB upload size mismatch. Local: {home.Size}, Quest: {temporarySize?.ToString(CultureInfo.InvariantCulture) ?? "unknown"}.");
            }

            var temporaryHash = await GetSha256Async(session, temporaryPath, cancellationToken).ConfigureAwait(false);
            if (!string.Equals(temporaryHash, home.FileSha256, StringComparison.OrdinalIgnoreCase))
            {
                throw new IOException("ADB upload SHA-256 verification failed. The temporary upload will be removed.");
            }

            for (var suffix = 1; suffix <= MaximumCollisionAttempts; suffix++)
            {
                cancellationToken.ThrowIfCancellationRequested();
                var candidateName = HomeFileNamePolicy.AddSuffix(safeName, suffix);
                var candidatePath = $"{RemoteImportDirectory}/{candidateName}";
                if (await ExistsAsync(session, candidatePath, cancellationToken).ConfigureAwait(false))
                {
                    var existingHash = await GetSha256Async(session, candidatePath, cancellationToken).ConfigureAwait(false);
                    if (string.Equals(existingHash, home.FileSha256, StringComparison.OrdinalIgnoreCase))
                    {
                        return new HomeImportResult(
                            HomeImportDisposition.AlreadyPresent,
                            home.Path,
                            candidatePath,
                            home.FileSha256,
                            home.SceneSha256);
                    }

                    continue;
                }

                // mv -n is the no-clobber boundary. If another writer creates the target
                // after our existence check, the unique temporary upload remains in place.
                var move = await session.ShellAsync(
                    $"mv -n {RemoteShell.Quote(temporaryPath)} {RemoteShell.Quote(candidatePath)}",
                    cancellationToken).ConfigureAwait(false);
                if (!move.Succeeded)
                {
                    throw new IOException($"Quest could not finalize the no-clobber upload: {move.CombinedOutput}");
                }

                if (await ExistsAsync(session, temporaryPath, cancellationToken).ConfigureAwait(false))
                {
                    // A collision won the race. Keep looking without touching its file.
                    continue;
                }

                temporaryMayExist = false;
                var finalHash = await GetSha256Async(session, candidatePath, cancellationToken).ConfigureAwait(false);
                if (!string.Equals(finalHash, home.FileSha256, StringComparison.OrdinalIgnoreCase))
                {
                    throw new IOException("The finalized Quest file failed SHA-256 verification. It was left untouched for manual inspection.");
                }

                return new HomeImportResult(
                    HomeImportDisposition.Uploaded,
                    home.Path,
                    candidatePath,
                    home.FileSha256,
                    home.SceneSha256);
            }

            throw new IOException("No unused safe Home filename was available on the Quest.");
        }
        finally
        {
            if (temporaryMayExist)
            {
                await RemoveOwnedTemporaryFileAsync(session, temporaryPath).ConfigureAwait(false);
            }
        }
    }

    private static async Task EnsureDirectoryAsync(AdbSession session, CancellationToken cancellationToken)
    {
        var result = await session.ShellAsync(
            $"mkdir -p {RemoteShell.Quote(RemoteImportDirectory)}",
            cancellationToken).ConfigureAwait(false);
        if (!result.Succeeded)
        {
            throw new IOException($"Quest Home import directory could not be created: {result.CombinedOutput}");
        }
    }

    private static async Task<bool> ExistsAsync(
        AdbSession session,
        string remotePath,
        CancellationToken cancellationToken)
    {
        var result = await session.ShellAsync(
            $"test -e {RemoteShell.Quote(remotePath)}",
            cancellationToken).ConfigureAwait(false);
        return result.ExitCode switch
        {
            0 => true,
            1 => false,
            _ => throw new IOException($"Quest file existence check failed safely: {result.CombinedOutput}")
        };
    }

    private static async Task<string?> GetSha256Async(
        AdbSession session,
        string remotePath,
        CancellationToken cancellationToken)
    {
        foreach (var command in new[]
                 {
                     $"sha256sum {RemoteShell.Quote(remotePath)}",
                     $"toybox sha256sum {RemoteShell.Quote(remotePath)}"
                 })
        {
            var result = await session.ShellAsync(command, cancellationToken).ConfigureAwait(false);
            if (!result.Succeeded)
            {
                continue;
            }

            var match = Sha256Line.Match(result.StandardOutput);
            if (match.Success)
            {
                return match.Groups["hash"].Value.ToUpperInvariant();
            }
        }

        return null;
    }

    private static async Task<long?> GetSizeAsync(
        AdbSession session,
        string remotePath,
        CancellationToken cancellationToken)
    {
        var result = await session.ShellAsync(
            $"stat -c %s {RemoteShell.Quote(remotePath)}",
            cancellationToken).ConfigureAwait(false);
        return result.Succeeded && long.TryParse(
            result.StandardOutput.Trim(),
            NumberStyles.None,
            CultureInfo.InvariantCulture,
            out var size)
            ? size
            : null;
    }

    private static async Task RemoveOwnedTemporaryFileAsync(AdbSession session, string temporaryPath)
    {
        var fileName = Path.GetFileName(temporaryPath);
        if (!temporaryPath.StartsWith(RemoteImportDirectory + "/", StringComparison.Ordinal) ||
            !fileName.StartsWith(".qhs-upload-", StringComparison.Ordinal) ||
            !fileName.EndsWith(".part", StringComparison.Ordinal))
        {
            throw new InvalidOperationException("Refused to remove a remote path outside the owned temporary-file boundary.");
        }

        try
        {
            await session.ShellAsync($"rm -f {RemoteShell.Quote(temporaryPath)}").ConfigureAwait(false);
        }
        catch
        {
            // Cleanup is best effort and may not hide the original safe failure.
        }
    }
}
