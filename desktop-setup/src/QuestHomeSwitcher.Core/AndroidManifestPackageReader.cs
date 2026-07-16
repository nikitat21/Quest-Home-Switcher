using System.Buffers.Binary;
using System.Text;
using System.Xml;

namespace QuestHomeSwitcher.Core;

internal static class AndroidManifestPackageReader
{
    private const ushort XmlChunkType = 0x0003;
    private const ushort StringPoolChunkType = 0x0001;
    private const ushort StartElementChunkType = 0x0102;
    private const uint NoString = uint.MaxValue;
    private const byte TypedString = 0x03;
    private const uint Utf8Flag = 0x00000100;
    private static readonly UTF8Encoding StrictUtf8 = new(false, true);
    private static readonly UnicodeEncoding StrictUtf16 = new(false, false, true);

    public static string ReadPackageName(ReadOnlySpan<byte> manifest)
    {
        if (manifest.IsEmpty)
        {
            throw new InvalidDataException("AndroidManifest.xml is empty.");
        }

        if (LooksLikeTextXml(manifest))
        {
            return ReadTextPackageName(manifest);
        }

        return ReadBinaryPackageName(manifest);
    }

    private static bool LooksLikeTextXml(ReadOnlySpan<byte> bytes)
    {
        var hasUtf8Bom = bytes.Length >= 3 && bytes[0] == 0xEF && bytes[1] == 0xBB && bytes[2] == 0xBF;
        var offset = hasUtf8Bom ? 3 : 0;
        while (offset < bytes.Length && bytes[offset] is (byte)' ' or (byte)'\t' or (byte)'\r' or (byte)'\n')
        {
            offset++;
        }

        return offset < bytes.Length && bytes[offset] == (byte)'<';
    }

    private static string ReadTextPackageName(ReadOnlySpan<byte> manifest)
    {
        using var stream = new MemoryStream(manifest.ToArray(), writable: false);
        using var reader = XmlReader.Create(stream, new XmlReaderSettings
        {
            DtdProcessing = DtdProcessing.Prohibit,
            XmlResolver = null,
            IgnoreComments = true,
            IgnoreWhitespace = true,
            MaxCharactersInDocument = manifest.Length
        });
        reader.MoveToContent();
        if (!string.Equals(reader.LocalName, "manifest", StringComparison.Ordinal))
        {
            throw new InvalidDataException("AndroidManifest.xml has no manifest root element.");
        }

        return reader.GetAttribute("package")?.Trim() is { Length: > 0 } packageName
            ? packageName
            : throw new InvalidDataException("AndroidManifest.xml has no package attribute.");
    }

    private static string ReadBinaryPackageName(ReadOnlySpan<byte> manifest)
    {
        if (manifest.Length < 8 || ReadUInt16(manifest, 0) != XmlChunkType)
        {
            throw new InvalidDataException("AndroidManifest.xml is neither text XML nor Android binary XML.");
        }

        var xmlHeaderSize = ReadUInt16(manifest, 2);
        var totalSize = ReadBoundedSize(manifest, 4, manifest.Length);
        if (xmlHeaderSize < 8 || xmlHeaderSize > totalSize)
        {
            throw new InvalidDataException("Android binary XML has an invalid document header.");
        }

        string[]? strings = null;
        var offset = (int)xmlHeaderSize;
        while (offset < totalSize)
        {
            RequireRange(manifest, offset, 8);
            var type = ReadUInt16(manifest, offset);
            var headerSize = ReadUInt16(manifest, offset + 2);
            var chunkSize = ReadBoundedSize(manifest, offset + 4, totalSize - offset);
            if (headerSize < 8 || headerSize > chunkSize)
            {
                throw new InvalidDataException("Android binary XML contains an invalid chunk header.");
            }

            if (type == StringPoolChunkType)
            {
                strings = ReadStringPool(manifest.Slice(offset, chunkSize));
            }
            else if (type == StartElementChunkType)
            {
                if (strings is null)
                {
                    throw new InvalidDataException("Android binary XML uses an element before its string pool.");
                }

                var packageName = TryReadManifestElement(manifest.Slice(offset, chunkSize), headerSize, strings);
                if (packageName is not null)
                {
                    return packageName;
                }
            }

            offset = checked(offset + chunkSize);
        }

        throw new InvalidDataException("AndroidManifest.xml has no manifest package attribute.");
    }

