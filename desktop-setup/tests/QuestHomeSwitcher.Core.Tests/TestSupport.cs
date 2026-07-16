using System.IO.Compression;
using System.Security.Cryptography;
using System.Text;
using System.Text.RegularExpressions;
using QuestHomeSwitcher.Core;

namespace QuestHomeSwitcher.Core.Tests;

internal static class AssertEx
{
    public static void True(bool condition, string message)
    {
        if (!condition)
        {
            throw new InvalidOperationException(message);
        }
    }

    public static void Equal<T>(T expected, T actual, string message)
    {
        if (!EqualityComparer<T>.Default.Equals(expected, actual))
        {
            throw new InvalidOperationException($"{message} Expected: {expected}; actual: {actual}.");
        }
    }

    public static async Task ThrowsAsync<TException>(Func<Task> action, string message)
        where TException : Exception
    {
        try
        {
            await action().ConfigureAwait(false);
        }
        catch (TException)
        {
            return;
        }

        throw new InvalidOperationException(message);
    }
}

internal sealed class TestWorkspace : IDisposable
{
    public TestWorkspace()
    {
        Root = Path.Combine(Path.GetTempPath(), "QuestHomeSwitcherCoreTests", Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(Root);
    }

    public string Root { get; }

    public string CreateHomeApk(string name, bool compatible = true, bool smuggleCompatibleMarker = false)
    {
        var path = Path.Combine(Root, name);
        using var file = new FileStream(path, FileMode.CreateNew, FileAccess.Write, FileShare.None);
        using var archive = new ZipArchive(file, ZipArchiveMode.Create);
        var manifest = archive.CreateEntry("AndroidManifest.xml", CompressionLevel.NoCompression);
        using (var stream = manifest.Open())
        {
            var packageName = compatible ? HomeApkValidator.HomePackageIdentifier : "example.invalid.package";
            var comment = smuggleCompatibleMarker ? $"<!-- {HomeApkValidator.HomePackageIdentifier} -->" : string.Empty;
            stream.Write(Encoding.UTF8.GetBytes($"<?xml version=\"1.0\"?><manifest package=\"{packageName}\">{comment}</manifest>"));
        }

        var scene = archive.CreateEntry("assets/scene.zip", CompressionLevel.NoCompression);
        using (var stream = scene.Open())
        {
            using var sceneBytes = new MemoryStream();
            using (var sceneArchive = new ZipArchive(sceneBytes, ZipArchiveMode.Create, leaveOpen: true))
            {
                var payload = sceneArchive.CreateEntry("scene.bin", CompressionLevel.NoCompression);
                using var payloadStream = payload.Open();
                payloadStream.Write(Encoding.UTF8.GetBytes("verified-scene-payload"));
            }

            stream.Write(sceneBytes.ToArray());
        }

        return path;
    }

    public void Dispose()
    {
        try
        {
            Directory.Delete(Root, recursive: true);
        }
        catch
        {
            // Test cleanup is best effort.
        }
    }
}

internal sealed class InMemoryAdbClient : IAdbClient
{
    private static readonly Regex QuotedPath = new("'(?<path>[^']*)'", RegexOptions.Compiled | RegexOptions.CultureInvariant);
    private readonly Dictionary<string, byte[]> remoteFiles = new(StringComparer.Ordinal);

    public string ExecutablePath => "fake-adb";

    public List<IReadOnlyList<string>> Commands { get; } = [];

    public string? InjectCollisionOnNextMovePath { get; set; }

    public byte[] InjectedCollisionBytes { get; set; } = Encoding.UTF8.GetBytes("simulated-racing-writer");

    public bool ReportQuestIdentity { get; set; } = true;

    public void AddRemoteFile(string path, byte[] content) => remoteFiles.Add(path, content.ToArray());

    public byte[] GetRemoteFile(string path) => remoteFiles[path].ToArray();

    public bool HasRemoteFile(string path) => remoteFiles.ContainsKey(path);

