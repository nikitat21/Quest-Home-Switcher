namespace QuestHomeSwitcher.Cli;

internal sealed class CliArguments
{
    private static readonly HashSet<string> ValueOptions = new(StringComparer.Ordinal)
    {
        "--adb",
        "--serial",
        "--sha256",
        "--name"
    };

    private CliArguments(
        string command,
        IReadOnlyList<string> positionals,
        IReadOnlyDictionary<string, string> options,
        bool helpRequested)
    {
        Command = command;
        Positionals = positionals;
        Options = options;
        HelpRequested = helpRequested;
    }

    public string Command { get; }

    public IReadOnlyList<string> Positionals { get; }

    public IReadOnlyDictionary<string, string> Options { get; }

    public bool HelpRequested { get; }

    public string? GetOption(string name) => Options.GetValueOrDefault(name);

    public static CliArguments Parse(string[] args)
    {
        ArgumentNullException.ThrowIfNull(args);
        if (args.Length == 0)
        {
            return new CliArguments("help", [], new Dictionary<string, string>(), true);
        }

        var command = args[0].Trim().ToLowerInvariant();
        var positionals = new List<string>();
        var options = new Dictionary<string, string>(StringComparer.Ordinal);
        var help = false;

        for (var index = 1; index < args.Length; index++)
        {
            var argument = args[index];
            if (argument is "--help" or "-h")
            {
                help = true;
                continue;
            }

            if (!argument.StartsWith("--", StringComparison.Ordinal))
            {
                positionals.Add(argument);
                continue;
            }

            var equalsIndex = argument.IndexOf('=');
            var optionName = equalsIndex >= 0 ? argument[..equalsIndex] : argument;
            if (!ValueOptions.Contains(optionName))
            {
                throw new ArgumentException($"Unknown option '{optionName}'.");
            }

            if (options.ContainsKey(optionName))
            {
                throw new ArgumentException($"Option '{optionName}' may only be specified once.");
            }

            string optionValue;
            if (equalsIndex >= 0)
            {
                optionValue = argument[(equalsIndex + 1)..];
            }
            else if (++index < args.Length)
            {
                optionValue = args[index];
            }
            else
            {
                throw new ArgumentException($"Option '{optionName}' requires a value.");
            }

            if (string.IsNullOrWhiteSpace(optionValue))
            {
                throw new ArgumentException($"Option '{optionName}' requires a non-empty value.");
            }

            options.Add(optionName, optionValue);
        }

        return new CliArguments(command, positionals, options, help);
    }
}
