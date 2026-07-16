param(
    [string]$KeyStore = (Join-Path $env:USERPROFILE 'AndroidKeys\QuestHomeSwitcher.jks')
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$sdk = if ($env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT
} elseif ($env:ANDROID_HOME) {
    $env:ANDROID_HOME
} else {
    Join-Path $env:LOCALAPPDATA 'Android\Sdk'
}
$buildTools = Join-Path $sdk 'build-tools\37.0.0'
$zipalign = Join-Path $buildTools 'zipalign.exe'
$apksigner = Join-Path $buildTools 'apksigner.bat'
$aapt = Join-Path $buildTools 'aapt.exe'
$unsigned = Join-Path $root 'app\build\outputs\apk\release\app-release-unsigned.apk'
$releaseDirectory = Join-Path $root 'release'
$aligned = Join-Path $releaseDirectory 'Quest-Home-Switcher-aligned-unsigned.apk'
$output = Join-Path $releaseDirectory 'Quest-Home-Switcher-v1.8.apk'
$expectedCertificate = '85569394c59b355e850c540ac8b3247e27fbde16235ce20e95bcead337d93f75'

foreach ($required in @($KeyStore, $zipalign, $apksigner, $aapt, (Join-Path $root 'gradlew.bat'))) {
    if (-not (Test-Path -LiteralPath $required)) {
        throw "Required build input is missing: $required"
    }
}

$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
if (-not $env:ANDROID_USER_HOME) {
    $env:ANDROID_USER_HOME = Join-Path $env:USERPROFILE '.android'
}
if (-not $env:GRADLE_USER_HOME) {
    $env:GRADLE_USER_HOME = Join-Path $env:USERPROFILE '.gradle'
}

Push-Location $root
try {
    # Do not run Gradle's clean task here: Android lint can keep migrated JARs
    # memory-mapped on Windows even after a successful verification build.
    & .\gradlew.bat :app:assembleRelease
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $unsigned)) {
        throw 'Gradle did not create the unsigned release APK.'
    }

    New-Item -ItemType Directory -Path $releaseDirectory -Force | Out-Null
    & $zipalign -f 4 $unsigned $aligned
    if ($LASTEXITCODE -ne 0) { throw 'zipalign failed.' }

    Write-Host 'Enter the permanent Quest Home Switcher keystore password when apksigner asks.'
    & $apksigner sign `
        --ks $KeyStore `
        --ks-key-alias questhomeswitcher `
        --v1-signing-enabled false `
        --v2-signing-enabled true `
        --v3-signing-enabled true `
        --v4-signing-enabled false `
        --out $output `
        $aligned
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $output)) {
        throw 'APK signing failed.'
    }

    $previousErrorAction = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        # JDK 25 writes a native-access warning to stderr although apksigner succeeds.
        $verification = (& $apksigner verify --verbose --print-certs $output 2>&1) -join "`n"
        $verificationExitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousErrorAction
    }
    if ($verificationExitCode -ne 0) { throw "APK signature verification failed.`n$verification" }
    if ($verification -notmatch [regex]::Escape($expectedCertificate)) {
        throw 'The APK was not signed by the permanent Quest Home Switcher certificate.'
    }

    $badging = (& $aapt dump badging $output 2>&1) -join "`n"
    if ($LASTEXITCODE -ne 0 -or $badging -notmatch "package: name='io\.github\.nikitat21\.questhomeswitcher'") {
        throw 'The signed APK has an unexpected package ID.'
    }
    if ($badging -notmatch "versionCode='16'" -or $badging -notmatch "versionName='1\.8'") {
        throw 'The signed APK has an unexpected version.'
    }

    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $output).Hash
    Write-Output "BUILD_OK $output"
    Write-Output "SHA256 $hash"
    Write-Output "CERT_SHA256 $expectedCertificate"
} finally {
    Remove-Item -LiteralPath $aligned -Force -ErrorAction SilentlyContinue
    Pop-Location
}