    private static string[] ReadStringPool(ReadOnlySpan<byte> chunk)
    {
        if (chunk.Length < 28)
        {
            throw new InvalidDataException("Android binary XML string pool is truncated.");
        }

        var headerSize = ReadUInt16(chunk, 2);
        var stringCount = ReadBoundedCount(chunk, 8);
        var styleCount = ReadBoundedCount(chunk, 12);
        var flags = ReadUInt32(chunk, 16);
        var stringsStart = ReadBoundedOffset(chunk, 20);
        _ = ReadBoundedOffset(chunk, 24);
        var offsetsBytes = checked((stringCount + styleCount) * 4);
        RequireRange(chunk, headerSize, offsetsBytes);
        if (stringsStart < headerSize + offsetsBytes || stringsStart > chunk.Length)
        {
            throw new InvalidDataException("Android binary XML string data has an invalid offset.");
        }

        var result = new string[stringCount];
        for (var index = 0; index < stringCount; index++)
        {
            var relative = ReadBoundedOffset(chunk, headerSize + index * 4);
            var cursor = checked(stringsStart + relative);
            if (cursor >= chunk.Length)
            {
                throw new InvalidDataException("Android binary XML string offset is outside the pool.");
            }

            result[index] = (flags & Utf8Flag) != 0
                ? ReadUtf8String(chunk, ref cursor)
                : ReadUtf16String(chunk, ref cursor);
        }

        return result;
    }

    private static string? TryReadManifestElement(
        ReadOnlySpan<byte> chunk,
        ushort nodeHeaderSize,
        IReadOnlyList<string> strings)
    {
        if (nodeHeaderSize < 16 || chunk.Length < nodeHeaderSize + 20)
        {
            throw new InvalidDataException("Android binary XML start element is truncated.");
        }

        var extension = (int)nodeHeaderSize;
        var elementNameIndex = ReadUInt32(chunk, extension + 4);
        if (!string.Equals(GetString(strings, elementNameIndex), "manifest", StringComparison.Ordinal))
        {
            return null;
        }

        var attributeStart = ReadUInt16(chunk, extension + 8);
        var attributeSize = ReadUInt16(chunk, extension + 10);
        var attributeCount = ReadUInt16(chunk, extension + 12);
        if (attributeSize < 20)
        {
            throw new InvalidDataException("Android binary XML has an invalid attribute size.");
        }

        var attributesOffset = checked(extension + attributeStart);
        RequireRange(chunk, attributesOffset, checked(attributeSize * attributeCount));
        for (var index = 0; index < attributeCount; index++)
        {
            var attribute = checked(attributesOffset + index * attributeSize);
            var attributeName = GetString(strings, ReadUInt32(chunk, attribute + 4));
            if (!string.Equals(attributeName, "package", StringComparison.Ordinal))
            {
                continue;
            }

            var rawValue = ReadUInt32(chunk, attribute + 8);
            var typedValueSize = ReadUInt16(chunk, attribute + 12);
            var typedValueType = chunk[attribute + 15];
            var typedValueData = ReadUInt32(chunk, attribute + 16);
            if (typedValueSize < 8)
            {
                throw new InvalidDataException("Android binary XML package attribute is malformed.");
            }

            var valueIndex = rawValue != NoString
                ? rawValue
                : typedValueType == TypedString
                    ? typedValueData
                    : NoString;
            var packageName = GetString(strings, valueIndex)?.Trim();
            return !string.IsNullOrWhiteSpace(packageName)
                ? packageName
                : throw new InvalidDataException("AndroidManifest.xml has an empty package attribute.");
        }

        throw new InvalidDataException("AndroidManifest.xml has no package attribute.");
    }

