using System.Text;

namespace QuestHomeSwitcher.Core;

public static class HomeFileNamePolicy
{
    private const int MaximumStemCharacters = 96;

    public static string Create(string sourcePath, string? requestedName = null)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(sourcePath);
        var input = string.IsNullOrWhiteSpace(requestedName)
            ? Path.GetFileNameWithoutExtension(sourcePath)
            : Path.GetFileNameWithoutExtension(requestedName.Trim());
        input = input.Normalize(NormalizationForm.FormKC);

        var builder = new StringBuilder();
        var pendingSpace = false;
        foreach (var rune in input.EnumerateRunes())
        {
            var allowedPunctuation = rune.Value is '-' or '_' or '.' or '(' or ')' or '[' or ']';
            if (Rune.IsLetterOrDigit(rune) || allowedPunctuation)
            {
                if (pendingSpace && builder.Length > 0 && builder[^1] != ' ')
                {
                    builder.Append(' ');
                }

                pendingSpace = false;
                builder.Append(rune.ToString());
            }
            else
            {
                pendingSpace = true;
            }

            if (builder.Length >= MaximumStemCharacters)
            {
                break;
            }
        }

        var stem = builder.ToString().Trim(' ', '.', '-', '_');
        if (string.IsNullOrWhiteSpace(stem))
        {
            stem = "Custom Home";
        }

        return $"{stem}.apk";
    }

    public static string AddSuffix(string safeFileName, int number)
    {
        if (number < 2)
        {
            return safeFileName;
        }

        var stem = Path.GetFileNameWithoutExtension(safeFileName);
        return $"{stem}-{number}.apk";
    }
}