    public Task<CommandResult> RunAsync(
        IReadOnlyList<string> arguments,
        CancellationToken cancellationToken = default)
    {
        cancellationToken.ThrowIfCancellationRequested();
        Commands.Add(arguments.ToArray());
        var commandStart = arguments.Count >= 2 && arguments[0] == "-s" ? 2 : 0;
        if (commandStart == 0 && arguments.SequenceEqual(["devices", "-l"]))
        {
            return Task.FromResult(Success("List of devices attached\nQUEST\tdevice product:eureka model:Quest_3"));
        }

        if (arguments.Count > commandStart + 2 &&
            arguments[commandStart] == "shell" &&
            arguments[commandStart + 1] == "getprop")
        {
            var property = arguments[commandStart + 2];
            return Task.FromResult(property switch
            {
                "ro.product.manufacturer" => Success(ReportQuestIdentity ? "Oculus" : "Example Phone Corp"),
                "ro.product.model" => Success(ReportQuestIdentity ? "Quest 3" : "Phone X"),
                _ => Failure("unknown property")
            });
        }

        if (arguments.Count > commandStart + 3 &&
            arguments[commandStart] == "shell" &&
            arguments[commandStart + 1] == "pm" &&
            arguments[commandStart + 2] == "path" &&
            arguments[commandStart + 3] == "com.oculus.vrshell")
        {
            return Task.FromResult(ReportQuestIdentity
                ? Success("package:/system/priv-app/OVRService/OVRService.apk")
                : Failure("unknown package"));
        }
        if (arguments.Count > commandStart && arguments[commandStart] == "push")
        {
            var local = arguments[commandStart + 1];
            var remote = arguments[commandStart + 2];
            remoteFiles[remote] = File.ReadAllBytes(local);
            return Task.FromResult(Success("1 file pushed"));
        }

        if (arguments.Count > commandStart + 3 &&
            arguments[commandStart] == "shell" &&
            arguments[commandStart + 1] == "sh" &&
            arguments[commandStart + 2] == "-c")
        {
            return Task.FromResult(RunShell(arguments[commandStart + 3]));
        }

        if (arguments.Count > commandStart && arguments[commandStart] == "install")
        {
            return Task.FromResult(Success("Success"));
        }

        return Task.FromResult(Failure("unsupported fake ADB command"));
    }

    private CommandResult RunShell(string command)
    {
        var paths = QuotedPath.Matches(command).Select(match => match.Groups["path"].Value).ToArray();
        if (command.StartsWith("mkdir -p ", StringComparison.Ordinal))
        {
            return Success();
        }

        if (command.StartsWith("test -e ", StringComparison.Ordinal))
        {
            return remoteFiles.ContainsKey(paths[0]) ? Success() : Failure();
        }

        if (command.StartsWith("stat -c %s ", StringComparison.Ordinal))
        {
            return remoteFiles.TryGetValue(paths[0], out var bytes)
                ? Success(bytes.LongLength.ToString(System.Globalization.CultureInfo.InvariantCulture))
                : Failure("missing");
        }

        if (command.StartsWith("sha256sum ", StringComparison.Ordinal) ||
            command.StartsWith("toybox sha256sum ", StringComparison.Ordinal))
        {
            return remoteFiles.TryGetValue(paths[0], out var bytes)
                ? Success($"{Convert.ToHexString(SHA256.HashData(bytes))}  {paths[0]}")
                : Failure("missing");
        }

        if (command.StartsWith("mv -n ", StringComparison.Ordinal))
        {
            var source = paths[0];
            var destination = paths[1];
            if (!remoteFiles.TryGetValue(source, out var bytes))
            {
                return Failure("missing source");
            }

            if (string.Equals(destination, InjectCollisionOnNextMovePath, StringComparison.Ordinal))
            {
                remoteFiles.TryAdd(destination, InjectedCollisionBytes.ToArray());
                InjectCollisionOnNextMovePath = null;
            }

            if (!remoteFiles.ContainsKey(destination))
            {
                remoteFiles.Add(destination, bytes);
                remoteFiles.Remove(source);
            }

            return Success();
        }

        if (command.StartsWith("rm -f ", StringComparison.Ordinal))
        {
            remoteFiles.Remove(paths[0]);
            return Success();
        }

        if (command.StartsWith("pm path ", StringComparison.Ordinal))
        {
            return Success("package:/data/app/io.github.nikitat21.questhomeswitcher/base.apk");
        }

        if (command.StartsWith("dumpsys package ", StringComparison.Ordinal))
        {
            return Success("  versionCode=16 minSdk=29\n  versionName=1.8");
        }

        return Failure($"unsupported fake shell command: {command}");
    }

    private static CommandResult Success(string output = "") => new(0, output, "");

    private static CommandResult Failure(string error = "") => new(1, "", error);
}
