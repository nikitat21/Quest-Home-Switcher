param(
    [string]$CatalogPath = (Join-Path $PSScriptRoot 'Official-Home-Library-v1.5.json'),
    [string]$AssetRoot
)

$ErrorActionPreference = 'Stop'

function Assert-Condition([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw $Message }
}

function Get-StreamSha256([System.IO.Stream]$Stream) {
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        return ([BitConverter]::ToString($sha.ComputeHash($Stream))).Replace('-', '')
    } finally {
        $sha.Dispose()
    }
}

$expectedHomes = @{
    'blue-hill-gold-mine' = @('Blue Hill Gold Mine', '93E106293F6773724974E63D4033ED14FAB7B09ABFDCC6ED868030A598D2D22A', 'meta-scene-catalog', '')
    'cascadia' = @('Cascadia', 'A26FE53836D91F30B6E4BF2154213783B7D55B7B2105AE9356F7E6E878A20D73', 'meta-scene-catalog', '')
    'classic-home' = @('Classic Home', 'A58CFECE0F04E66C23641113561058EDB7A7F19362E184A7B136A67D51074C7E', 'meta-scene-catalog', '')
    'crystal-atrium' = @('Crystal Atrium', '911874E0B007B52DCABB45E32D9D6137BCEE13D70251AAB863DA937D13724E98', 'meta-scene-catalog', '')
    'cyber-city' = @('Cyber City', '34288D09200710F6CE53610192FB7EB07E4849ACAA37A85DA9B6C13AE8821662', 'meta-scene-catalog', '')
    'desert-terrace' = @('Desert Terrace', '2509E83C772751604631C58BA92539FA8BE5600D32F8E6365DDBD78C4A291BEA', 'meta-scene-catalog', '')
    'futurescape' = @('Futurescape', '2BDF76BA5DCCDFCE6DB460A3FD2422F6AC617A1F55CF129ABC5D6D0257C7EE53', 'meta-scene-catalog', '')
    'lakeside-peak' = @('Lakeside Peak', '3241C2572373E8F5DD400D4E0B905831C3CD0AD4CE81C5EDEEC1F046887A3683', 'meta-scene-catalog', '')
    'meta-horizon-terrace' = @('Meta Horizon Terrace', 'E150A68CF9CC9D5CF7529F7910DCDC90554D1B4DE1F992FB57CDF688CF4E83FE', 'meta-scene-catalog', '')
    'mogu-hall' = @('Mogu Hall', 'BDA6A7F54BF28FDCCF4B64A2949119CACC62D0B3BB121A6137725C9203E312C2', 'meta-scene-catalog', '')
    'mountain-study' = @('Mountain Study', '22BCDAE4343AE3609B20FC464480CDAF0886DC45BDBE4A408A66573CD0D96F29', 'meta-scene-catalog', '')
    'oceanarium' = @('Oceanarium', '024188D0E0BE1A2788AFC8917419C16FD202231FBB2944C939E725885A7768C5', 'meta-scene-catalog', '')
    'paradiso' = @('Paradiso', '6D07BC945A7FFD48AD9F4C33F8A65307572B0C1F301049A8D093F182DB810CAF', 'meta-scene-catalog', '')
    'polar-village' = @('Polar Village', 'FAA75A4C7404112040C6945DE4DDECF259FE975883BC10A7CA832843F4256FF5', 'meta-scene-catalog', '')
    'quest-dome' = @('Quest Dome', 'A56C71705976A4DB5D3016E2FF6CFE881770014B8E64359DD7F623DBEB4A7133', 'meta-scene-catalog', '')
    'rockquarry' = @('Rockquarry', '82F4C04451A30E0EAB6154EFA686BCD0B6BBFB5E5DD8610353E128F285B5AB95', 'oculus-apps-certificate', '6631568AD0F84212370C35FB8C2E2401D0CE7DF1A0B48D0AA9FD0C83BD4D933B')
    'space-station' = @('Space Station', 'D1ADE12BE1DC6989436EF4ACD766656A614AC5A0AFB144207C10F3A7666BB5B6', 'meta-scene-catalog', '')
    'storybook' = @('Storybook', '34A8D3533E665DF8E920077531DAAE23A1C27116559C3EDE842FFEB64162F0F3', 'meta-scene-catalog', '')
    'studio' = @('Studio', 'C0C710B4A26BB746B61A6CE3B2D0E00730037D38EA0974F88A5C441C037F645F', 'meta-scene-catalog', '')
    'winter-lodge' = @('Winter Lodge', '36FE9E252E9535391C288842B3819891910F139565035E9FDDA1CDE04F6EEF00', 'meta-scene-catalog', '')
}
$readyIds = @(
    'blue-hill-gold-mine', 'classic-home', 'crystal-atrium', 'cyber-city',
    'desert-terrace', 'futurescape', 'lakeside-peak', 'mogu-hall',
    'mountain-study', 'paradiso', 'polar-village', 'quest-dome',
    'rockquarry', 'space-station', 'studio', 'winter-lodge'
)
$topProperties = @('schemaVersion', 'catalogVersion', 'repository', 'releaseTag', 'homes')
$homeProperties = @(
    'id', 'displayName', 'provenance', 'sourceSceneSha256', 'sourceSignerSha256',
    'status', 'statusText', 'installable', 'runtimeTested', 'targetFileName',
    'assetName', 'apkSize', 'apkSha256', 'cookedSceneSha256'
)

