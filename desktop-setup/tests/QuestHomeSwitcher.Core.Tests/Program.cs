using System.IO.Compression;
using System.Security.Cryptography;
using System.Text;
using QuestHomeSwitcher.Core;

namespace QuestHomeSwitcher.Core.Tests;

internal static class Program
{
    public static async Task<int> Main()
    {
        var tests = new (string Name, Func<Task> Run)[]
        {
            ("platform detection", TestPlatformDetectionAsync),
            ("ADB device parsing", TestDeviceParsingAsync),
            ("non-Quest Android device rejection", TestNonQuestRejectionAsync),
            ("remote shell quoting", TestRemoteShellQuotingAsync),
            ("safe Home filename", TestSafeFileNameAsync),
            ("compatible Home validation", TestHomeValidationAsync),
            ("incompatible Home rejection", TestHomeRejectionAsync),
            ("manifest marker smuggling rejection", TestManifestMarkerSmugglingAsync),
            ("untrusted Switcher fails before ADB", TestUntrustedSwitcherAsync),
            ("Switcher install never uninstalls", TestSwitcherInstallAsync),
            ("identical Home is not overwritten", TestAlreadyPresentImportAsync),
            ("Home collision receives suffix", TestCollisionImportAsync),
            ("racing writer is not overwritten", TestRacingCollisionImportAsync)
        };

        var failures = 0;
        foreach (var test in tests)
        {
            try
            {
                await test.Run().ConfigureAwait(false);
                Console.WriteLine($"PASS {test.Name}");
            }
            catch (Exception exception)
            {
                failures++;
                Console.Error.WriteLine($"FAIL {test.Name}: {exception}");
            }
        }

        Console.WriteLine($"{tests.Length - failures}/{tests.Length} tests passed.");
        return failures == 0 ? 0 : 1;
    }

    private static Task TestPlatformDetectionAsync()
    {
        var host = PlatformDetector.Detect();
        AssertEx.True(host.Platform != HostPlatform.Unsupported, "CI host must be one of the three supported desktop platforms.");
        AssertEx.True(host.PlatformToolsDownload.IsAbsoluteUri, "Platform Tools URL must be absolute.");
        return Task.CompletedTask;
    }

    private static Task TestDeviceParsingAsync()
    {
        var devices = AdbDeviceService.ParseDeviceList(
            "List of devices attached\nQUEST123\tdevice product:hollywood model:Quest\nQUEST999\tunauthorized usb:1-1\n");
        AssertEx.Equal(2, devices.Count, "Two ADB rows should be parsed.");
        AssertEx.True(devices[0].IsReady, "The authorized device should be ready.");
        AssertEx.Equal("unauthorized", devices[1].State, "Unauthorized state should be retained.");
        return Task.CompletedTask;
    }

    private static Task TestRemoteShellQuotingAsync()
    {
        AssertEx.Equal("'Quest Homes'", RemoteShell.Quote("Quest Homes"), "Spaces must remain inside one shell literal.");
        AssertEx.Equal("'a'\\''b'", RemoteShell.Quote("a'b"), "Apostrophes must be escaped without shell evaluation.");
        return Task.CompletedTask;
    }

    private static Task TestSafeFileNameAsync()
    {
        var safe = HomeFileNamePolicy.Create("ignored.apk", "../ My: Home??.APK");
        AssertEx.Equal("My Home.apk", safe, "Unsafe punctuation and traversal must be removed.");
        AssertEx.Equal("My Home-2.apk", HomeFileNamePolicy.AddSuffix(safe, 2), "Collision suffix should be stable.");
        return Task.CompletedTask;
    }

    private static async Task TestHomeValidationAsync()
    {
        using var workspace = new TestWorkspace();
        var path = workspace.CreateHomeApk("Home.apk");
        var result = await new HomeApkValidator().ValidateAsync(path).ConfigureAwait(false);
        string expectedScene;
        using (var archive = ZipFile.OpenRead(path))
        await using (var scene = archive.GetEntry("assets/scene.zip")!.Open())
        {
            expectedScene = Convert.ToHexString(await SHA256.HashDataAsync(scene).ConfigureAwait(false));
        }
        AssertEx.Equal(expectedScene, result.SceneSha256, "Decompressed scene entry hash must be reported.");
        AssertEx.Equal(await FileHashing.Sha256Async(path), result.FileSha256, "Whole APK hash must be reported.");
    }

    private static async Task TestHomeRejectionAsync()
    {
        using var workspace = new TestWorkspace();
        var path = workspace.CreateHomeApk("Wrong.apk", compatible: false);
        await AssertEx.ThrowsAsync<InvalidDataException>(
            () => new HomeApkValidator().ValidateAsync(path),
            "An APK without the Quest Home package marker must fail closed.").ConfigureAwait(false);
    }

    private static async Task TestManifestMarkerSmugglingAsync()
    {
        using var workspace = new TestWorkspace();
        var path = workspace.CreateHomeApk("Smuggled.apk", compatible: false, smuggleCompatibleMarker: true);
        await AssertEx.ThrowsAsync<InvalidDataException>(
            () => new HomeApkValidator().ValidateAsync(path),
            "A marker outside the manifest package attribute must not pass validation.").ConfigureAwait(false);
    }

    private static async Task TestNonQuestRejectionAsync()
    {
        var adb = new InMemoryAdbClient { ReportQuestIdentity = false };
        await AssertEx.ThrowsAsync<InvalidOperationException>(
            () => new AdbDeviceService(adb).GetReadySessionAsync(),
            "An authorized phone must not be selected as a Quest.").ConfigureAwait(false);
    }