    private static string ReadUtf8String(ReadOnlySpan<byte> bytes, ref int cursor)
    {
        _ = ReadLength8(bytes, ref cursor);
        var byteLength = ReadLength8(bytes, ref cursor);
        RequireRange(bytes, cursor, byteLength + 1);
        if (bytes[cursor + byteLength] != 0)
        {
            throw new InvalidDataException("Android binary XML UTF-8 string is not terminated.");
        }

        var value = StrictUtf8.GetString(bytes.Slice(cursor, byteLength));
        cursor += byteLength + 1;
        return value;
    }

    private static string ReadUtf16String(ReadOnlySpan<byte> bytes, ref int cursor)
    {
        var characterLength = ReadLength16(bytes, ref cursor);
        var byteLength = checked(characterLength * 2);
        RequireRange(bytes, cursor, byteLength + 2);
        if (ReadUInt16(bytes, cursor + byteLength) != 0)
        {
            throw new InvalidDataException("Android binary XML UTF-16 string is not terminated.");
        }

        var value = StrictUtf16.GetString(bytes.Slice(cursor, byteLength));
        cursor += byteLength + 2;
        return value;
    }

    private static int ReadLength8(ReadOnlySpan<byte> bytes, ref int cursor)
    {
        RequireRange(bytes, cursor, 1);
        var first = bytes[cursor++];
        if ((first & 0x80) == 0)
        {
            return first;
        }

        RequireRange(bytes, cursor, 1);
        return ((first & 0x7F) << 8) | bytes[cursor++];
    }

    private static int ReadLength16(ReadOnlySpan<byte> bytes, ref int cursor)
    {
        RequireRange(bytes, cursor, 2);
        var first = ReadUInt16(bytes, cursor);
        cursor += 2;
        if ((first & 0x8000) == 0)
        {
            return first;
        }

        RequireRange(bytes, cursor, 2);
        var second = ReadUInt16(bytes, cursor);
        cursor += 2;
        return checked(((first & 0x7FFF) << 16) | second);
    }

    private static string? GetString(IReadOnlyList<string> strings, uint index) =>
        index == NoString || index >= strings.Count ? null : strings[(int)index];

    private static ushort ReadUInt16(ReadOnlySpan<byte> bytes, int offset)
    {
        RequireRange(bytes, offset, 2);
        return BinaryPrimitives.ReadUInt16LittleEndian(bytes.Slice(offset, 2));
    }

    private static uint ReadUInt32(ReadOnlySpan<byte> bytes, int offset)
    {
        RequireRange(bytes, offset, 4);
        return BinaryPrimitives.ReadUInt32LittleEndian(bytes.Slice(offset, 4));
    }

    private static int ReadBoundedSize(ReadOnlySpan<byte> bytes, int offset, int maximum)
    {
        var value = ReadUInt32(bytes, offset);
        if (value < 8 || value > maximum || value > int.MaxValue)
        {
            throw new InvalidDataException("Android binary XML contains an invalid chunk size.");
        }

        return (int)value;
    }

    private static int ReadBoundedCount(ReadOnlySpan<byte> bytes, int offset)
    {
        var value = ReadUInt32(bytes, offset);
        if (value > 1_000_000)
        {
            throw new InvalidDataException("Android binary XML contains an unreasonable item count.");
        }

        return (int)value;
    }

    private static int ReadBoundedOffset(ReadOnlySpan<byte> bytes, int offset)
    {
        var value = ReadUInt32(bytes, offset);
        if (value > int.MaxValue)
        {
            throw new InvalidDataException("Android binary XML contains an invalid offset.");
        }

        return (int)value;
    }

    private static void RequireRange(ReadOnlySpan<byte> bytes, int offset, int length)
    {
        if (offset < 0 || length < 0 || offset > bytes.Length - length)
        {
            throw new InvalidDataException("Android binary XML is truncated or malformed.");
        }
    }
}