Assert-Condition (Test-Path -LiteralPath $CatalogPath -PathType Leaf) 'Catalog file is missing.'
$catalogFile = Get-Item -LiteralPath $CatalogPath
Assert-Condition ($catalogFile.Length -ge 256 -and $catalogFile.Length -le 1MB) 'Catalog size is outside the accepted bounds.'
$catalogHash = (Get-FileHash -LiteralPath $CatalogPath -Algorithm SHA256).Hash
$document = Get-Content -LiteralPath $CatalogPath -Raw -Encoding UTF8 | ConvertFrom-Json

$actualTopProperties = @($document.PSObject.Properties.Name | Sort-Object)
Assert-Condition ((Compare-Object ($topProperties | Sort-Object) $actualTopProperties).Count -eq 0) 'Catalog top-level properties do not match the strict schema.'
Assert-Condition ($document.schemaVersion -is [int] -and [int]$document.schemaVersion -eq 1) 'Unsupported schemaVersion.'
Assert-Condition ([string]$document.catalogVersion -ceq '1.5.0') 'Unsupported catalogVersion.'
Assert-Condition ([string]$document.repository -ceq 'nikitat21/Quest-Home-Switcher') 'Unexpected repository.'
Assert-Condition ([string]$document.releaseTag -ceq 'homes-v1.5.0') 'Unexpected releaseTag.'

$homes = @($document.homes)
Assert-Condition ($homes.Count -eq 20) 'The catalog must contain exactly 20 verified official Meta Homes.'
$seenIds = @{}
$seenNames = @{}
$seenTargets = @{}
$seenAssets = @{}
$seenApkHashes = @{}
$seenCookedHashes = @{}
$availableBytes = 0L

foreach ($entry in $homes) {
    $actualProperties = @($entry.PSObject.Properties.Name | Sort-Object)
    Assert-Condition ((Compare-Object ($homeProperties | Sort-Object) $actualProperties).Count -eq 0) 'A Home entry does not match the strict schema.'
    $id = [string]$entry.id
    Assert-Condition ($id -match '^[a-z0-9]+(?:-[a-z0-9]+)*$' -and $expectedHomes.ContainsKey($id)) "Unexpected Home id: $id"
    Assert-Condition (-not $seenIds.ContainsKey($id)) "Duplicate Home id: $id"
    $seenIds[$id] = $true

    $expected = $expectedHomes[$id]
    $displayName = [string]$entry.displayName
    Assert-Condition ($displayName -ceq $expected[0]) "Unexpected display name for $id."
    Assert-Condition (-not $seenNames.ContainsKey($displayName.ToLowerInvariant())) "Duplicate display name: $displayName"
    $seenNames[$displayName.ToLowerInvariant()] = $true
    Assert-Condition ([string]$entry.sourceSceneSha256 -ceq $expected[1]) "Unexpected source scene hash for $displayName."
    Assert-Condition ([string]$entry.provenance -ceq $expected[2]) "Unexpected provenance for $displayName."
    if ($expected[3]) {
        Assert-Condition ([string]$entry.sourceSignerSha256 -ceq $expected[3]) "Unexpected signer certificate for $displayName."
    } else {
        Assert-Condition ($null -eq $entry.sourceSignerSha256) "A scene-catalog Home must not carry a signer override: $displayName"
    }

    Assert-Condition ($entry.installable -is [bool] -and $entry.runtimeTested -is [bool]) "Boolean safety fields are invalid for $displayName."
    $targetFileName = [string]$entry.targetFileName
    Assert-Condition ($targetFileName -ceq "$displayName.apk" -and $targetFileName -notmatch '[\\/:*?""<>|\x00-\x1F]') "Invalid target file name for $displayName."
    Assert-Condition (-not $seenTargets.ContainsKey($targetFileName.ToLowerInvariant())) "Duplicate target file name: $targetFileName"
    $seenTargets[$targetFileName.ToLowerInvariant()] = $true
    Assert-Condition (-not [string]::IsNullOrWhiteSpace([string]$entry.statusText) -and ([string]$entry.statusText).Length -le 160) "Invalid status text for $displayName."

    $shouldBeReady = $readyIds -contains $id
    if ($shouldBeReady) {
        Assert-Condition ([string]$entry.status -ceq 'available' -and $entry.installable -eq $true -and $entry.runtimeTested -eq $true) "$displayName must be fully available and runtime tested."
        $assetName = [string]$entry.assetName
        Assert-Condition ($assetName -match '^Meta-Home-[A-Za-z0-9-]+-v1\.5\.0\.apk$' -and $assetName.Length -le 120) "Invalid asset name for $displayName."
        Assert-Condition (-not $seenAssets.ContainsKey($assetName.ToLowerInvariant())) "Duplicate asset name: $assetName"
        $seenAssets[$assetName.ToLowerInvariant()] = $true
        Assert-Condition ($entry.apkSize -is [ValueType] -and [int64]$entry.apkSize -ge 1MB -and [int64]$entry.apkSize -le 512MB) "Invalid APK size for $displayName."
        Assert-Condition ([string]$entry.apkSha256 -match '^[0-9A-F]{64}$') "Invalid APK hash for $displayName."
        Assert-Condition ([string]$entry.cookedSceneSha256 -match '^[0-9A-F]{64}$') "Invalid cooked scene hash for $displayName."
        Assert-Condition (-not $seenApkHashes.ContainsKey([string]$entry.apkSha256)) "Duplicate APK hash for $displayName."
        Assert-Condition (-not $seenCookedHashes.ContainsKey([string]$entry.cookedSceneSha256)) "Duplicate cooked scene hash for $displayName."
        $seenApkHashes[[string]$entry.apkSha256] = $true
        $seenCookedHashes[[string]$entry.cookedSceneSha256] = $true
        $availableBytes += [int64]$entry.apkSize
    } else {
        Assert-Condition ([string]$entry.status -ceq 'comingSoon' -and $entry.installable -eq $false -and $entry.runtimeTested -eq $false) "$displayName must fail closed as coming soon."
        Assert-Condition ($null -eq $entry.assetName -and $null -eq $entry.apkSize -and $null -eq $entry.apkSha256 -and $null -eq $entry.cookedSceneSha256) "Coming-soon Home $displayName must not expose downloadable artifact metadata."
    }
}

