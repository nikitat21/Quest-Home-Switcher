param(
    [string]$InputPng = (Join-Path $PSScriptRoot 'branding\Quest-Home-Switcher-Icon.png'),
    [string]$OutputIco = (Join-Path $PSScriptRoot 'branding\Quest-Home-Switcher.ico')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

if (-not (Test-Path -LiteralPath $InputPng -PathType Leaf)) {
    throw "Icon source PNG is missing: $InputPng"
}

$sizes = @(256, 128, 64, 48, 32, 24, 16)
$images = New-Object 'System.Collections.Generic.List[byte[]]'
$source = [System.Drawing.Image]::FromFile($InputPng)
try {
    foreach ($size in $sizes) {
        $bitmap = New-Object System.Drawing.Bitmap(
            $size,
            $size,
            [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
        )
        try {
            $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
            try {
                $graphics.CompositingMode = [System.Drawing.Drawing2D.CompositingMode]::SourceCopy
                $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
                $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
                $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
                $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
                $graphics.DrawImage($source, 0, 0, $size, $size)
            } finally {
                $graphics.Dispose()
            }

            $stream = New-Object System.IO.MemoryStream
            try {
                $bitmap.Save($stream, [System.Drawing.Imaging.ImageFormat]::Png)
                $images.Add($stream.ToArray())
            } finally {
                $stream.Dispose()
            }
        } finally {
            $bitmap.Dispose()
        }
    }
} finally {
    $source.Dispose()
}

$parent = Split-Path -Parent $OutputIco
New-Item -ItemType Directory -Path $parent -Force | Out-Null
$file = [System.IO.File]::Open($OutputIco, [System.IO.FileMode]::Create)
$writer = New-Object System.IO.BinaryWriter($file)
try {
    $writer.Write([uint16]0)
    $writer.Write([uint16]1)
    $writer.Write([uint16]$images.Count)

    $offset = 6 + (16 * $images.Count)
    for ($index = 0; $index -lt $images.Count; $index++) {
        $size = $sizes[$index]
        $writer.Write([byte]$(if ($size -eq 256) { 0 } else { $size }))
        $writer.Write([byte]$(if ($size -eq 256) { 0 } else { $size }))
        $writer.Write([byte]0)
        $writer.Write([byte]0)
        $writer.Write([uint16]1)
        $writer.Write([uint16]32)
        $writer.Write([uint32]$images[$index].Length)
        $writer.Write([uint32]$offset)
        $offset += $images[$index].Length
    }

    foreach ($image in $images) {
        $writer.Write($image)
    }
} finally {
    $writer.Dispose()
    $file.Dispose()
}

$icon = New-Object System.Drawing.Icon($OutputIco)
try {
    if ($icon.Width -le 0 -or $icon.Height -le 0) {
        throw 'Windows could not load the generated icon.'
    }
} finally {
    $icon.Dispose()
}

$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $OutputIco).Hash
Write-Output "ICON_OK $OutputIco"
Write-Output "SHA256 $hash"
