using System;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Security.Cryptography;
using System.Windows.Forms;

[assembly: AssemblyTitle("Quest Home Switcher Setup")]
[assembly: AssemblyDescription("State-aware setup for Quest Home Switcher and Shizuku")]
[assembly: AssemblyCompany("Quest Community Tools")]
[assembly: AssemblyProduct("Quest Home Switcher Setup")]
[assembly: AssemblyVersion("1.3.0.0")]
[assembly: AssemblyFileVersion("1.3.0.0")]

internal static class Program
{
    private const string ScriptResourceName = "QuestHomeSwitcherSetupAssistant.QuestHomeSwitcherSetup.ps1";
    private const string PayloadResourceName = "QuestHomeSwitcherSetupAssistant.Quest-Home-Switcher.apk";
    private const string ExpectedPayloadSha256 = "A500F308DB4B997BC8BE8C555963D76B201114FF04F39790C50288CAEF7B34F8";

    [STAThread]
    private static void Main(string[] args)
    {
        string runtime = Path.Combine(
            Path.GetTempPath(),
            "QuestHomeSwitcherSetup",
            "1.3.0",
            Guid.NewGuid().ToString("N"));

        try
        {
            Directory.CreateDirectory(runtime);
            string script = Path.Combine(runtime, "QuestHomeSwitcherSetup.ps1");
            string payload = Path.Combine(runtime, "Quest-Home-Switcher.apk");
            ExtractResource(ScriptResourceName, script);
            ExtractResource(PayloadResourceName, payload);
            VerifySha256(payload, ExpectedPayloadSha256);

            string mode = Array.Exists(args, value => value == "--self-test")
                ? " -SelfTest"
                : string.Empty;

            var startInfo = new ProcessStartInfo
            {
                FileName = "powershell.exe",
                Arguments =
                    "-NoProfile -ExecutionPolicy Bypass -STA -File " +
                    Quote(script) +
                    " -DistributionRoot " + Quote(runtime) +
                    mode,
                UseShellExecute = false,
                CreateNoWindow = true,
                WorkingDirectory = runtime
            };

            using (Process process = Process.Start(startInfo))
            {
                process.WaitForExit();
                Environment.ExitCode = process.ExitCode;
            }
        }
        catch (Exception ex)
        {
            MessageBox.Show(
                "Quest Home Switcher Setup could not start.\n\n" + ex.Message,
                "Quest Home Switcher Setup",
                MessageBoxButtons.OK,
                MessageBoxIcon.Error);
            Environment.ExitCode = 1;
        }
        finally
        {
            try { Directory.Delete(runtime, true); } catch { }
        }
    }

    private static string Quote(string value)
    {
        return "\"" + value.Replace("\"", "\\\"") + "\"";
    }

    private static void ExtractResource(string resourceName, string destination)
    {
        Assembly assembly = Assembly.GetExecutingAssembly();
        using (Stream input = assembly.GetManifestResourceStream(resourceName))
        {
            if (input == null)
                throw new InvalidOperationException("Embedded setup resource is missing: " + resourceName);

            using (FileStream output = new FileStream(destination, FileMode.Create, FileAccess.Write, FileShare.None))
                input.CopyTo(output);
        }
    }

    private static void VerifySha256(string path, string expected)
    {
        string actual;
        using (SHA256 sha256 = SHA256.Create())
        using (FileStream input = File.OpenRead(path))
            actual = BitConverter.ToString(sha256.ComputeHash(input)).Replace("-", string.Empty);

        if (!string.Equals(actual, expected, StringComparison.OrdinalIgnoreCase))
            throw new InvalidDataException("Embedded Quest Home Switcher payload failed its SHA-256 integrity check.");
    }
}