Assert-Condition ($seenIds.Count -eq $expectedHomes.Count) 'One or more expected official Meta Homes are missing.'
Assert-Condition (-not $seenNames.ContainsKey('meta home (system)')) 'System Footprint must never be distributed by the library.'
Assert-Condition ($availableBytes -le 4GB) 'Available library payload exceeds the safety limit.'

if (-not [string]::IsNullOrWhiteSpace($AssetRoot)) {
    Assert-Condition (Test-Path -LiteralPath $AssetRoot -PathType Container) 'AssetRoot does not exist.'
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $assetRootFull = [System.IO.Path]::GetFullPath((Resolve-Path -LiteralPath $AssetRoot).Path).TrimEnd([IO.Path]::DirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
    foreach ($entry in @($homes | Where-Object { $_.installable -eq $true })) {
        $assetPath = [System.IO.Path]::GetFullPath((Join-Path $assetRootFull ([string]$entry.assetName)))
        Assert-Condition ($assetPath.StartsWith($assetRootFull, [System.StringComparison]::OrdinalIgnoreCase)) "Asset path escapes AssetRoot for $($entry.displayName)."
        Assert-Condition (Test-Path -LiteralPath $assetPath -PathType Leaf) "Missing asset for $($entry.displayName)."
        $assetFile = Get-Item -LiteralPath $assetPath
        Assert-Condition ($assetFile.Length -eq [int64]$entry.apkSize) "Asset size mismatch for $($entry.displayName)."
        Assert-Condition ((Get-FileHash -LiteralPath $assetPath -Algorithm SHA256).Hash -ceq [string]$entry.apkSha256) "Asset hash mismatch for $($entry.displayName)."
        $archive = [System.IO.Compression.ZipFile]::OpenRead($assetPath)
        try {
            $sceneEntry = $archive.GetEntry('assets/scene.zip')
            Assert-Condition ($null -ne $sceneEntry -and $sceneEntry.Length -gt 0) "Scene payload is missing for $($entry.displayName)."
            $stream = $sceneEntry.Open()
            try { $sceneHash = Get-StreamSha256 $stream } finally { $stream.Dispose() }
            Assert-Condition ($sceneHash -ceq [string]$entry.cookedSceneSha256) "Cooked scene hash mismatch for $($entry.displayName)."
        } finally {
            $archive.Dispose()
        }
    }
}

Write-Output "OFFICIAL_HOME_LIBRARY_CATALOG_OK SHA256=$catalogHash ENTRIES=$($homes.Count) READY=$($readyIds.Count) COMING_SOON=$($homes.Count - $readyIds.Count)"