    private static async Task TestUntrustedSwitcherAsync()
    {
        using var workspace = new TestWorkspace();
        var path = Path.Combine(workspace.Root, "Switcher.apk");
        await File.WriteAllBytesAsync(path, [1, 2, 3, 4]).ConfigureAwait(false);
        var adb = new InMemoryAdbClient();
        var service = new SwitcherService(new DeviceStatusService());
        await AssertEx.ThrowsAsync<InvalidDataException>(
            () => service.InstallAsync(new AdbSession(adb, "QUEST"), path),
            "An unpinned payload must be rejected.").ConfigureAwait(false);
        AssertEx.Equal(0, adb.Commands.Count, "No ADB command may run before payload trust is established.");
    }

    private static async Task TestSwitcherInstallAsync()
    {
        using var workspace = new TestWorkspace();
        var path = Path.Combine(workspace.Root, "Switcher.apk");
        await File.WriteAllBytesAsync(path, [5, 6, 7, 8]).ConfigureAwait(false);
        var hash = await FileHashing.Sha256Async(path).ConfigureAwait(false);
        var adb = new InMemoryAdbClient();
        var result = await new SwitcherService(new DeviceStatusService())
            .InstallAsync(new AdbSession(adb, "QUEST"), path, hash)
            .ConfigureAwait(false);
        AssertEx.True(result.InstalledPackage.Installed, "Expected Switcher package must be verified after install.");
        AssertEx.True(
            adb.Commands.All(command => !string.Join(' ', command).Contains("uninstall", StringComparison.OrdinalIgnoreCase)),
            "Switcher installation must contain no uninstall command.");
    }

    private static async Task TestAlreadyPresentImportAsync()
    {
        using var workspace = new TestWorkspace();
        var path = workspace.CreateHomeApk("Home.apk");
        var content = await File.ReadAllBytesAsync(path).ConfigureAwait(false);
        var target = $"{HomeImportService.RemoteImportDirectory}/Home.apk";
        var adb = new InMemoryAdbClient();
        adb.AddRemoteFile(target, content);
        var result = await new HomeImportService(new HomeApkValidator())
            .ImportAsync(new AdbSession(adb, "QUEST"), path)
            .ConfigureAwait(false);
        AssertEx.Equal(HomeImportDisposition.AlreadyPresent, result.Disposition, "Identical remote APK should be skipped.");
        AssertEx.True(content.SequenceEqual(adb.GetRemoteFile(target)), "Existing target bytes must remain unchanged.");
        AssertOnlyOwnedTemporaryDeletes(adb);
    }

    private static async Task TestCollisionImportAsync()
    {
        using var workspace = new TestWorkspace();
        var path = workspace.CreateHomeApk("Home.apk");
        var originalTarget = $"{HomeImportService.RemoteImportDirectory}/Home.apk";
        var suffixedTarget = $"{HomeImportService.RemoteImportDirectory}/Home-2.apk";
        var personalBytes = Encoding.UTF8.GetBytes("personal-existing-file");
        var adb = new InMemoryAdbClient();
        adb.AddRemoteFile(originalTarget, personalBytes);
        var result = await new HomeImportService(new HomeApkValidator())
            .ImportAsync(new AdbSession(adb, "QUEST"), path)
            .ConfigureAwait(false);
        AssertEx.Equal(HomeImportDisposition.Uploaded, result.Disposition, "Different existing APK should force a suffix.");
        AssertEx.Equal(suffixedTarget, result.RemotePath, "First safe suffix should be selected.");
        AssertEx.True(personalBytes.SequenceEqual(adb.GetRemoteFile(originalTarget)), "Personal collision target must remain byte-identical.");
        AssertEx.True(adb.HasRemoteFile(suffixedTarget), "New APK should be present under the suffixed name.");
        AssertOnlyOwnedTemporaryDeletes(adb);
    }

    private static async Task TestRacingCollisionImportAsync()
    {
        using var workspace = new TestWorkspace();
        var path = workspace.CreateHomeApk("Home.apk");
        var racingTarget = $"{HomeImportService.RemoteImportDirectory}/Home.apk";
        var safeTarget = $"{HomeImportService.RemoteImportDirectory}/Home-2.apk";
        var adb = new InMemoryAdbClient { InjectCollisionOnNextMovePath = racingTarget };
        var result = await new HomeImportService(new HomeApkValidator())
            .ImportAsync(new AdbSession(adb, "QUEST"), path)
            .ConfigureAwait(false);
        AssertEx.Equal(safeTarget, result.RemotePath, "A race at commit time must advance to a suffix.");
        AssertEx.True(
            adb.InjectedCollisionBytes.SequenceEqual(adb.GetRemoteFile(racingTarget)),
            "The racing writer's target must remain byte-identical.");
        AssertEx.True(adb.HasRemoteFile(safeTarget), "The verified temporary upload should be reused for the next safe name.");
        AssertOnlyOwnedTemporaryDeletes(adb);
    }

    private static void AssertOnlyOwnedTemporaryDeletes(InMemoryAdbClient adb)
    {
        var deleteCommands = adb.Commands
            .Select(command => string.Join(' ', command))
            .Where(command => command.Contains("rm -f", StringComparison.Ordinal))
            .ToArray();
        AssertEx.True(
            deleteCommands.All(command => command.Contains("/.qhs-upload-", StringComparison.Ordinal) && command.Contains(".part", StringComparison.Ordinal)),
            "Only uniquely owned .part uploads may be removed.");
    }
}
