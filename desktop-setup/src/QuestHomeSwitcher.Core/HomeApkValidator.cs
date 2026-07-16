using System.IO.Compression;
using System.Buffers;
using System.Security.Cryptography;

namespace QuestHomeSwitcher.Core;

public sealed record HomeApkInfo(
    string Path,
    string FileName,
    long Size,
    string FileSha256,
    string SceneSha256);

public sealed class HomeApkValidator
{
    public const string HomePackageIdentifier = "com.meta.shell.env.footprint.haven2025";
    private const int MaximumManifestBytes = 16 * 1024 * 1024;

    public async Task<HomeApkInfo> ValidateAsync(string path, CancellationToken cancellationToken = default)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(path);
        var fullPath = System.IO.Path.GetFullPath(path);
        if (!File.Exists(fullPath))
        {
            throw new FileNotFoundException("The Home APK was not found.", fullPath);
        }

        if (!string.Equals(System.IO.Path.GetExtension(fullPath), ".apk", StringComparison.OrdinalIgnoreCase))
        {
            throw new InvalidDataException("A Home import must be an .apk file.");
        }

        string sceneHash;
        try
        {
            using var archive = ZipFile.OpenRead(fullPath);
            var sceneEntries = archive.Entries
                .Where(entry => string.Equals(entry.FullName, "assets/scene.zip", StringComparison.Ordinal))
                .ToArray();
            if (sceneEntries.Length != 1 || sceneEntries[0].Length <= 0)
            {
                throw new InvalidDataException("The APK must contain exactly one non-empty assets/scene.zip entry.");
            }

            var manifests = archive.Entries
                .Where(entry => string.Equals(entry.FullName, "AndroidManifest.xml", StringComparison.Ordinal))
                .ToArray();
            if (manifests.Length != 1 || manifests[0].Length <= 0 || manifests[0].Length > MaximumManifestBytes)
            {
                throw new InvalidDataException("The APK contains no readable AndroidManifest.xml.");
            }

            var manifestBytes = await ReadEntryAsync(manifests[0], cancellationToken).ConfigureAwait(false);
            var packageName = AndroidManifestPackageReader.ReadPackageName(manifestBytes);
            if (!string.Equals(packageName, HomePackageIdentifier, StringComparison.Ordinal))
            {
                throw new InvalidDataException("The APK is not a compatible NoRoot-spoof Quest Home.");
            }

            sceneHash = await ValidateSceneZipAndHashAsync(sceneEntries[0], cancellationToken).ConfigureAwait(false);
        }
        catch (InvalidDataException)
        {
            throw;
        }
        catch (Exception exception) when (exception is IOException or UnauthorizedAccessException)
        {
            throw new InvalidDataException("The Home APK could not be read safely.", exception);
        }

        var fileInfo = new FileInfo(fullPath);
        var fileHash = await FileHashing.Sha256Async(fullPath, cancellationToken).ConfigureAwait(false);
        return new HomeApkInfo(fullPath, fileInfo.Name, fileInfo.Length, fileHash, sceneHash);
    }

    private static async Task<byte[]> ReadEntryAsync(
        ZipArchiveEntry entry,
        CancellationToken cancellationToken)
    {
        await using var stream = entry.Open();
        using var memory = new MemoryStream((int)entry.Length);
        await stream.CopyToAsync(memory, cancellationToken).ConfigureAwait(false);
        return memory.ToArray();
    }

    private static async Task<string> ValidateSceneZipAndHashAsync(
        ZipArchiveEntry sceneEntry,
        CancellationToken cancellationToken)
    {
        var validationRoot = Path.Combine(Path.GetTempPath(), "QuestHomeSwitcher", "scene-validation");
        Directory.CreateDirectory(validationRoot);
        var temporaryPath = Path.Combine(validationRoot, $"{Guid.NewGuid():N}.scene.zip");
        var buffer = ArrayPool<byte>.Shared.Rent(128 * 1024);
        try
        {
            using var hash = IncrementalHash.CreateHash(HashAlgorithmName.SHA256);
            await using (var input = sceneEntry.Open())
            await using (var output = new FileStream(
                             temporaryPath,
                             FileMode.CreateNew,
                             FileAccess.Write,
                             FileShare.None,
                             bufferSize: buffer.Length,
                             FileOptions.Asynchronous | FileOptions.SequentialScan))
            {
                while (true)
                {
                    var read = await input.ReadAsync(buffer, cancellationToken).ConfigureAwait(false);
                    if (read == 0)
                    {
                        break;
                    }

                    hash.AppendData(buffer, 0, read);
                    await output.WriteAsync(buffer.AsMemory(0, read), cancellationToken).ConfigureAwait(false);
                }

                await output.FlushAsync(cancellationToken).ConfigureAwait(false);
            }

            using var sceneArchive = ZipFile.OpenRead(temporaryPath);
            if (!sceneArchive.Entries.Any(entry =>
                    !entry.FullName.EndsWith("/", StringComparison.Ordinal) && entry.Length > 0))
            {
                throw new InvalidDataException("assets/scene.zip contains no non-empty scene files.");
            }

            return Convert.ToHexString(hash.GetHashAndReset());
        }
        catch (InvalidDataException)
        {
            throw;
        }
        catch (Exception exception) when (exception is IOException or UnauthorizedAccessException)
        {
            throw new InvalidDataException("assets/scene.zip is not a readable ZIP archive.", exception);
        }
        finally
        {
            ArrayPool<byte>.Shared.Return(buffer, clearArray: true);
            try
            {
                File.Delete(temporaryPath);
            }
            catch
            {
                // Validation cleanup is best effort and must not hide the original result.
            }
        }
    }
}
