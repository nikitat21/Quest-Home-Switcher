param(
    [switch]$SkipSelfTest
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$source = Join-Path $root 'QuestHomeSwitcherSetupLauncher.cs'
$script = Join-Path $root 'QuestHomeSwitcherSetup.ps1'
$payload = Join-Path $root 'Quest-Home-Switcher.apk'
$libraryCatalog = Join-Path $root 'Official-Home-Library-v1.5.json'
$icon = Join-Path $root 'branding\Quest-Home-Switcher.ico'
$output = Join-Path $root 'Quest-Home-Switcher-Setup-v1.8.exe'
$expectedPayloadHash = 'CFF3676D81209A2BC30C56A4587ECFC04789F6F0AF18733D84ADE04917362A50'
$expectedLibraryCatalogHash = 'DDF843091444A27684D0AC76A4A3C663A0ACA6A54D0DDB6585D043080749ED23'

foreach ($required in @($source, $script, $payload, $libraryCatalog, $icon)) {
    if (-not (Test-Path -LiteralPath $required)) {
        throw "Required build input is missing: $required"
    }
}

$actualPayloadHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $payload).Hash
if ($actualPayloadHash -ne $expectedPayloadHash) {
    throw 'Quest-Home-Switcher.apk does not match the expected permanently signed v1.8 payload.'
}
$actualLibraryCatalogHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $libraryCatalog).Hash
if ($actualLibraryCatalogHash -ne $expectedLibraryCatalogHash) {
    throw 'Official-Home-Library-v1.5.json does not match the pinned verified catalog.'
}

$compilerCandidates = @(
    "$env:WINDIR\Microsoft.NET\Framework64\v4.0.30319\csc.exe",
    "$env:WINDIR\Microsoft.NET\Framework\v4.0.30319\csc.exe"
)
$compiler = $compilerCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if (-not $compiler) {
    throw 'The Windows C# compiler was not found.'
}

if (Test-Path -LiteralPath $output) {
    Remove-Item -LiteralPath $output -Force
}

& $compiler /nologo /target:winexe /optimize+ `
    "/out:$output" `
    "/win32icon:$icon" `
    "/resource:$script,QuestHomeSwitcherSetupAssistant.QuestHomeSwitcherSetup.ps1" `
    "/resource:$payload,QuestHomeSwitcherSetupAssistant.Quest-Home-Switcher.apk" `
    "/resource:$libraryCatalog,QuestHomeSwitcherSetupAssistant.Official-Home-Library-v1.5.json" `
    /reference:System.Windows.Forms.dll `
    $source

if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $output)) {
    throw 'Quest Home Switcher Setup could not be built.'
}

if (-not $SkipSelfTest) {
    # Prove that the distributable works with no sibling files. The EXE is copied
    # by itself to an isolated build-test directory before the embedded-payload test.
    $testBase = [System.IO.Path]::GetFullPath((Join-Path ([System.IO.Path]::GetTempPath()) 'QuestHomeSwitcherSetupBuildTest'))
    $testRoot = [System.IO.Path]::GetFullPath((Join-Path $testBase ([guid]::NewGuid().ToString('N'))))
    $testExe = Join-Path $testRoot 'Quest-Home-Switcher-Setup-v1.8.exe'
    try {
        New-Item -ItemType Directory -Force -Path $testRoot | Out-Null
        Copy-Item -LiteralPath $output -Destination $testExe
        $test = Start-Process -FilePath $testExe -ArgumentList '--self-test' -WindowStyle Hidden -Wait -PassThru
        if ($test.ExitCode -ne 0) {
            throw "Embedded one-file setup self-test failed with exit code $($test.ExitCode)."
        }
    } finally {
        $safePrefix = $testBase.TrimEnd([System.IO.Path]::DirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
        if ($testRoot.StartsWith($safePrefix, [System.StringComparison]::OrdinalIgnoreCase) -and (Test-Path -LiteralPath $testRoot)) {
            Remove-Item -LiteralPath $testRoot -Recurse -Force
        }
    }
}

$hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $output).Hash
Write-Output "BUILD_OK $output"
Write-Output "SHA256 $hash"
