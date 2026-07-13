param(
    [switch]$SelfTest,
    [string]$DistributionRoot
)

$ErrorActionPreference = 'Stop'
$script:ToolRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$script:DistributionRoot = if ($DistributionRoot) {
    [System.IO.Path]::GetFullPath($DistributionRoot)
} else {
    $script:ToolRoot
}
$script:RuntimeRoot = Join-Path $env:LOCALAPPDATA 'QuestHomeSwitcherSetup'
$script:AdbPath = $null
$script:UiInitialized = $false
$script:SetupComplete = $false

$script:ShizukuPackage = 'moe.shizuku.privileged.api'
$script:ShizukuActivity = 'moe.shizuku.privileged.api/moe.shizuku.manager.MainActivity'
$script:SwitcherPackage = 'io.github.nikitat21.questhomeswitcher'
$script:SwitcherActivity = 'io.github.nikitat21.questhomeswitcher/.MainActivity'
$script:SwitcherApk = Join-Path $script:DistributionRoot 'Quest-Home-Switcher.apk'
$script:ExpectedSwitcherVersionCode = 13
$script:ExpectedSwitcherVersionName = '1.0'
$script:ExpectedSwitcherSha256 = '57398CD94654694FDCFF01B7C73F190D4B8E6F96234CD5BDB7FC21C3328A3F17'
$script:HomePackageIdentifier = 'com.meta.shell.env.footprint.haven2025'
$script:HomeImportDirectory = '/sdcard/Download/Quest Homes'
$script:HomeImportHistoryFile = Join-Path $script:RuntimeRoot 'home-import-directory.txt'
$script:OfficialHomeSceneCatalog = @{
    '33139012EDB50FF352DA4EC1BF357DABB03422F48BD418DADF0093911F076E91' = 'Blue Hill Gold Mine'
    '93E106293F6773724974E63D4033ED14FAB7B09ABFDCC6ED868030A598D2D22A' = 'Blue Hill Gold Mine'
    '0CD9414EBECD731E9F3B10BCB0803E1F1F4A4F1EF9155D4770B96505FB01D59D' = 'Futurescape'
    'F4AB2140838BA626A29E5C0CF3A74195DD24E70A71FFECA6D6C7DEE52A0E4E92' = 'Futurescape'
    '2BDF76BA5DCCDFCE6DB460A3FD2422F6AC617A1F55CF129ABC5D6D0257C7EE53' = 'Futurescape'
    '3241C2572373E8F5DD400D4E0B905831C3CD0AD4CE81C5EDEEC1F046887A3683' = 'Lakeside Peak'
    '1C1D3F7857E868F793AF3BBDD663B2DCBCC535E4436EDE5677FB1FC3563DBE46' = 'Lakeside Peak'
    '1738D80E3A648F0EF73E304FD423B48F68A6C946B278DEAF9D19BF1CE1DD95DE' = 'Crystal Atrium'
    '3C16539B821D57C5704A56296B4C6CA22CFBE729BCC792C27B956718657857EC' = 'Crystal Atrium'
    '49B5AB47D0758B98E6CBEAD24587A35FE2C8EA7A432CAA643C8FF79218D69A17' = 'Crystal Atrium'
    '911874E0B007B52DCABB45E32D9D6137BCEE13D70251AAB863DA937D13724E98' = 'Crystal Atrium'
    '424BA6214BEA34F9C3132F2A1AE5ED3B6EFE4E216960212204AE32592C7405C6' = 'Polar Village'
    '11DDC60749C7EA85762C0674A9AD981C2A23B1084C9AC134010FE18D664D6BE4' = 'Polar Village'
    'FAA75A4C7404112040C6945DE4DDECF259FE975883BC10A7CA832843F4256FF5' = 'Polar Village'
    'E150A68CF9CC9D5CF7529F7910DCDC90554D1B4DE1F992FB57CDF688CF4E83FE' = 'Meta Horizon Terrace'
    '412FFD70126CAE8DACA3907A3978EB8734B6C48EC3B89509979CAD70A3A681B6' = 'Meta Horizon Terrace'
    '34A8D3533E665DF8E920077531DAAE23A1C27116559C3EDE842FFEB64162F0F3' = 'Storybook'
    'FFC143F40A80B9A10B02AF110E07BFAFB1B65EF40C44054D8B3DA67010697475' = 'Paradiso'
    '3B6211A19E9BFCC5114957B44C30C4647F0A298EA68EAB965257D42E8C3C4130' = 'Paradiso'
    '6D07BC945A7FFD48AD9F4C33F8A65307572B0C1F301049A8D093F182DB810CAF' = 'Paradiso'
    '024188D0E0BE1A2788AFC8917419C16FD202231FBB2944C939E725885A7768C5' = 'Oceanarium'
    'B4B6367659321E366DC32A368A001C1B8B6DBDDBB626A5909C1061E066E7501E' = 'Cascadia'
    '04A3E64076B297CF7A2F58206BB0C30890863D6CF8CA9F4053E45EADA035E78C' = 'Cascadia'
    'A26FE53836D91F30B6E4BF2154213783B7D55B7B2105AE9356F7E6E878A20D73' = 'Cascadia'
    '34288D09200710F6CE53610192FB7EB07E4849ACAA37A85DA9B6C13AE8821662' = 'Cyber City'
    '3C8D7CF57DBF6949A486F2FED4B6160E3FB3AEC96DBFE2BCC68594122A3560E9' = 'Cyber City'
    '0202C7BC3BF03102E1093211DE17BF96F65F3D8794E63C662E4B34A61C3A5E3F' = 'Cyber City'
    '8E105686424230AE534FD8CC43E6E43C5005AF551356DB3EECD9FFD131BB37E4' = 'Quest Dome'
    '3CA225FC87E55DF7C5DE8BA68596D6B8104AF8768D488112AFB57A32F3AEDC47' = 'Quest Dome'
    'A56C71705976A4DB5D3016E2FF6CFE881770014B8E64359DD7F623DBEB4A7133' = 'Quest Dome'
    '7B125CAD02F8D218D4B98D30BD9F33C0339E0AA53BC45852642598E5DE80D140' = 'Quest Dome'
    '8F3546EE7A8981587A9EB0B670AB3AD057BE88A56276980CFF3FEF43D6BBC668' = 'Mogu Hall'
    'BDA6A7F54BF28FDCCF4B64A2949119CACC62D0B3BB121A6137725C9203E312C2' = 'Mogu Hall'
    '2D688323FDEBF6280291BBF8C65CD2A2DB87CC00B86499347A8DA7C9C066DDCB' = 'Studio'
    'C0C710B4A26BB746B61A6CE3B2D0E00730037D38EA0974F88A5C441C037F645F' = 'Studio'
    'FACFF2C0E16D1196A49D5EA55E2A9DE7856807274B438C7CDBB9D5AD7EC53E91' = 'Mountain Study'
    '22BCDAE4343AE3609B20FC464480CDAF0886DC45BDBE4A408A66573CD0D96F29' = 'Mountain Study'
    '8EE66163A7C99204F6613F9C42679CB397C36B2D79E37B0DB0AA23723D263A3E' = 'Classic Home'
    'A58CFECE0F04E66C23641113561058EDB7A7F19362E184A7B136A67D51074C7E' = 'Classic Home'
    'F2C091140A5F0B8A98D7D8DB1368455E6DE4048AD780103760E924ACEA72AD43' = 'Classic Home'
    '36FE9E252E9535391C288842B3819891910F139565035E9FDDA1CDE04F6EEF00' = 'Winter Lodge'
    '2509E83C772751604631C58BA92539FA8BE5600D32F8E6365DDBD78C4A291BEA' = 'Desert Terrace'
    'D1ADE12BE1DC6989436EF4ACD766656A614AC5A0AFB144207C10F3A7666BB5B6' = 'Space Station'
    '8503FC8D849068116C313B761516DC32FC87C6B452F3099E07BD5BFE6A376EBD' = 'Meta Home (System)'
}

function Resolve-ExistingDirectory([string]$Path) {
    if ([string]::IsNullOrWhiteSpace($Path)) { return $null }
    try {
        $expanded = [Environment]::ExpandEnvironmentVariables($Path.Trim())
        if (-not [System.IO.Path]::IsPathRooted($expanded)) { return $null }
        $item = Get-Item -LiteralPath ([System.IO.Path]::GetFullPath($expanded)) -Force -ErrorAction Stop
        if ($item.PSIsContainer) { return $item.FullName }
    } catch {}
    return $null
}

function Get-DownloadsDirectory {
    try {
        $key = Get-Item -LiteralPath 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Explorer\User Shell Folders' -ErrorAction Stop
        $raw = $key.GetValue('{374DE290-123F-4565-9164-39C4925E467B}', $null, [Microsoft.Win32.RegistryValueOptions]::DoNotExpandEnvironmentNames)
        $resolved = Resolve-ExistingDirectory ([Environment]::ExpandEnvironmentVariables([string]$raw))
        if ($resolved) { return $resolved }
    } catch {}

    $profileDownloads = if ($env:USERPROFILE) { Join-Path $env:USERPROFILE 'Downloads' } else { $null }
    return Resolve-ExistingDirectory $profileDownloads
}

function Get-DefaultHomeImportSearchRoots {
    $roots = New-Object System.Collections.Generic.List[string]
    $seen = New-Object 'System.Collections.Generic.HashSet[string]' ([System.StringComparer]::OrdinalIgnoreCase)
    $addRoot = {
        param([string]$Candidate)
        $resolved = Resolve-ExistingDirectory $Candidate
        if ($resolved -and $seen.Add($resolved)) { $roots.Add($resolved) }
    }

    foreach ($seed in @($script:DistributionRoot, $script:ToolRoot)) {
        $current = Resolve-ExistingDirectory $seed
        for ($level = 0; $current -and $level -lt 8; $level++) {
            & $addRoot $current
            $parent = Split-Path -Parent $current
            if (-not $parent -or $parent -eq $current) { break }
            $current = $parent
        }
    }

    & $addRoot (Get-DownloadsDirectory)
    & $addRoot ([Environment]::GetFolderPath([Environment+SpecialFolder]::DesktopDirectory))
    & $addRoot ([Environment]::GetFolderPath([Environment+SpecialFolder]::MyDocuments))
    & $addRoot $env:USERPROFILE

    try {
        foreach ($drive in [System.IO.DriveInfo]::GetDrives()) {
            if ($drive.IsReady -and $drive.DriveType -eq [System.IO.DriveType]::Fixed) {
                & $addRoot $drive.RootDirectory.FullName
            }
        }
    } catch {}

    return [string[]]$roots.ToArray()
}

function Find-HomeEditorCookedDirectory([string[]]$Roots) {
    $knownRelativePaths = @(
        'cooked',
        'Quest Home Editor\cooked',
        'Quest-Home-Editor\cooked',
        'custom home tool\cooked',
        'Custom Home Tool\cooked'
    )

    foreach ($root in $Roots) {
        $resolvedRoot = Resolve-ExistingDirectory $root
        if (-not $resolvedRoot) { continue }
        if ([System.IO.Path]::GetFileName($resolvedRoot.TrimEnd([System.IO.Path]::DirectorySeparatorChar)) -ieq 'cooked') {
            return $resolvedRoot
        }
        foreach ($relativePath in $knownRelativePaths) {
            $candidate = Resolve-ExistingDirectory (Join-Path $resolvedRoot $relativePath)
            if ($candidate) { return $candidate }
        }
    }

    # Detect versioned or renamed editor folders without recursively scanning a whole drive.
    foreach ($root in $Roots) {
        $resolvedRoot = Resolve-ExistingDirectory $root
        if (-not $resolvedRoot) { continue }
        try {
            foreach ($child in Get-ChildItem -LiteralPath $resolvedRoot -Directory -Force -ErrorAction Stop) {
                if ($child.Attributes -band [System.IO.FileAttributes]::ReparsePoint) { continue }
                if ($child.Name -notmatch '(?i)(quest.*home.*editor|custom.*home.*tool|quest.*home.*tool)') { continue }
                $candidate = Resolve-ExistingDirectory (Join-Path $child.FullName 'cooked')
                if ($candidate) { return $candidate }
            }
        } catch {}
    }
    return $null
}

function Read-HomeImportDirectory([string]$HistoryFile = $script:HomeImportHistoryFile) {
    if ([string]::IsNullOrWhiteSpace($HistoryFile) -or -not (Test-Path -LiteralPath $HistoryFile -PathType Leaf)) { return $null }
    try {
        $value = [System.IO.File]::ReadAllText($HistoryFile).Trim()
        if ($value.Length -gt 32768) { return $null }
        return Resolve-ExistingDirectory $value
    } catch {
        return $null
    }
}

function Save-HomeImportDirectory([string]$SelectedPath, [string]$HistoryFile = $script:HomeImportHistoryFile) {
    try {
        if ([string]::IsNullOrWhiteSpace($SelectedPath) -or [string]::IsNullOrWhiteSpace($HistoryFile)) { return $false }
        $selectedItem = Get-Item -LiteralPath $SelectedPath -Force -ErrorAction Stop
        $directory = if ($selectedItem.PSIsContainer) { $selectedItem.FullName } else { $selectedItem.DirectoryName }
        $directory = Resolve-ExistingDirectory $directory
        if (-not $directory) { return $false }

        $historyParent = Split-Path -Parent ([System.IO.Path]::GetFullPath($HistoryFile))
        if (-not $historyParent) { return $false }
        New-Item -ItemType Directory -Path $historyParent -Force | Out-Null
        $temporaryFile = "$HistoryFile.$PID.tmp"
        try {
            [System.IO.File]::WriteAllText($temporaryFile, $directory, (New-Object System.Text.UTF8Encoding($false)))
            Move-Item -LiteralPath $temporaryFile -Destination $HistoryFile -Force
        } finally {
            if (Test-Path -LiteralPath $temporaryFile) { Remove-Item -LiteralPath $temporaryFile -Force -ErrorAction SilentlyContinue }
        }
        return $true
    } catch {
        # A local convenience preference must never block Home validation or upload.
        return $false
    }
}

function Get-HomeImportInitialDirectory(
    [string[]]$SearchRoots,
    [string]$HistoryFile = $script:HomeImportHistoryFile,
    [string]$DownloadsPath
) {
    $remembered = Read-HomeImportDirectory $HistoryFile
    if ($remembered -and [System.IO.Path]::GetFileName($remembered.TrimEnd([System.IO.Path]::DirectorySeparatorChar)) -ieq 'cooked') {
        return $remembered
    }

    if (-not $PSBoundParameters.ContainsKey('SearchRoots')) {
        $SearchRoots = Get-DefaultHomeImportSearchRoots
    }
    $cooked = Find-HomeEditorCookedDirectory $SearchRoots
    if ($cooked) { return $cooked }
    if ($remembered) { return $remembered }

    if (-not $PSBoundParameters.ContainsKey('DownloadsPath')) {
        $DownloadsPath = Get-DownloadsDirectory
    }
    $downloads = Resolve-ExistingDirectory $DownloadsPath
    if ($downloads) { return $downloads }

    foreach ($root in $SearchRoots) {
        $fallback = Resolve-ExistingDirectory $root
        if ($fallback) { return $fallback }
    }
    return Resolve-ExistingDirectory $env:USERPROFILE
}

function Find-Adb {
    $command = Get-Command adb.exe -ErrorAction SilentlyContinue
    $candidates = @(
        $(if ($command) { $command.Source }),
        (Join-Path $script:RuntimeRoot 'platform-tools\adb.exe'),
        (Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'),
        (Join-Path $env:ProgramFiles 'SideQuest\resources\app.asar.unpacked\build\platform-tools\adb.exe'),
        (Join-Path ${env:ProgramFiles(x86)} 'SideQuest\resources\app.asar.unpacked\build\platform-tools\adb.exe')
    ) | Where-Object { $_ -and (Test-Path -LiteralPath $_) }
    return $candidates | Select-Object -First 1
}

function Install-PlatformTools {
    $zip = Join-Path $script:RuntimeRoot 'platform-tools.zip'
    $extract = Join-Path $script:RuntimeRoot 'platform-tools'
    New-Item -ItemType Directory -Force -Path $script:RuntimeRoot | Out-Null
    Invoke-WebRequest -UseBasicParsing -Uri 'https://dl.google.com/android/repository/platform-tools-latest-windows.zip' -OutFile $zip
    if (Test-Path -LiteralPath $extract) {
        Remove-Item -LiteralPath $extract -Recurse -Force
    }
    Expand-Archive -LiteralPath $zip -DestinationPath $script:RuntimeRoot -Force
    Remove-Item -LiteralPath $zip -Force
    $adb = Join-Path $extract 'adb.exe'
    if (-not (Test-Path -LiteralPath $adb)) {
        throw 'Google Platform Tools could not be installed.'
    }
    return $adb
}

function Invoke-Adb([string[]]$Arguments, [switch]$AllowFailure) {
    if (-not $script:AdbPath) {
        throw 'ADB is not ready.'
    }
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = & $script:AdbPath @Arguments 2>&1 | Out-String
        $exit = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    if (-not $AllowFailure -and $exit -ne 0) {
        throw "ADB error:`n$output"
    }
    return [pscustomobject]@{
        ExitCode = $exit
        Output = $output.Trim()
    }
}

function Get-QuestState {
    if (-not $script:AdbPath) {
        return [pscustomobject]@{ State='no-adb'; Serial=''; Detail='Google Platform Tools are not installed yet.' }
    }
    $result = Invoke-Adb @('devices') -AllowFailure
    $rows = @($result.Output -split "`r?`n" | Where-Object { $_ -match '^\S+\s+(device|unauthorized|offline)$' })
    if ($rows.Count -gt 1) {
        return [pscustomobject]@{ State='multiple'; Serial=''; Detail='More than one Android device is connected.' }
    }
    if (-not $rows) {
        return [pscustomobject]@{ State='missing'; Serial=''; Detail='No Quest found over USB.' }
    }
    $parts = $rows[0] -split '\s+'
    return [pscustomobject]@{ State=$parts[1]; Serial=$parts[0]; Detail=$rows[0] }
}

function Get-ReadyQuest([scriptblock]$Status) {
    & $Status 'Preparing Google Platform Tools...' 8
    $script:AdbPath = Find-Adb
    if (-not $script:AdbPath) {
        & $Status 'Downloading Google Platform Tools from Google...' 14
        $script:AdbPath = Install-PlatformTools
    }
    Invoke-Adb @('start-server') | Out-Null
    & $Status 'Looking for your Quest...' 20
    $quest = Get-QuestState
    switch ($quest.State) {
        'device' { return $quest }
        'unauthorized' { throw 'Put on the Quest, allow USB debugging, and select Always allow from this computer. Then try again.' }
        'offline' { throw 'The Quest is offline. Unplug the USB cable, reconnect it, and try again.' }
        'multiple' { throw 'More than one Android device is connected. Disconnect the other device and try again.' }
        default { throw 'No Quest found. Connect it over USB and make sure USB debugging is enabled.' }
    }
}

function Get-PackageInfo([string]$Serial, [string]$PackageName) {
    $path = Invoke-Adb @('-s', $Serial, 'shell', 'pm', 'path', $PackageName) -AllowFailure
    if ($path.ExitCode -ne 0 -or $path.Output -notmatch '(?m)^package:') {
        return [pscustomobject]@{
            Installed = $false
            PackageName = $PackageName
            VersionCode = 0
            VersionName = ''
            LegacyNativeLibraryDir = ''
        }
    }
    $dump = Invoke-Adb @('-s', $Serial, 'shell', 'dumpsys', 'package', $PackageName) -AllowFailure
    $versionCodeMatch = [regex]::Match($dump.Output, '(?m)^\s*versionCode=(\d+)')
    $versionNameMatch = [regex]::Match($dump.Output, '(?m)^\s*versionName=([^\r\n]+)')
    $libraryMatch = [regex]::Match($dump.Output, '(?m)^\s*legacyNativeLibraryDir=([^\r\n]+)')
    return [pscustomobject]@{
        Installed = $true
        PackageName = $PackageName
        VersionCode = if ($versionCodeMatch.Success) { [int64]$versionCodeMatch.Groups[1].Value } else { 0 }
        VersionName = if ($versionNameMatch.Success) { $versionNameMatch.Groups[1].Value.Trim() } else { '' }
        LegacyNativeLibraryDir = if ($libraryMatch.Success) { $libraryMatch.Groups[1].Value.Trim() } else { '' }
    }
}

function Get-ShizukuState([string]$Serial) {
    $package = Get-PackageInfo $Serial $script:ShizukuPackage
    if (-not $package.Installed) {
        return [pscustomobject]@{
            State = 'Missing'
            Version = ''
            VersionCode = 0
            Pid = ''
            Detail = 'Shizuku is not installed.'
            Package = $package
        }
    }

    $pidResult = Invoke-Adb @('-s', $Serial, 'shell', 'pidof', 'shizuku_server') -AllowFailure
    $pidMatch = [regex]::Match($pidResult.Output, '(?m)\b(\d+)\b')
    if (-not $pidMatch.Success) {
        return [pscustomobject]@{
            State = 'InstalledStopped'
            Version = $package.VersionName
            VersionCode = $package.VersionCode
            Pid = ''
            Detail = 'Shizuku is installed but its server is not running.'
            Package = $package
        }
    }

    $serverPid = $pidMatch.Groups[1].Value
    $commandLine = Invoke-Adb @('-s', $Serial, 'shell', 'cat', "/proc/$serverPid/cmdline") -AllowFailure
    $processStatus = Invoke-Adb @('-s', $Serial, 'shell', 'cat', "/proc/$serverPid/status") -AllowFailure
    $uidMatch = [regex]::Match($processStatus.Output, '(?m)^Uid:\s*(\d+)')
    $verifiedName = $commandLine.ExitCode -eq 0 -and $commandLine.Output -match '(^|\x00)shizuku_server(\x00|$)'
    $verifiedUid = $uidMatch.Success -and $uidMatch.Groups[1].Value -eq '2000'

    if ($verifiedName -and $verifiedUid) {
        return [pscustomobject]@{
            State = 'Running'
            Version = $package.VersionName
            VersionCode = $package.VersionCode
            Pid = $serverPid
            Detail = "Verified shizuku_server (PID $serverPid, UID 2000)."
            Package = $package
        }
    }

    return [pscustomobject]@{
        State = 'InvalidProcess'
        Version = $package.VersionName
        VersionCode = $package.VersionCode
        Pid = $serverPid
        Detail = 'A process named shizuku_server exists, but its identity could not be verified as Android shell UID 2000.'
        Package = $package
    }
}

function Get-SwitcherState([string]$Serial) {
    $package = Get-PackageInfo $Serial $script:SwitcherPackage
    if (-not $package.Installed) {
        return [pscustomobject]@{
            State = 'Missing'
            Version = ''
            VersionCode = 0
            Detail = 'Quest Home Switcher is not installed.'
        }
    }
    $state = if (
        $package.VersionCode -gt $script:ExpectedSwitcherVersionCode -or
        ($package.VersionCode -eq $script:ExpectedSwitcherVersionCode -and $package.VersionName -eq $script:ExpectedSwitcherVersionName)
    ) { 'Current' } else { 'Outdated' }
    return [pscustomobject]@{
        State = $state
        Version = $package.VersionName
        VersionCode = $package.VersionCode
        Detail = "Installed version $($package.VersionName) (code $($package.VersionCode))."
    }
}

function Get-Shizuku117Apk {
    New-Item -ItemType Directory -Force -Path $script:RuntimeRoot | Out-Null
    $name = 'shizuku-11.7.0.r600.b86c9af-release.apk'
    $apk = Join-Path $script:RuntimeRoot $name
    $expectedHash = 'AC2E6717A5F2D73F1F3CD5933E89A262B28F3EE768A3ECC6638F0390141044E7'
    $valid = $false
    if (Test-Path -LiteralPath $apk) {
        $valid = (Get-FileHash -Algorithm SHA256 -LiteralPath $apk).Hash -eq $expectedHash
    }
    if (-not $valid) {
        Invoke-WebRequest -UseBasicParsing -Uri "https://github.com/RikkaApps/Shizuku/releases/download/v11.7.0/$name" -OutFile $apk
    }
    if ((Get-FileHash -Algorithm SHA256 -LiteralPath $apk).Hash -ne $expectedHash) {
        throw 'The official Shizuku 11.7 download failed its integrity check.'
    }
    return $apk
}

function Get-LatestShizukuApk {
    New-Item -ItemType Directory -Force -Path $script:RuntimeRoot | Out-Null
    $headers = @{ 'User-Agent'='Quest-Home-Switcher-Setup'; 'Accept'='application/vnd.github+json' }
    $release = Invoke-RestMethod -Uri 'https://api.github.com/repos/RikkaApps/Shizuku/releases/latest' -Headers $headers
    $asset = $release.assets | Where-Object { $_.name -match '(?i)^shizuku.*\.apk$' } | Select-Object -First 1
    if (-not $asset) {
        throw 'No APK was found in the official Shizuku release.'
    }
    $apk = Join-Path $script:RuntimeRoot $asset.name
    Invoke-WebRequest -UseBasicParsing -Uri $asset.browser_download_url -OutFile $apk
    if ((Get-Item -LiteralPath $apk).Length -lt 1MB) {
        throw 'The Shizuku download is incomplete.'
    }
    return [pscustomobject]@{ Path=$apk; Version=$release.tag_name; Name=$asset.name }
}

function Enable-AndroidDeveloperOptions([string]$Serial) {
    $before = Invoke-Adb @('-s', $Serial, 'shell', 'settings', 'get', 'global', 'development_settings_enabled') -AllowFailure
    $wasEnabled = $before.ExitCode -eq 0 -and $before.Output.Trim() -eq '1'
    if (-not $wasEnabled) {
        $enable = Invoke-Adb @('-s', $Serial, 'shell', 'settings', 'put', 'global', 'development_settings_enabled', '1') -AllowFailure
        if ($enable.ExitCode -ne 0 -or $enable.Output -match '(?i)error|exception|denied') {
            throw "Android Developer options could not be enabled automatically. On the Quest, open Settings > System > Software Update > Build number and select the build number seven times, then try again.`n`n$($enable.Output)"
        }
    }
    $verify = Invoke-Adb @('-s', $Serial, 'shell', 'settings', 'get', 'global', 'development_settings_enabled') -AllowFailure
    if ($verify.ExitCode -ne 0 -or $verify.Output.Trim() -ne '1') {
        throw 'Android Developer options are still disabled. On the Quest, select the build number seven times, then try again.'
    }
    if (-not $wasEnabled) {
        Invoke-Adb @('-s', $Serial, 'shell', 'am', 'force-stop', 'com.android.settings') -AllowFailure | Out-Null
        Start-Sleep -Milliseconds 500
    }
}

function Open-QuestWirelessDebugging([string]$Serial) {
    Enable-AndroidDeveloperOptions $Serial
    Invoke-Adb @('-s', $Serial, 'shell', 'am', 'force-stop', 'com.android.settings') -AllowFailure | Out-Null
    Start-Sleep -Milliseconds 400

    $activity = 'com.android.settings/.Settings\$DevelopmentSettingsDashboardActivity'
    $open = Invoke-Adb @('-s', $Serial, 'shell', 'am', 'start', '-n', $activity) -AllowFailure
    if ($open.ExitCode -ne 0 -or $open.Output -match '(?i)error|exception|securityexception') {
        $open = Invoke-Adb @('-s', $Serial, 'shell', 'am', 'start', '-a', 'android.settings.APPLICATION_DEVELOPMENT_SETTINGS') -AllowFailure
    }
    if ($open.ExitCode -ne 0 -or $open.Output -match '(?i)error|exception') {
        throw "Developer options could not be opened:`n$($open.Output)"
    }

    Invoke-Adb @('-s', $Serial, 'shell', 'settings', 'put', 'global', 'adb_wifi_enabled', '1') -AllowFailure | Out-Null
    Start-Sleep -Milliseconds 700

    for ($attempt = 0; $attempt -lt 32; $attempt++) {
        Invoke-Adb @('-s', $Serial, 'shell', 'uiautomator', 'dump', '/sdcard/quest_home_switcher_settings.xml') -AllowFailure | Out-Null
        $ui = Invoke-Adb @('-s', $Serial, 'exec-out', 'cat', '/sdcard/quest_home_switcher_settings.xml') -AllowFailure
        if ($ui.Output -match 'text="(?:Use wireless debugging|Debugging[^\"]*WLAN verwenden)"' -and
            $ui.Output -match 'text="(?:Pair device with pairing code|[^\"]*Kopplungscode koppeln)"') {
            return
        }
        $match = [regex]::Match($ui.Output, '<node[^>]*text="(?:Wireless debugging|Drahtlos(?:es|e) Debugging|Debugging[^\"]*WLAN)"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')
        if ($match.Success) {
            $x = [int](([int]$match.Groups[1].Value + [int]$match.Groups[3].Value) / 2)
            $y = [int](([int]$match.Groups[2].Value + [int]$match.Groups[4].Value) / 2)
            Invoke-Adb @('-s', $Serial, 'shell', 'input', 'tap', "$x", "$y") -AllowFailure | Out-Null
            Start-Sleep -Milliseconds 900
            return
        }
        Invoke-Adb @('-s', $Serial, 'shell', 'input', 'swipe', '250', '700', '250', '520', '350') -AllowFailure | Out-Null
        Start-Sleep -Milliseconds 250
    }
    throw 'Developer options are open, but Wireless debugging could not be selected automatically. Select it manually.'
}

function Open-ShizukuManager([string]$Serial) {
    $open = Invoke-Adb @('-s', $Serial, 'shell', 'am', 'start', '-n', $script:ShizukuActivity) -AllowFailure
    if ($open.ExitCode -ne 0 -or $open.Output -match '(?i)error|exception') {
        throw "Shizuku could not be opened:`n$($open.Output)"
    }
}

function Install-PairingVersion([string]$Serial, [scriptblock]$Status) {
    & $Status 'Downloading official Shizuku 11.7...' 30
    $apk = Get-Shizuku117Apk
    & $Status 'Installing the one-time pairing version...' 45
    $install = Invoke-Adb @('-s', $Serial, 'install', $apk) -AllowFailure
    if ($install.ExitCode -ne 0 -or $install.Output -notmatch 'Success') {
        throw "Shizuku 11.7 could not be installed. This setup will never uninstall an existing Shizuku installation automatically.`n`n$($install.Output)"
    }
    & $Status 'Opening Shizuku and Wireless debugging...' 58
    Open-ShizukuManager $Serial
    Open-QuestWirelessDebugging $Serial
}

function Update-ShizukuAfterPairing([string]$Serial, [scriptblock]$Status) {
    & $Status 'Downloading the latest official Shizuku...' 32
    $download = Get-LatestShizukuApk
    & $Status "Updating Shizuku to $($download.Version) without deleting pairing data..." 54
    $install = Invoke-Adb @('-s', $Serial, 'install', '-r', $download.Path) -AllowFailure
    if ($install.ExitCode -ne 0 -or $install.Output -notmatch 'Success') {
        throw "Shizuku could not be updated. Pairing data was not deleted.`n`n$($install.Output)"
    }
}

function Try-StartInstalledShizuku([string]$Serial, [scriptblock]$Status) {
    $before = Get-ShizukuState $Serial
    if ($before.State -eq 'Running') {
        return $before
    }
    if ($before.State -ne 'InstalledStopped') {
        return $before
    }
    $libraryRoot = $before.Package.LegacyNativeLibraryDir
    if (-not $libraryRoot) {
        return $before
    }

    & $Status 'Trying the installed Shizuku native starter...' 64
    $starter = "$libraryRoot/arm64/libshizuku.so"
    Invoke-Adb @('-s', $Serial, 'shell', $starter) -AllowFailure | Out-Null
    Start-Sleep -Seconds 2
    return Get-ShizukuState $Serial
}

function Test-SwitcherPayload {
    if (-not (Test-Path -LiteralPath $script:SwitcherApk)) {
        throw 'The embedded Quest Home Switcher payload is missing. Download a fresh setup EXE.'
    }
    $file = Get-Item -LiteralPath $script:SwitcherApk
    if ($file.Length -lt 1MB) {
        throw 'Quest-Home-Switcher.apk is incomplete.'
    }
    $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $script:SwitcherApk).Hash
    if ($hash -ne $script:ExpectedSwitcherSha256) {
        throw 'Quest-Home-Switcher.apk failed its integrity check. Download a fresh setup package.'
    }
    return $hash
}

function Invoke-SwitcherSignatureMigration([string]$Serial, [scriptblock]$Status, [scriptblock]$ConfirmMigration) {
    if (-not $ConfirmMigration -or -not (& $ConfirmMigration)) {
        throw 'Switcher replacement was canceled. The existing Quest Home Switcher remains installed and was not changed.'
    }

    & $Status 'Removing only the incompatible old Quest Home Switcher...' 80
    $remove = Invoke-Adb @('-s', $Serial, 'uninstall', $script:SwitcherPackage) -AllowFailure
    if ($remove.ExitCode -ne 0 -or $remove.Output -notmatch '(?m)^Success\s*$') {
        throw "Only Quest Home Switcher was selected for removal, but Android did not complete it. Shizuku was not touched.`n`n$($remove.Output)"
    }

    & $Status 'Installing the permanently signed Quest Home Switcher...' 85
    $install = Invoke-Adb @('-s', $Serial, 'install', $script:SwitcherApk) -AllowFailure
    if ($install.ExitCode -ne 0 -or $install.Output -notmatch '(?m)^Success\s*$') {
        throw "The incompatible old Switcher was removed, but the new Quest Home Switcher could not be installed. Run this setup again. Shizuku, its pairing, and Home APK files were not changed.`n`n$($install.Output)"
    }
}

function Ensure-SwitcherInstalled([string]$Serial, [scriptblock]$Status, [scriptblock]$ConfirmMigration) {
    & $Status 'Checking the Quest Home Switcher package...' 74
    Test-SwitcherPayload | Out-Null
    $state = Get-SwitcherState $Serial
    if ($state.State -eq 'Missing' -or $state.State -eq 'Outdated') {
        & $Status 'Installing Quest Home Switcher...' 82
        $install = Invoke-Adb @('-s', $Serial, 'install', '-r', $script:SwitcherApk) -AllowFailure
        if ($install.ExitCode -ne 0 -or $install.Output -notmatch 'Success') {
            if ($install.Output -match 'INSTALL_FAILED_UPDATE_INCOMPATIBLE') {
                Invoke-SwitcherSignatureMigration $Serial $Status $ConfirmMigration
            } else {
                throw "Quest Home Switcher could not be installed:`n`n$($install.Output)"
            }
        }
    }

    & $Status 'Verifying Quest Home Switcher...' 90
    $verified = Get-SwitcherState $Serial
    if ($verified.State -ne 'Current') {
        throw 'Quest Home Switcher installation could not be verified.'
    }
    return $verified
}

function Start-Switcher([string]$Serial, [scriptblock]$Status) {
    & $Status 'Opening Quest Home Switcher...' 96
    $start = Invoke-Adb @('-s', $Serial, 'shell', 'am', 'start', '-W', '-n', $script:SwitcherActivity) -AllowFailure
    if ($start.ExitCode -ne 0 -or $start.Output -match '(?i)error|exception|unable to resolve') {
        throw "Quest Home Switcher was installed, but it could not be opened:`n`n$($start.Output)"
    }
}

Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem

function Test-ByteSequence([byte[]]$Buffer, [byte[]]$Needle) {
    if (-not $Buffer -or -not $Needle -or $Needle.Length -gt $Buffer.Length) {
        return $false
    }
    $lastStart = $Buffer.Length - $Needle.Length
    for ($offset = 0; $offset -le $lastStart; $offset++) {
        if ($Buffer[$offset] -ne $Needle[0]) { continue }
        $matches = $true
        for ($index = 1; $index -lt $Needle.Length; $index++) {
            if ($Buffer[$offset + $index] -ne $Needle[$index]) {
                $matches = $false
                break
            }
        }
        if ($matches) { return $true }
    }
    return $false
}

function Read-ZipEntryBytes([System.IO.Compression.ZipArchiveEntry]$Entry) {
    $entryStream = $Entry.Open()
    $memory = New-Object System.IO.MemoryStream
    try {
        $entryStream.CopyTo($memory)
        return ,$memory.ToArray()
    } finally {
        $entryStream.Dispose()
        $memory.Dispose()
    }
}

function Get-ZipEntrySha256([System.IO.Compression.ZipArchiveEntry]$Entry) {
    $entryStream = $Entry.Open()
    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    try {
        $hashBytes = $sha256.ComputeHash($entryStream)
        return ([System.BitConverter]::ToString($hashBytes)).Replace('-', '')
    } finally {
        $sha256.Dispose()
        $entryStream.Dispose()
    }
}

function ConvertTo-ReadableHomeName([string]$Path) {
    $name = [System.IO.Path]::GetFileNameWithoutExtension($Path)
    $name = [regex]::Replace($name, '(?i)(?:[._ -]+)(?:no[._ -]*root(?:[._ -]*spoof(?:ed)?)?|root[._ -]*spoof(?:ed)?)$', '')
    $name = [regex]::Replace($name, '^\d{4}-\d{2}-\d{2}T\d{2}[-:]\d{2}[-:]\d{2}(?:\.\d+)?Z[._ -]*(?:\d+[._ -]*)?', '', 'IgnoreCase')
    $name = [regex]::Replace($name, '[._-]+', ' ')
    $name = [regex]::Replace($name, '\s+', ' ').Trim()
    if (-not $name) { return 'Quest Home' }

    $culture = [System.Globalization.CultureInfo]::InvariantCulture.TextInfo
    $words = foreach ($word in ($name -split ' ')) {
        if ($word -cmatch '^(?=.*[A-Z].*[A-Z])(?=.*[A-Za-z])[A-Za-z0-9]+$' -or $word -match '\d') {
            $word
        } else {
            $culture.ToTitleCase($word.ToLowerInvariant())
        }
    }
    return ($words -join ' ')
}

function Test-HomeApk([string]$Path) {
    $name = [System.IO.Path]::GetFileName($Path)
    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return [pscustomobject]@{ Valid=$false; Path=$Path; Name=$name; Reason='File not found.' }
    }
    if (-not [string]::Equals([System.IO.Path]::GetExtension($Path), '.apk', [System.StringComparison]::OrdinalIgnoreCase)) {
        return [pscustomobject]@{ Valid=$false; Path=$Path; Name=$name; Reason='Only .apk files are accepted.' }
    }

    $archive = $null
    try {
        $archive = [System.IO.Compression.ZipFile]::OpenRead($Path)
        $scene = $archive.GetEntry('assets/scene.zip')
        if (-not $scene) {
            return [pscustomobject]@{ Valid=$false; Path=$Path; Name=$name; Reason='Missing assets/scene.zip.' }
        }
        $sceneHash = Get-ZipEntrySha256 $scene
        $manifest = $archive.GetEntry('AndroidManifest.xml')
        if (-not $manifest) {
            return [pscustomobject]@{ Valid=$false; Path=$Path; Name=$name; Reason='Missing AndroidManifest.xml.' }
        }
        if ($manifest.Length -gt 16MB) {
            return [pscustomobject]@{ Valid=$false; Path=$Path; Name=$name; Reason='AndroidManifest.xml is unexpectedly large.' }
        }

        $manifestBytes = Read-ZipEntryBytes $manifest
        $utf8Needle = [System.Text.Encoding]::UTF8.GetBytes($script:HomePackageIdentifier)
        $utf16Needle = [System.Text.Encoding]::Unicode.GetBytes($script:HomePackageIdentifier)
        $packageFound = (Test-ByteSequence $manifestBytes $utf8Needle) -or (Test-ByteSequence $manifestBytes $utf16Needle)
        if (-not $packageFound) {
            return [pscustomobject]@{ Valid=$false; Path=$Path; Name=$name; Reason="Manifest does not contain $($script:HomePackageIdentifier)." }
        }

        $officialName = $script:OfficialHomeSceneCatalog[$sceneHash]
        $displayName = if ($officialName) { $officialName } else { ConvertTo-ReadableHomeName $Path }
        $suggestedTargetName = ConvertTo-SafeApkName "$displayName.apk"
        $kind = if ($officialName) { 'Official Meta Home' } else { 'Custom Home' }
        return [pscustomobject]@{
            Valid = $true
            Path = $Path
            Name = $name
            Reason = 'Compatible Quest Home APK.'
            SceneHash = $sceneHash
            DisplayName = $displayName
            SuggestedTargetName = $suggestedTargetName
            Kind = $kind
            KnownOfficial = [bool]$officialName
        }
    } catch {
        return [pscustomobject]@{ Valid=$false; Path=$Path; Name=$name; Reason="Invalid APK/ZIP: $($_.Exception.Message)" }
    } finally {
        if ($archive) { $archive.Dispose() }
    }
}

function ConvertTo-SafeApkName([string]$Path) {
    $baseName = [System.IO.Path]::GetFileNameWithoutExtension($Path)
    $safeBase = [regex]::Replace($baseName, '[^\p{L}\p{Nd} ._()-]+', ' ')
    $safeBase = [regex]::Replace($safeBase, '\s+', ' ').Trim([char[]]' ._-')
    if (-not $safeBase) { $safeBase = 'Quest Home' }
    if ($safeBase.Length -gt 90) { $safeBase = $safeBase.Substring(0, 90).TrimEnd([char[]]' ._-') }
    return "$safeBase.apk"
}

function Add-ApkNameSuffix([string]$SafeName, [int]$Index) {
    if ($Index -lt 2) { return $SafeName }
    $baseName = [System.IO.Path]::GetFileNameWithoutExtension($SafeName)
    $suffix = "-$Index"
    $maxBaseLength = 96 - $suffix.Length - 4
    if ($baseName.Length -gt $maxBaseLength) {
        $baseName = $baseName.Substring(0, $maxBaseLength).TrimEnd([char[]]'._-')
    }
    return "$baseName$suffix.apk"
}

function New-HomeImportReviewItems([object[]]$Accepted) {
    $items = New-Object System.Collections.Generic.List[object]
    $usedNames = @{}
    foreach ($item in $Accepted) {
        $suggested = ConvertTo-SafeApkName $item.SuggestedTargetName
        $candidate = $suggested
        $suffix = 2
        while ($usedNames.ContainsKey($candidate)) {
            $candidate = Add-ApkNameSuffix $suggested $suffix
            $suffix++
        }
        $usedNames[$candidate] = $true
        $hashPreview = if ($item.SceneHash) { $item.SceneHash.Substring(0, [Math]::Min(12, $item.SceneHash.Length)) } else { 'unknown' }
        $items.Add([pscustomobject]@{
            Name = $item.Name
            SourceName = $item.Name
            DetectedName = $item.DisplayName
            Identification = "$($item.Kind) - scene.zip $hashPreview..."
            TargetName = $candidate
            Path = $item.Path
            SceneHash = $item.SceneHash
            KnownOfficial = $item.KnownOfficial
        })
    }
    return [object[]]$items.ToArray()
}

function Test-HomeImportReviewNames([object[]]$Items) {
    $changed = $false
    $usedNames = @{}
    $duplicates = New-Object System.Collections.Generic.List[string]
    foreach ($item in $Items) {
        $cleanName = ConvertTo-SafeApkName ([string]$item.TargetName)
        if ([string]$item.TargetName -cne $cleanName) {
            $item.TargetName = $cleanName
            $changed = $true
        }
        if ($usedNames.ContainsKey($cleanName)) {
            if (-not $duplicates.Contains($cleanName)) { $duplicates.Add($cleanName) }
        } else {
            $usedNames[$cleanName] = $true
        }
    }

    if ($changed) {
        return [pscustomobject]@{ Valid=$false; Changed=$true; Message='Some target names contained unsupported characters or were missing .apk. The cleaned names are now shown; review them once more.' }
    }
    if ($duplicates.Count -gt 0) {
        return [pscustomobject]@{ Valid=$false; Changed=$false; Message="Each selected Home needs a unique target name. Rename: $($duplicates -join ', ')" }
    }
    return [pscustomobject]@{ Valid=$true; Changed=$false; Message='' }
}

function New-HomeImportReviewWindow {
    [xml]$reviewXaml = @'
<Window xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
        xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
        Title="Review Home APK Names" Width="1060" Height="650" MinWidth="900" MinHeight="540"
        WindowStartupLocation="CenterOwner" ResizeMode="CanResize"
        Background="#080D14" Foreground="#F4F7FB" FontFamily="Segoe UI">
  <Window.Resources>
    <Style TargetType="Button">
      <Setter Property="Background" Value="#55E0B5"/><Setter Property="Foreground" Value="#07110E"/>
      <Setter Property="FontWeight" Value="Bold"/><Setter Property="FontSize" Value="14"/>
      <Setter Property="Height" Value="52"/><Setter Property="BorderThickness" Value="0"/>
      <Setter Property="Cursor" Value="Hand"/><Setter Property="Padding" Value="22,0"/>
    </Style>
    <Style x:Key="SecondaryReviewButton" TargetType="Button" BasedOn="{StaticResource {x:Type Button}}">
      <Setter Property="Background" Value="#172230"/><Setter Property="Foreground" Value="#D7E1ED"/>
      <Setter Property="BorderBrush" Value="#314155"/><Setter Property="BorderThickness" Value="1"/>
    </Style>
  </Window.Resources>
  <Grid Margin="34">
    <Grid.RowDefinitions>
      <RowDefinition Height="Auto"/><RowDefinition Height="Auto"/><RowDefinition Height="*"/>
      <RowDefinition Height="Auto"/><RowDefinition Height="Auto"/>
    </Grid.RowDefinitions>
    <StackPanel Grid.Row="0">
      <TextBlock Text="REVIEW BEFORE UPLOAD" Foreground="#55E0B5" FontWeight="Bold" FontSize="13"/>
      <TextBlock Text="Name your Quest Homes" FontSize="30" FontWeight="Bold" Margin="0,8,0,6"/>
      <TextBlock Text="Official Meta Homes are identified by the SHA-256 of assets/scene.zip. Unknown compatible Homes keep a cleaned version of their original file name." Foreground="#AEBBCB" FontSize="15" TextWrapping="Wrap"/>
    </StackPanel>
    <Border Grid.Row="1" Background="#101925" CornerRadius="10" Padding="15,11" Margin="0,20,0,12">
      <TextBlock Text="Edit Target file if you want. Every name is cleaned safely and must be unique. If a file already exists on the Quest, a collision suffix is added without overwriting it." Foreground="#C3D0DE" FontSize="14" TextWrapping="Wrap"/>
    </Border>
    <Border Grid.Row="2" Background="#0D151F" BorderBrush="#263648" BorderThickness="1" CornerRadius="10" Padding="1">
      <DataGrid x:Name="ReviewGrid" AutoGenerateColumns="False" CanUserAddRows="False" CanUserDeleteRows="False"
                CanUserReorderColumns="False" CanUserSortColumns="False" HeadersVisibility="Column"
                GridLinesVisibility="Horizontal" HorizontalGridLinesBrush="#263648"
                Background="#0D151F" Foreground="#EEF4FA" RowBackground="#101925" AlternatingRowBackground="#131E2B"
                BorderThickness="0" RowHeight="62" ColumnHeaderHeight="42" FontSize="14" SelectionMode="Extended">
        <DataGrid.Columns>
          <DataGridTextColumn Header="Selected APK" Binding="{Binding SourceName}" IsReadOnly="True" Width="2*"/>
          <DataGridTextColumn Header="Detected Home" Binding="{Binding DetectedName}" IsReadOnly="True" Width="1.45*"/>
          <DataGridTextColumn Header="Identification" Binding="{Binding Identification}" IsReadOnly="True" Width="1.5*"/>
          <DataGridTextColumn Header="Target file (editable)" Binding="{Binding TargetName, UpdateSourceTrigger=PropertyChanged}" Width="1.7*"/>
        </DataGrid.Columns>
      </DataGrid>
    </Border>
    <TextBlock x:Name="ReviewErrorText" Grid.Row="3" Visibility="Collapsed" Foreground="#FFB4A9" FontSize="14" TextWrapping="Wrap" Margin="2,12,2,0"/>
    <Grid Grid.Row="4" Margin="0,18,0,0">
      <Grid.ColumnDefinitions><ColumnDefinition Width="*"/><ColumnDefinition Width="Auto"/><ColumnDefinition Width="Auto"/></Grid.ColumnDefinitions>
      <TextBlock Grid.Column="0" Text="Nothing is uploaded until you continue." Foreground="#8293A8" VerticalAlignment="Center" FontSize="13"/>
      <Button x:Name="ReviewCancelButton" Grid.Column="1" Content="CANCEL" Style="{StaticResource SecondaryReviewButton}" Width="150" Margin="0,0,12,0"/>
      <Button x:Name="ReviewContinueButton" Grid.Column="2" Content="CONTINUE TO IMPORT" Width="230"/>
    </Grid>
  </Grid>
</Window>
'@
    $reader = New-Object System.Xml.XmlNodeReader $reviewXaml
    return [Windows.Markup.XamlReader]::Load($reader)
}

function Show-HomeImportReview([object[]]$Items, [object]$Owner) {
    $reviewWindow = New-HomeImportReviewWindow
    if ($Owner) { $reviewWindow.Owner = $Owner }
    $reviewGrid = $reviewWindow.FindName('ReviewGrid')
    $errorText = $reviewWindow.FindName('ReviewErrorText')
    $cancelButton = $reviewWindow.FindName('ReviewCancelButton')
    $continueButton = $reviewWindow.FindName('ReviewContinueButton')
    $rows = New-Object 'System.Collections.ObjectModel.ObservableCollection[object]'
    foreach ($item in $Items) { $rows.Add($item) }
    $reviewGrid.ItemsSource = $rows
    $state = [pscustomobject]@{ Confirmed=$false }

    $cancelButton.Add_Click({ $reviewWindow.Close() })
    $continueButton.Add_Click({
        $reviewGrid.CommitEdit([System.Windows.Controls.DataGridEditingUnit]::Cell, $true) | Out-Null
        $reviewGrid.CommitEdit([System.Windows.Controls.DataGridEditingUnit]::Row, $true) | Out-Null
        $currentRows = [object[]]($rows | ForEach-Object { $_ })
        $validation = Test-HomeImportReviewNames $currentRows
        if (-not $validation.Valid) {
            $errorText.Text = $validation.Message
            $errorText.Visibility = 'Visible'
            return
        }
        $state.Confirmed = $true
        $reviewWindow.Close()
    })

    $reviewWindow.ShowDialog() | Out-Null
    if (-not $state.Confirmed) { return $null }
    return [object[]]($rows | ForEach-Object { $_ })
}

function ConvertTo-RemoteShellLiteral([string]$Value) {
    if ($Value.Contains("'")) {
        throw 'Unsafe quote in generated remote path.'
    }
    return "'$Value'"
}

function Ensure-RemoteImportDirectory([string]$Serial) {
    $directory = ConvertTo-RemoteShellLiteral $script:HomeImportDirectory
    $create = Invoke-Adb @('-s', $Serial, 'shell', "mkdir -p $directory") -AllowFailure
    if ($create.ExitCode -ne 0 -or $create.Output -match '(?i)error|exception|denied') {
        throw "The Quest Homes download folder could not be created:`n$($create.Output)"
    }
}

function Test-RemoteFileExists([string]$Serial, [string]$RemotePath) {
    $literal = ConvertTo-RemoteShellLiteral $RemotePath
    $result = Invoke-Adb @('-s', $Serial, 'shell', "if [ -f $literal ]; then printf EXISTS; else printf MISSING; fi") -AllowFailure
    if ($result.ExitCode -ne 0) {
        throw "The existing Quest Home file could not be checked safely:`n$($result.Output)"
    }
    switch ($result.Output.Trim()) {
        'EXISTS' { return $true }
        'MISSING' { return $false }
        default { throw "The Quest returned an unexpected file-check result: $($result.Output)" }
    }
}

function Get-RemoteFileSha256([string]$Serial, [string]$RemotePath) {
    $literal = ConvertTo-RemoteShellLiteral $RemotePath
    $result = Invoke-Adb @('-s', $Serial, 'shell', "command -v sha256sum >/dev/null 2>&1 || exit 127; sha256sum $literal") -AllowFailure
    if ($result.ExitCode -ne 0) { return $null }
    $match = [regex]::Match($result.Output, '(?i)\b([0-9a-f]{64})\b')
    if (-not $match.Success) { return $null }
    return $match.Groups[1].Value.ToUpperInvariant()
}

function Get-RemoteFileSize([string]$Serial, [string]$RemotePath) {
    $literal = ConvertTo-RemoteShellLiteral $RemotePath
    $result = Invoke-Adb @('-s', $Serial, 'shell', "stat -c %s $literal 2>/dev/null || wc -c < $literal") -AllowFailure
    $match = [regex]::Match($result.Output, '(?m)^\s*(\d+)\s*$')
    if ($result.ExitCode -ne 0 -or -not $match.Success) { return $null }
    return [int64]$match.Groups[1].Value
}

function Remove-RemoteImportFile([string]$Serial, [string]$RemotePath) {
    $literal = ConvertTo-RemoteShellLiteral $RemotePath
    Invoke-Adb @('-s', $Serial, 'shell', "rm -f $literal") -AllowFailure | Out-Null
}

function Get-RemoteImportTarget([string]$Serial, [string]$SafeName, [string]$LocalHash) {
    for ($index = 1; $index -le 999; $index++) {
        $candidateName = Add-ApkNameSuffix $SafeName $index
        $candidatePath = "$($script:HomeImportDirectory)/$candidateName"
        if (-not (Test-RemoteFileExists $Serial $candidatePath)) {
            return [pscustomobject]@{ Skip=$false; Name=$candidateName; Path=$candidatePath; ExistingHash='' }
        }

        $existingHash = Get-RemoteFileSha256 $Serial $candidatePath
        if ($existingHash -and $existingHash -eq $LocalHash) {
            return [pscustomobject]@{ Skip=$true; Name=$candidateName; Path=$candidatePath; ExistingHash=$existingHash }
        }
    }
    throw 'More than 999 files with the same safe name already exist in Quest Homes.'
}

function Send-HomeApk([string]$Serial, [string]$LocalPath, [string]$DesiredName) {
    $localFile = Get-Item -LiteralPath $LocalPath
    $localHash = (Get-FileHash -Algorithm SHA256 -LiteralPath $LocalPath).Hash
    $safeName = ConvertTo-SafeApkName $(if ($DesiredName) { $DesiredName } else { $LocalPath })
    $target = Get-RemoteImportTarget $Serial $safeName $localHash
    if ($target.Skip) {
        return [pscustomobject]@{ Status='Skipped'; Local=$localFile.Name; Remote=$target.Name; Verification='Identical SHA-256 already present.' }
    }

    $push = Invoke-Adb @('-s', $Serial, 'push', $LocalPath, $target.Path) -AllowFailure
    if ($push.ExitCode -ne 0 -or $push.Output -match '(?i)failed|error|denied') {
        Remove-RemoteImportFile $Serial $target.Path
        throw "ADB push failed for $($localFile.Name):`n$($push.Output)"
    }

    $remoteSize = Get-RemoteFileSize $Serial $target.Path
    if ($null -eq $remoteSize -or $remoteSize -ne $localFile.Length) {
        Remove-RemoteImportFile $Serial $target.Path
        throw "Size verification failed for $($localFile.Name). The partial Quest copy was removed."
    }

    $remoteHash = Get-RemoteFileSha256 $Serial $target.Path
    if ($remoteHash -and $remoteHash -ne $localHash) {
        Remove-RemoteImportFile $Serial $target.Path
        throw "SHA-256 verification failed for $($localFile.Name). The invalid Quest copy was removed."
    }

    $verification = if ($remoteHash) { 'Size and SHA-256 verified.' } else { 'Size verified; sha256sum is unavailable on this Quest.' }
    return [pscustomobject]@{ Status='Uploaded'; Local=$localFile.Name; Remote=$target.Name; Verification=$verification }
}

function Invoke-SwitcherFastMode([scriptblock]$Status, [scriptblock]$ConfirmMigration) {
    $quest = Get-ReadyQuest $Status
    $installed = Ensure-SwitcherInstalled $quest.Serial $Status $ConfirmMigration
    Start-Switcher $quest.Serial $Status
    return $installed
}

function Invoke-HomeImport([object[]]$Accepted, [scriptblock]$Status) {
    if (-not $Accepted -or $Accepted.Count -eq 0) { return @() }
    $quest = Get-ReadyQuest $Status
    & $Status 'Creating Download/Quest Homes on the Quest...' 24
    Ensure-RemoteImportDirectory $quest.Serial

    $results = New-Object System.Collections.Generic.List[object]
    for ($index = 0; $index -lt $Accepted.Count; $index++) {
        $item = $Accepted[$index]
        $percent = 28 + [int]((($index + 1) / [double]$Accepted.Count) * 67)
        & $Status "Importing $($item.Name)..." $percent
        try {
            $results.Add((Send-HomeApk $quest.Serial $item.Path $item.TargetName))
        } catch {
            $results.Add([pscustomobject]@{ Status='Failed'; Local=$item.Name; Remote=''; Verification=$_.Exception.Message })
        }
    }
    return [object[]]$results.ToArray()
}

Add-Type -AssemblyName PresentationFramework, PresentationCore, WindowsBase

[xml]$xaml = @'
<Window xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation"
        xmlns:x="http://schemas.microsoft.com/winfx/2006/xaml"
        Title="Quest Home Switcher Setup" Width="1060" Height="770" MinWidth="930" MinHeight="700"
        WindowStartupLocation="CenterScreen" ResizeMode="CanResize"
        Background="#080D14" Foreground="#F4F7FB" FontFamily="Segoe UI">
  <Window.Resources>
    <Style TargetType="Button">
      <Setter Property="Background" Value="#55E0B5"/><Setter Property="Foreground" Value="#07110E"/>
      <Setter Property="FontWeight" Value="Bold"/><Setter Property="FontSize" Value="15"/>
      <Setter Property="Height" Value="58"/><Setter Property="BorderThickness" Value="0"/>
      <Setter Property="Cursor" Value="Hand"/><Setter Property="Padding" Value="24,0"/>
    </Style>
    <Style x:Key="SecondaryButton" TargetType="Button">
      <Setter Property="Background" Value="#1A2635"/><Setter Property="Foreground" Value="#D7E1ED"/>
      <Setter Property="FontWeight" Value="SemiBold"/><Setter Property="FontSize" Value="14"/>
      <Setter Property="Height" Value="58"/><Setter Property="BorderBrush" Value="#34465B"/>
      <Setter Property="BorderThickness" Value="1"/><Setter Property="Cursor" Value="Hand"/><Setter Property="Padding" Value="20,0"/>
    </Style>
  </Window.Resources>
  <Grid Margin="42,32,42,32">
    <Grid.RowDefinitions>
      <RowDefinition Height="Auto"/><RowDefinition Height="Auto"/><RowDefinition Height="*"/><RowDefinition Height="Auto"/>
    </Grid.RowDefinitions>

    <StackPanel>
      <Border Background="#17372F" BorderBrush="#2B806A" BorderThickness="1" Padding="10,5" HorizontalAlignment="Left">
        <TextBlock Text="QUEST HOME SWITCHER SETUP 1.0" Foreground="#65E9C0" FontSize="12" FontWeight="Bold"/>
      </Border>
      <TextBlock Text="One setup. One clear result." FontSize="34" FontWeight="Bold" Margin="0,14,0,6"/>
      <TextBlock Text="Detects your Quest and Shizuku state, installs the Home Switcher, then opens it for you." Foreground="#AEB9C9" FontSize="17"/>
    </StackPanel>

    <Grid Grid.Row="1" Margin="0,26,0,20">
      <Grid.ColumnDefinitions><ColumnDefinition Width="*"/><ColumnDefinition Width="*"/><ColumnDefinition Width="*"/></Grid.ColumnDefinitions>
      <Border Grid.Column="0" Background="#111B28" BorderBrush="#1F3143" BorderThickness="1" CornerRadius="12" Padding="19" Margin="0,0,8,0">
        <StackPanel><TextBlock Text="DEVICE" Foreground="#8192A7" FontSize="12" FontWeight="Bold"/><TextBlock x:Name="DeviceStateText" Text="Checking..." FontSize="19" FontWeight="SemiBold" Margin="0,8,0,4"/><TextBlock x:Name="DeviceDetailText" Text="Connect your Quest over USB." Foreground="#AEB9C9" FontSize="13" TextWrapping="Wrap"/></StackPanel>
      </Border>
      <Border Grid.Column="1" Background="#111B28" BorderBrush="#1F3143" BorderThickness="1" CornerRadius="12" Padding="19" Margin="4,0,4,0">
        <StackPanel><TextBlock Text="SHIZUKU" Foreground="#8192A7" FontSize="12" FontWeight="Bold"/><TextBlock x:Name="ShizukuStateText" Text="Unknown" FontSize="19" FontWeight="SemiBold" Margin="0,8,0,4"/><TextBlock x:Name="ShizukuDetailText" Text="No changes are made until you start setup." Foreground="#AEB9C9" FontSize="13" TextWrapping="Wrap"/></StackPanel>
      </Border>
      <Border Grid.Column="2" Background="#111B28" BorderBrush="#1F3143" BorderThickness="1" CornerRadius="12" Padding="19" Margin="8,0,0,0">
        <StackPanel><TextBlock Text="HOME SWITCHER" Foreground="#8192A7" FontSize="12" FontWeight="Bold"/><TextBlock x:Name="SwitcherStateText" Text="Unknown" FontSize="19" FontWeight="SemiBold" Margin="0,8,0,4"/><TextBlock x:Name="SwitcherDetailText" Text="A verified APK is embedded in this setup." Foreground="#AEB9C9" FontSize="13" TextWrapping="Wrap"/></StackPanel>
      </Border>
    </Grid>

    <Border Grid.Row="2" Background="#0E1722" BorderBrush="#1F3143" BorderThickness="1" CornerRadius="14" Padding="26">
      <StackPanel>
        <TextBlock x:Name="StageTitleText" Text="READY" Foreground="#55E0B5" FontSize="14" FontWeight="Bold"/>
        <TextBlock x:Name="StatusText" Text="Ready to check your Quest" FontSize="22" FontWeight="SemiBold" Margin="0,8,0,0"/>
        <ProgressBar x:Name="Progress" Height="10" Minimum="0" Maximum="100" Value="0" Margin="0,20,0,20" Foreground="#55E0B5" Background="#263243"/>
        <TextBlock x:Name="DetailText" Text="1. Connect your Quest over USB.&#x0a;2. Allow USB debugging inside the headset.&#x0a;3. Select SET UP / REPAIR.&#x0a;&#x0a;If Shizuku is already running, it will not be changed or restarted." Foreground="#C2CDDA" FontSize="16" LineHeight="27" TextWrapping="Wrap"/>
      </StackPanel>
    </Border>

    <Grid Grid.Row="3" Margin="0,16,0,0">
      <Grid.RowDefinitions><RowDefinition Height="Auto"/><RowDefinition Height="Auto"/></Grid.RowDefinitions>
      <Grid>
        <Grid.ColumnDefinitions><ColumnDefinition Width="*"/><ColumnDefinition Width="Auto"/></Grid.ColumnDefinitions>
        <TextBlock Text="OPTIONAL TOOLS - ADB ONLY, NO SHIZUKU SETUP" Foreground="#65E9C0" FontSize="12" FontWeight="Bold"/>
        <TextBlock Grid.Column="1" Text="NORMAL FLOW: SET UP / REPAIR" Foreground="#8192A7" FontSize="12" FontWeight="Bold"/>
      </Grid>
      <Grid Grid.Row="1" Margin="0,10,0,0">
        <Grid.ColumnDefinitions><ColumnDefinition Width="Auto"/><ColumnDefinition Width="Auto"/><ColumnDefinition Width="*"/><ColumnDefinition Width="Auto"/><ColumnDefinition Width="Auto"/></Grid.ColumnDefinitions>
        <Button x:Name="ImportButton" Grid.Column="0" Style="{StaticResource SecondaryButton}" Content="IMPORT HOME APKS" Margin="0,0,10,0"/>
        <Button x:Name="FastSwitcherButton" Grid.Column="1" Style="{StaticResource SecondaryButton}" Content="UPDATE / OPEN SWITCHER"/>
        <Button x:Name="WirelessButton" Grid.Column="3" Style="{StaticResource SecondaryButton}" Content="WIRELESS DEBUGGING" Margin="0,0,10,0"/>
        <Button x:Name="SetupButton" Grid.Column="4" Content="SET UP / REPAIR"/>
      </Grid>
    </Grid>
  </Grid>
</Window>
'@

$reader = New-Object System.Xml.XmlNodeReader $xaml
$window = [Windows.Markup.XamlReader]::Load($reader)

if ($SelfTest) {
    $required = @(
        'Find-Adb','Install-PlatformTools','Invoke-Adb','Get-QuestState','Get-ReadyQuest','Get-PackageInfo',
        'Get-ShizukuState','Get-SwitcherState','Get-Shizuku117Apk','Get-LatestShizukuApk',
        'Enable-AndroidDeveloperOptions','Open-QuestWirelessDebugging','Open-ShizukuManager',
        'Install-PairingVersion','Update-ShizukuAfterPairing','Try-StartInstalledShizuku',
        'Test-SwitcherPayload','Invoke-SwitcherSignatureMigration','Ensure-SwitcherInstalled','Start-Switcher',
        'Test-ByteSequence','Read-ZipEntryBytes','Get-ZipEntrySha256','ConvertTo-ReadableHomeName','Test-HomeApk',
        'Resolve-ExistingDirectory','Get-DownloadsDirectory','Get-DefaultHomeImportSearchRoots',
        'Find-HomeEditorCookedDirectory','Read-HomeImportDirectory','Save-HomeImportDirectory','Get-HomeImportInitialDirectory',
        'ConvertTo-SafeApkName','Add-ApkNameSuffix','New-HomeImportReviewItems','Test-HomeImportReviewNames',
        'New-HomeImportReviewWindow','Show-HomeImportReview',
        'ConvertTo-RemoteShellLiteral','Ensure-RemoteImportDirectory','Test-RemoteFileExists',
        'Get-RemoteFileSha256','Get-RemoteFileSize','Remove-RemoteImportFile','Get-RemoteImportTarget','Send-HomeApk',
        'Invoke-SwitcherFastMode','Invoke-HomeImport'
    )
    foreach ($name in $required) {
        if (-not (Get-Command $name -CommandType Function -ErrorAction SilentlyContinue)) {
            throw "Self-test failed: $name"
        }
    }
    if (-not $window) {
        throw 'Self-test failed: XAML window was not created.'
    }
    Test-SwitcherPayload | Out-Null

    $apkTestBase = [System.IO.Path]::GetFullPath((Join-Path ([System.IO.Path]::GetTempPath()) 'QuestHomeSwitcherApkValidatorTest'))
    $apkTestRoot = [System.IO.Path]::GetFullPath((Join-Path $apkTestBase ([guid]::NewGuid().ToString('N'))))
    try {
        New-Item -ItemType Directory -Force -Path $apkTestRoot | Out-Null
        function New-MockHomeApk([string]$Path, [byte[]]$ManifestBytes, [bool]$IncludeScene, [byte[]]$SceneBytes) {
            $file = [System.IO.File]::Open($Path, [System.IO.FileMode]::CreateNew, [System.IO.FileAccess]::ReadWrite, [System.IO.FileShare]::None)
            $zip = New-Object System.IO.Compression.ZipArchive($file, [System.IO.Compression.ZipArchiveMode]::Create, $false)
            try {
                $manifestEntry = $zip.CreateEntry('AndroidManifest.xml')
                $manifestStream = $manifestEntry.Open()
                try { $manifestStream.Write($ManifestBytes, 0, $ManifestBytes.Length) } finally { $manifestStream.Dispose() }
                if ($IncludeScene) {
                    $sceneEntry = $zip.CreateEntry('assets/scene.zip')
                    $sceneStream = $sceneEntry.Open()
                    if (-not $SceneBytes) { $SceneBytes = [System.Text.Encoding]::ASCII.GetBytes('mock-scene') }
                    try { $sceneStream.Write($sceneBytes, 0, $sceneBytes.Length) } finally { $sceneStream.Dispose() }
                }
            } finally {
                $zip.Dispose()
                $file.Dispose()
            }
        }

        $validUtf8 = Join-Path $apkTestRoot 'My_custom_world_NoRoot-Spoof.apk'
        $validUtf16 = Join-Path $apkTestRoot 'official-test.apk'
        $missingScene = Join-Path $apkTestRoot 'missing-scene.apk'
        $wrongPackage = Join-Path $apkTestRoot 'wrong-package.apk'
        $customSceneBytes = [System.Text.Encoding]::ASCII.GetBytes('mock-custom-scene')
        $officialSceneBytes = [System.Text.Encoding]::ASCII.GetBytes('mock-official-scene')
        New-MockHomeApk $validUtf8 ([System.Text.Encoding]::UTF8.GetBytes("prefix-$($script:HomePackageIdentifier)-suffix")) $true $customSceneBytes
        New-MockHomeApk $validUtf16 ([System.Text.Encoding]::Unicode.GetBytes("prefix-$($script:HomePackageIdentifier)-suffix")) $true $officialSceneBytes
        New-MockHomeApk $missingScene ([System.Text.Encoding]::UTF8.GetBytes($script:HomePackageIdentifier)) $false $null
        New-MockHomeApk $wrongPackage ([System.Text.Encoding]::UTF8.GetBytes('com.example.not.a.quest.home')) $true $customSceneBytes

        $customResult = Test-HomeApk $validUtf8
        if (-not $customResult.Valid -or $customResult.KnownOfficial) { throw 'Self-test failed: UTF-8 custom Home APK validation.' }
        if ($customResult.DisplayName -ne 'My Custom World' -or $customResult.SuggestedTargetName -ne 'My Custom World.apk') { throw 'Self-test failed: cleaned custom Home name.' }
        $officialSeed = Test-HomeApk $validUtf16
        $hadMockHash = $script:OfficialHomeSceneCatalog.ContainsKey($officialSeed.SceneHash)
        $previousMockName = $script:OfficialHomeSceneCatalog[$officialSeed.SceneHash]
        try {
            $script:OfficialHomeSceneCatalog[$officialSeed.SceneHash] = 'Mock Official Home'
            $officialResult = Test-HomeApk $validUtf16
            if (-not $officialResult.Valid -or -not $officialResult.KnownOfficial) { throw 'Self-test failed: official scene hash recognition.' }
            if ($officialResult.DisplayName -ne 'Mock Official Home' -or $officialResult.SuggestedTargetName -ne 'Mock Official Home.apk') { throw 'Self-test failed: official Home target name.' }
        } finally {
            if ($hadMockHash) { $script:OfficialHomeSceneCatalog[$officialSeed.SceneHash] = $previousMockName } else { $script:OfficialHomeSceneCatalog.Remove($officialSeed.SceneHash) }
        }
        if ($script:OfficialHomeSceneCatalog.Count -ne 44) { throw 'Self-test failed: official Home scene catalog count.' }
        if ($script:OfficialHomeSceneCatalog['1738D80E3A648F0EF73E304FD423B48F68A6C946B278DEAF9D19BF1CE1DD95DE'] -ne 'Crystal Atrium' -or
            $script:OfficialHomeSceneCatalog['34288D09200710F6CE53610192FB7EB07E4849ACAA37A85DA9B6C13AE8821662'] -ne 'Cyber City' -or
            $script:OfficialHomeSceneCatalog['A26FE53836D91F30B6E4BF2154213783B7D55B7B2105AE9356F7E6E878A20D73'] -ne 'Cascadia') {
            throw 'Self-test failed: official Home catalog names.'
        }
        if ((Test-HomeApk $missingScene).Valid) { throw 'Self-test failed: missing scene.zip rejection.' }
        if ((Test-HomeApk $wrongPackage).Valid) { throw 'Self-test failed: wrong package rejection.' }
        $unicodeHomeName = "My H$([char]0x00F6)hle (weird)"
        if ((ConvertTo-SafeApkName "C:\Unsafe Folder\$unicodeHomeName!.apk") -ne "$unicodeHomeName.apk") { throw 'Self-test failed: safe Unicode APK name.' }
        if ((Add-ApkNameSuffix 'My Home.apk' 2) -ne 'My Home-2.apk') { throw 'Self-test failed: collision suffix.' }
        if ((ConvertTo-SafeApkName 'C:\Unsafe\..apk') -ne 'Quest Home.apk') { throw 'Self-test failed: empty-name fallback.' }

        $pickerRoot = Join-Path $apkTestRoot 'picker'
        $editorCooked = Join-Path $pickerRoot 'Quest Home Editor\cooked'
        $pickerFallback = Join-Path $pickerRoot 'fallback'
        $pickerHistory = Join-Path $pickerRoot 'state\home-import-directory.txt'
        New-Item -ItemType Directory -Force -Path $editorCooked, $pickerFallback | Out-Null
        if ((Find-HomeEditorCookedDirectory @($pickerRoot)) -ne $editorCooked) { throw 'Self-test failed: Quest Home Editor cooked folder detection.' }
        if (-not (Save-HomeImportDirectory $validUtf8 $pickerHistory)) { throw 'Self-test failed: Home import directory preference save.' }
        if ((Read-HomeImportDirectory $pickerHistory) -ne $apkTestRoot) { throw 'Self-test failed: Home import directory preference read.' }
        if ((Get-HomeImportInitialDirectory -SearchRoots @($pickerRoot) -HistoryFile $pickerHistory -DownloadsPath $pickerFallback) -ne $editorCooked) { throw 'Self-test failed: detected cooked folder was not preferred over a non-cooked history path.' }
        if (-not (Save-HomeImportDirectory $editorCooked $pickerHistory)) { throw 'Self-test failed: remembered cooked directory save.' }
        if ((Get-HomeImportInitialDirectory -SearchRoots @($pickerFallback) -HistoryFile $pickerHistory -DownloadsPath $pickerFallback) -ne $editorCooked) { throw 'Self-test failed: remembered cooked folder was not restored.' }

        $reviewSource = @(
            [pscustomobject]@{ Name='one.apk'; DisplayName='Same Home'; SuggestedTargetName='Same Home.apk'; Kind='Custom Home'; Path='C:\one.apk'; SceneHash='AAAAAAAAAAAA'; KnownOfficial=$false },
            [pscustomobject]@{ Name='two.apk'; DisplayName='Same Home'; SuggestedTargetName='Same Home.apk'; Kind='Official Meta Home'; Path='C:\two.apk'; SceneHash='BBBBBBBBBBBB'; KnownOfficial=$true }
        )
        $reviewItems = @(New-HomeImportReviewItems $reviewSource)
        if ($reviewItems.Count -ne 2 -or $reviewItems[0].TargetName -ne 'Same Home.apk' -or $reviewItems[1].TargetName -ne 'Same Home-2.apk') { throw 'Self-test failed: unique multi-select suggestions.' }
        $reviewItems[0].TargetName = 'Edited*Home'
        $cleanReview = Test-HomeImportReviewNames $reviewItems
        if ($cleanReview.Valid -or -not $cleanReview.Changed -or $reviewItems[0].TargetName -ne 'Edited Home.apk') { throw 'Self-test failed: edited target cleanup.' }
        if (-not (Test-HomeImportReviewNames $reviewItems).Valid) { throw 'Self-test failed: cleaned review acceptance.' }
        $reviewItems[1].TargetName = $reviewItems[0].TargetName
        if ((Test-HomeImportReviewNames $reviewItems).Valid) { throw 'Self-test failed: duplicate edited target rejection.' }

        $reviewWindow = New-HomeImportReviewWindow
        try {
            if (-not $reviewWindow -or -not $reviewWindow.FindName('ReviewGrid') -or -not $reviewWindow.FindName('ReviewContinueButton')) { throw 'Self-test failed: Home name review XAML.' }
        } finally {
            if ($reviewWindow) { $reviewWindow.Close() }
        }
    } finally {
        $safePrefix = $apkTestBase.TrimEnd([System.IO.Path]::DirectorySeparatorChar) + [System.IO.Path]::DirectorySeparatorChar
        if ($apkTestRoot.StartsWith($safePrefix, [System.StringComparison]::OrdinalIgnoreCase) -and (Test-Path -LiteralPath $apkTestRoot)) {
            Remove-Item -LiteralPath $apkTestRoot -Recurse -Force
        }
    }

    $originalInvokeAdb = (Get-Item Function:Invoke-Adb).ScriptBlock
    try {
        $script:MockDeviceCase = 'Running'
        $script:MockSwitcherCase = 'Expected'
        Set-Item Function:Invoke-Adb -Value {
            param([string[]]$Arguments, [switch]$AllowFailure)
            $joined = $Arguments -join ' '
            $exitCode = 0
            $mockOutput = ''

            if ($joined -match 'pm path moe\.shizuku\.privileged\.api') {
                if ($script:MockDeviceCase -eq 'Missing') {
                    $exitCode = 1
                } else {
                    $mockOutput = 'package:/data/app/mock/shizuku/base.apk'
                }
            } elseif ($joined -match 'dumpsys package moe\.shizuku\.privileged\.api') {
                $mockOutput = "  legacyNativeLibraryDir=/data/app/mock/shizuku/lib`n  versionCode=1086 minSdk=24`n  versionName=13.6.0.mock"
            } elseif ($joined -match 'pidof shizuku_server') {
                if ($script:MockDeviceCase -eq 'Stopped') {
                    $exitCode = 1
                } else {
                    $mockOutput = '4321'
                }
            } elseif ($joined -match 'cat /proc/4321/cmdline') {
                $mockOutput = [string]::Concat('shizuku_server', [char]0)
            } elseif ($joined -match 'cat /proc/4321/status') {
                $uid = if ($script:MockDeviceCase -eq 'InvalidUid') { '1000' } else { '2000' }
                $mockOutput = "Name:`tmain`nUid:`t$uid`t$uid`t$uid`t$uid"
            } elseif ($joined -match 'pm path io\.github\.nikitat21\.questhomeswitcher') {
                $mockOutput = 'package:/data/app/mock/switcher/base.apk'
            } elseif ($joined -match 'dumpsys package io\.github\.nikitat21\.questhomeswitcher') {
                if ($script:MockSwitcherCase -eq 'SameCodeWrongName') {
                    $mockOutput = "  versionCode=13 minSdk=29`n  versionName=unrelated-test-build"
                } elseif ($script:MockSwitcherCase -eq 'Newer') {
                    $mockOutput = "  versionCode=14 minSdk=29`n  versionName=future-release"
                } elseif ($script:MockSwitcherCase -eq 'Legacy') {
                    $mockOutput = "  versionCode=12 minSdk=29`n  versionName=legacy-test-build"
                } else {
                    $mockOutput = "  versionCode=13 minSdk=29`n  versionName=1.0"
                }
            }

            return [pscustomobject]@{ ExitCode=$exitCode; Output=$mockOutput }
        }

        $script:MockDeviceCase = 'Running'
        if ((Get-ShizukuState 'MOCK').State -ne 'Running') { throw 'Self-test failed: verified running Shizuku state.' }
        if ((Get-SwitcherState 'MOCK').State -ne 'Current') { throw 'Self-test failed: current Switcher state.' }
        $script:MockSwitcherCase = 'SameCodeWrongName'
        if ((Get-SwitcherState 'MOCK').State -ne 'Outdated') { throw 'Self-test failed: same-code wrong-name Switcher rejection.' }
        $script:MockSwitcherCase = 'Newer'
        if ((Get-SwitcherState 'MOCK').State -ne 'Current') { throw 'Self-test failed: newer Switcher acceptance.' }
        $script:MockSwitcherCase = 'Legacy'
        if ((Get-SwitcherState 'MOCK').State -ne 'Outdated') { throw 'Self-test failed: older Switcher update detection.' }
        $script:MockSwitcherCase = 'Expected'

        $script:MockDeviceCase = 'Stopped'
        if ((Get-ShizukuState 'MOCK').State -ne 'InstalledStopped') { throw 'Self-test failed: stopped Shizuku state.' }

        $script:MockDeviceCase = 'Missing'
        if ((Get-ShizukuState 'MOCK').State -ne 'Missing') { throw 'Self-test failed: missing Shizuku state.' }

        $script:MockDeviceCase = 'InvalidUid'
        if ((Get-ShizukuState 'MOCK').State -ne 'InvalidProcess') { throw 'Self-test failed: invalid Shizuku UID rejection.' }

        $script:MockLocalHash = 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA'
        $script:MockCollisionCase = 'Identical'
        Set-Item Function:Invoke-Adb -Value {
            param([string[]]$Arguments, [switch]$AllowFailure)
            $joined = $Arguments -join ' '
            if ($script:MockCollisionCase -eq 'LookupFailure' -and $joined -match 'if \[ -f ') {
                return [pscustomobject]@{ ExitCode=1; Output='mock shell failure' }
            }
            if ($joined -match 'if \[ -f .*Home-2\.apk') {
                return [pscustomobject]@{ ExitCode=0; Output='MISSING' }
            }
            if ($joined -match 'if \[ -f .*Home\.apk') {
                return [pscustomobject]@{ ExitCode=0; Output='EXISTS' }
            }
            if ($joined -match 'sha256sum .*Home\.apk') {
                $hash = if ($script:MockCollisionCase -eq 'Identical') { $script:MockLocalHash } else { 'BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB' }
                return [pscustomobject]@{ ExitCode=0; Output="$hash  /sdcard/Download/Quest Homes/Home.apk" }
            }
            return [pscustomobject]@{ ExitCode=0; Output='' }
        }

        $identicalTarget = Get-RemoteImportTarget 'MOCK' 'Home.apk' $script:MockLocalHash
        if (-not $identicalTarget.Skip -or $identicalTarget.Name -ne 'Home.apk') { throw 'Self-test failed: identical SHA collision skip.' }
        $script:MockCollisionCase = 'Different'
        $suffixTarget = Get-RemoteImportTarget 'MOCK' 'Home.apk' $script:MockLocalHash
        if ($suffixTarget.Skip -or $suffixTarget.Name -ne 'Home-2.apk') { throw 'Self-test failed: non-identical collision suffix.' }
        $script:MockCollisionCase = 'LookupFailure'
        $lookupFailedClosed = $false
        try { Get-RemoteImportTarget 'MOCK' 'Home.apk' $script:MockLocalHash | Out-Null } catch { $lookupFailedClosed = $true }
        if (-not $lookupFailedClosed) { throw 'Self-test failed: remote collision lookup did not fail closed.' }
    } finally {
        Set-Item Function:Invoke-Adb -Value $originalInvokeAdb
    }

    $migrationOriginals = @{
        'Invoke-Adb' = (Get-Item Function:Invoke-Adb).ScriptBlock
        'Test-SwitcherPayload' = (Get-Item Function:Test-SwitcherPayload).ScriptBlock
        'Get-SwitcherState' = (Get-Item Function:Get-SwitcherState).ScriptBlock
        'Get-ReadyQuest' = (Get-Item Function:Get-ReadyQuest).ScriptBlock
        'Start-Switcher' = (Get-Item Function:Start-Switcher).ScriptBlock
        'Get-ShizukuState' = (Get-Item Function:Get-ShizukuState).ScriptBlock
    }
    try {
        $script:MigrationPhase = 'Before'
        $script:MigrationCommands = New-Object System.Collections.Generic.List[string]
        $script:MigrationShizukuCalls = 0
        $script:MigrationSwitcherStarts = 0
        Set-Item Function:Test-SwitcherPayload -Value { return 'MOCK_HASH' }
        Set-Item Function:Get-ReadyQuest -Value {
            param([scriptblock]$Status)
            return [pscustomobject]@{ State='device'; Serial='MOCK'; Detail='Mock Quest' }
        }
        Set-Item Function:Start-Switcher -Value {
            param([string]$Serial, [scriptblock]$Status)
            $script:MigrationSwitcherStarts++
        }
        Set-Item Function:Get-ShizukuState -Value {
            param([string]$Serial)
            $script:MigrationShizukuCalls++
            throw 'Signature migration or Fast Mode called Get-ShizukuState.'
        }
        Set-Item Function:Get-SwitcherState -Value {
            param([string]$Serial)
            if ($script:MigrationPhase -eq 'After') {
                return [pscustomobject]@{ State='Current'; Version='1.0'; VersionCode=11; Detail='mock current' }
            }
            return [pscustomobject]@{ State='Outdated'; Version='1.2.7-debug'; VersionCode=10; Detail='mock debug build' }
        }
        Set-Item Function:Invoke-Adb -Value {
            param([string[]]$Arguments, [switch]$AllowFailure)
            $joined = $Arguments -join ' '
            $script:MigrationCommands.Add($joined)
            if ($joined -match ' install -r ') {
                return [pscustomobject]@{ ExitCode=1; Output='Failure [INSTALL_FAILED_UPDATE_INCOMPATIBLE: signatures do not match]' }
            }
            if ($joined -match ' uninstall io\.github\.nikitat21\.questhomeswitcher$') {
                return [pscustomobject]@{ ExitCode=0; Output='Success' }
            }
            if ($joined -match ' install ' -and $joined -notmatch ' install -r ') {
                $script:MigrationPhase = 'After'
                return [pscustomobject]@{ ExitCode=0; Output='Success' }
            }
            return [pscustomobject]@{ ExitCode=0; Output='' }
        }

        $migrationStatus = { param([string]$Text, [int]$Percent) }
        $migrated = Invoke-SwitcherFastMode $migrationStatus { $true }
        if ($migrated.State -ne 'Current') { throw 'Self-test failed: approved signature migration did not verify.' }
        $uninstallCommands = @($script:MigrationCommands | Where-Object { $_ -match ' uninstall ' })
        if ($uninstallCommands.Count -ne 1 -or $uninstallCommands[0] -notmatch ' uninstall io\.github\.nikitat21\.questhomeswitcher$') { throw 'Self-test failed: migration uninstall package scope.' }
        if (@($script:MigrationCommands | Where-Object { $_ -match '(?i)shizuku' }).Count -ne 0) { throw 'Self-test failed: signature migration touched Shizuku.' }
        if ($script:MigrationShizukuCalls -ne 0) { throw 'Self-test failed: signature migration or Fast Mode inspected Shizuku.' }
        if ($script:MigrationSwitcherStarts -ne 1) { throw 'Self-test failed: migrated Switcher did not open.' }

        $script:MigrationPhase = 'Before'
        $script:MigrationCommands.Clear()
        $migrationCanceled = $false
        try { Invoke-SwitcherFastMode $migrationStatus { $false } | Out-Null } catch { $migrationCanceled = $_.Exception.Message -match 'canceled' }
        if (-not $migrationCanceled) { throw 'Self-test failed: rejected signature migration.' }
        if (@($script:MigrationCommands | Where-Object { $_ -match ' uninstall ' }).Count -ne 0) { throw 'Self-test failed: rejected migration removed an app.' }
        if ($script:MigrationShizukuCalls -ne 0 -or $script:MigrationSwitcherStarts -ne 1) { throw 'Self-test failed: rejected Fast Mode migration caused a side effect.' }
    } finally {
        foreach ($entry in $migrationOriginals.GetEnumerator()) {
            Set-Item "Function:$($entry.Key)" -Value $entry.Value
        }
    }

    $fastModeOriginals = @{
        'Get-ReadyQuest' = (Get-Item Function:Get-ReadyQuest).ScriptBlock
        'Ensure-SwitcherInstalled' = (Get-Item Function:Ensure-SwitcherInstalled).ScriptBlock
        'Start-Switcher' = (Get-Item Function:Start-Switcher).ScriptBlock
        'Ensure-RemoteImportDirectory' = (Get-Item Function:Ensure-RemoteImportDirectory).ScriptBlock
        'Send-HomeApk' = (Get-Item Function:Send-HomeApk).ScriptBlock
        'Get-ShizukuState' = (Get-Item Function:Get-ShizukuState).ScriptBlock
    }
    try {
        $script:FastModeShizukuCalls = 0
        $script:FastModeSwitcherStarts = 0
        $script:FastModeImports = 0
        Set-Item Function:Get-ShizukuState -Value {
            param([string]$Serial)
            $script:FastModeShizukuCalls++
            throw 'Fast mode called Get-ShizukuState.'
        }
        Set-Item Function:Get-ReadyQuest -Value {
            param([scriptblock]$Status)
            return [pscustomobject]@{ State='device'; Serial='MOCK'; Detail='Mock Quest' }
        }
        Set-Item Function:Ensure-SwitcherInstalled -Value {
            param([string]$Serial, [scriptblock]$Status, [scriptblock]$ConfirmMigration)
            return [pscustomobject]@{ State='Current'; Version='mock'; VersionCode=10 }
        }
        Set-Item Function:Start-Switcher -Value {
            param([string]$Serial, [scriptblock]$Status)
            $script:FastModeSwitcherStarts++
        }
        Set-Item Function:Ensure-RemoteImportDirectory -Value {
            param([string]$Serial)
        }
        Set-Item Function:Send-HomeApk -Value {
            param([string]$Serial, [string]$LocalPath, [string]$DesiredName)
            $script:FastModeImports++
            return [pscustomobject]@{ Status='Uploaded'; Local='mock.apk'; Remote='mock.apk'; Verification='mock' }
        }

        $silentStatus = { param([string]$Text, [int]$Percent) }
        Invoke-SwitcherFastMode $silentStatus { $false } | Out-Null
        $mockImport = [pscustomobject]@{ Valid=$true; Name='mock.apk'; Path='C:\mock.apk'; TargetName='Mock Home.apk' }
        $mockImportResults = @(Invoke-HomeImport @($mockImport) $silentStatus)
        if ($script:FastModeShizukuCalls -ne 0) { throw 'Self-test failed: optional fast mode called Shizuku state.' }
        if ($script:FastModeSwitcherStarts -ne 1) { throw 'Self-test failed: Switcher fast mode did not start the app.' }
        if ($script:FastModeImports -ne 1 -or $mockImportResults.Count -ne 1) { throw 'Self-test failed: Home import fast mode.' }
    } finally {
        foreach ($entry in $fastModeOriginals.GetEnumerator()) {
            Set-Item "Function:$($entry.Key)" -Value $entry.Value
        }
    }

    Write-Output 'SELF_TEST_OK_XAML_OK_PAYLOAD_OK_STATE_MACHINE_OK_HOME_IMPORT_OK_COOKED_PICKER_OK_PROFESSIONAL_NAMING_OK_SIGNATURE_MIGRATION_OK_FAST_MODES_NO_SHIZUKU_OK'
    exit 0
}

$deviceStateText = $window.FindName('DeviceStateText')
$deviceDetailText = $window.FindName('DeviceDetailText')
$shizukuStateText = $window.FindName('ShizukuStateText')
$shizukuDetailText = $window.FindName('ShizukuDetailText')
$switcherStateText = $window.FindName('SwitcherStateText')
$switcherDetailText = $window.FindName('SwitcherDetailText')
$stageTitleText = $window.FindName('StageTitleText')
$statusText = $window.FindName('StatusText')
$detailText = $window.FindName('DetailText')
$progress = $window.FindName('Progress')
$setupButton = $window.FindName('SetupButton')
$wirelessButton = $window.FindName('WirelessButton')
$importButton = $window.FindName('ImportButton')
$fastSwitcherButton = $window.FindName('FastSwitcherButton')

$updateStatus = {
    param([string]$Text, [int]$Percent)
    $statusText.Text = $Text
    $progress.Value = $Percent
    $window.Dispatcher.Invoke([action]{}, 'Background')
}

$confirmSwitcherMigration = {
    $message = "Android found a Quest Home Switcher build signed with a different test key.`n`nTo install the release build, setup must remove ONLY the current Quest Home Switcher app and then install the verified replacement.`n`nThis clears the old Switcher app settings. It does NOT uninstall or change Shizuku, Shizuku pairing, or Home APK files in Download/Quest Homes.`n`nReplace the incompatible Quest Home Switcher now?"
    $choice = [System.Windows.MessageBox]::Show(
        $window,
        $message,
        'Replace incompatible Quest Home Switcher?',
        [System.Windows.MessageBoxButton]::YesNo,
        [System.Windows.MessageBoxImage]::Warning,
        [System.Windows.MessageBoxResult]::No
    )
    return $choice -eq [System.Windows.MessageBoxResult]::Yes
}

function Set-StatusCard([System.Windows.Controls.TextBlock]$Title, [System.Windows.Controls.TextBlock]$Detail, [string]$StateText, [string]$DetailText, [bool]$Positive) {
    $Title.Text = $StateText
    $Detail.Text = $DetailText
    $Title.Foreground = if ($Positive) { '#65E9C0' } else { '#F4C96B' }
}

function Refresh-StatusCards {
    $script:AdbPath = Find-Adb
    if (-not $script:AdbPath) {
        Set-StatusCard $deviceStateText $deviceDetailText 'ADB not ready' 'Platform Tools will be downloaded when setup starts.' $false
        Set-StatusCard $shizukuStateText $shizukuDetailText 'Unknown' 'Connect the Quest and start setup.' $false
        Set-StatusCard $switcherStateText $switcherDetailText 'Payload ready' 'The embedded APK will be verified before installation.' $true
        return
    }

    Invoke-Adb @('start-server') -AllowFailure | Out-Null
    $quest = Get-QuestState
    if ($quest.State -ne 'device') {
        Set-StatusCard $deviceStateText $deviceDetailText 'Not connected' $quest.Detail $false
        Set-StatusCard $shizukuStateText $shizukuDetailText 'Unknown' 'A connected and authorized Quest is required.' $false
        Set-StatusCard $switcherStateText $switcherDetailText 'Payload ready' 'The included APK will be installed after connection.' $true
        return
    }

    Set-StatusCard $deviceStateText $deviceDetailText 'Quest connected' "Authorized device $($quest.Serial)." $true
    $shizuku = Get-ShizukuState $quest.Serial
    switch ($shizuku.State) {
        'Running' { Set-StatusCard $shizukuStateText $shizukuDetailText "Running $($shizuku.Version)" $shizuku.Detail $true }
        'Missing' { Set-StatusCard $shizukuStateText $shizukuDetailText 'Not installed' 'The guided 11.7 pairing path will be used.' $false }
        'InvalidProcess' { Set-StatusCard $shizukuStateText $shizukuDetailText 'Verification failed' $shizuku.Detail $false }
        default { Set-StatusCard $shizukuStateText $shizukuDetailText "Installed $($shizuku.Version)" 'The server is not running yet.' $false }
    }

    $switcher = Get-SwitcherState $quest.Serial
    switch ($switcher.State) {
        'Current' { Set-StatusCard $switcherStateText $switcherDetailText "Installed $($switcher.Version)" $switcher.Detail $true }
        'Outdated' { Set-StatusCard $switcherStateText $switcherDetailText "Update available" $switcher.Detail $false }
        default { Set-StatusCard $switcherStateText $switcherDetailText 'Not installed' 'The verified APK is ready to install.' $false }
    }
}

function Complete-SwitcherSetup([string]$Serial) {
    $installed = Ensure-SwitcherInstalled $Serial $updateStatus $confirmSwitcherMigration
    Start-Switcher $Serial $updateStatus
    $script:SetupComplete = $true
    $stageTitleText.Text = 'SETUP COMPLETE'
    $statusText.Text = 'Quest Home Switcher is ready'
    $detailText.Text = "Shizuku is verified as running.`n`nQuest Home Switcher $($installed.Version) is installed and has been opened on your Quest. Confirm its Shizuku permission inside the headset if Android asks once."
    $progress.Value = 100
    $setupButton.Content = 'SETUP COMPLETE - CLOSE'
    Refresh-StatusCards
}

function Invoke-StateBasedSetup {
    $quest = Get-ReadyQuest $updateStatus
    Refresh-StatusCards
    $shizuku = Get-ShizukuState $quest.Serial

    if ($shizuku.State -eq 'InvalidProcess') {
        throw $shizuku.Detail
    }

    if ($shizuku.State -eq 'Running') {
        $stageTitleText.Text = 'SHIZUKU VERIFIED'
        $detailText.Text = 'A verified Shizuku server is already running as Android shell UID 2000. It will be left completely untouched.'
        & $updateStatus 'Shizuku is already running - leaving it untouched...' 66
        Complete-SwitcherSetup $quest.Serial
        return
    }

    if ($shizuku.State -eq 'Missing') {
        $stageTitleText.Text = 'ONE-TIME SHIZUKU PAIRING'
        Install-PairingVersion $quest.Serial $updateStatus
        $statusText.Text = 'Pair Shizuku 11.7 once inside the Quest'
        $detailText.Text = "1. In Wireless debugging, select Pair device with pairing code.`n2. In Shizuku 11.7, select Pairing.`n3. Enter the six-digit code and wait for success.`n4. Do not press Start in Shizuku 11.7.`n5. Return here and select PAIRING COMPLETE - CONTINUE."
        $progress.Value = 60
        $setupButton.Content = 'PAIRING COMPLETE - CONTINUE'
        Refresh-StatusCards
        return
    }

    if ($shizuku.State -eq 'InstalledStopped' -and $shizuku.Version -match '^11\.7(?:\.|$)') {
        $stageTitleText.Text = 'SAFE SHIZUKU UPGRADE'
        Update-ShizukuAfterPairing $quest.Serial $updateStatus
        $shizuku = Get-ShizukuState $quest.Serial
    }

    if ($shizuku.State -eq 'InstalledStopped') {
        $stageTitleText.Text = 'STARTING SHIZUKU'
        $shizuku = Try-StartInstalledShizuku $quest.Serial $updateStatus
        if ($shizuku.State -ne 'Running') {
            Open-ShizukuManager $quest.Serial
            $statusText.Text = 'Shizuku is installed but still offline'
            $detailText.Text = "Shizuku was opened on your Quest. Select Start via Wireless debugging once, then return here and select SHIZUKU STARTED - CONTINUE.`n`nIf Shizuku keeps searching, open Wireless debugging, turn its main switch OFF, wait three seconds, and turn it ON again."
            $progress.Value = 68
            $setupButton.Content = 'SHIZUKU STARTED - CONTINUE'
            Refresh-StatusCards
            return
        }
    }

    if ($shizuku.State -ne 'Running') {
        throw 'Shizuku is not running yet. Start it inside the headset and try again.'
    }

    Complete-SwitcherSetup $quest.Serial
}

$window.Add_ContentRendered({
    if ($script:UiInitialized) { return }
    $script:UiInitialized = $true
    try {
        Refresh-StatusCards
    } catch {
        $statusText.Text = 'Ready - connect your Quest to continue'
        $detailText.Text = $_.Exception.Message
    }
})

$importButton.Add_Click({
    $dialog = New-Object Microsoft.Win32.OpenFileDialog
    $dialog.Title = 'Select compatible Quest Home APKs'
    $dialog.Filter = 'Android APK files (*.apk)|*.apk'
    $dialog.Multiselect = $true
    $initialDirectory = Get-HomeImportInitialDirectory
    if ($initialDirectory) { $dialog.InitialDirectory = $initialDirectory }
    $selected = $dialog.ShowDialog($window)
    if ($selected -ne $true) { return }
    if ($dialog.FileNames.Count -gt 0) { Save-HomeImportDirectory $dialog.FileNames[0] | Out-Null }

    $importButton.IsEnabled = $false
    $fastSwitcherButton.IsEnabled = $false
    $wirelessButton.IsEnabled = $false
    $setupButton.IsEnabled = $false
    try {
        $stageTitleText.Text = 'HOME APK IMPORT'
        & $updateStatus 'Validating selected APK contents...' 8
        $validation = @($dialog.FileNames | ForEach-Object { Test-HomeApk $_ })
        $accepted = @($validation | Where-Object { $_.Valid })
        $rejected = @($validation | Where-Object { -not $_.Valid })

        if (-not $accepted) {
            $statusText.Text = 'No compatible Quest Home APKs selected'
            $detailText.Text = (($rejected | Select-Object -First 12 | ForEach-Object { "REJECTED  $($_.Name) - $($_.Reason)" }) -join "`n")
            $progress.Value = 0
            return
        }

        $reviewItems = New-HomeImportReviewItems $accepted
        $reviewedItems = @(Show-HomeImportReview $reviewItems $window)
        if ($reviewedItems.Count -eq 0) {
            $stageTitleText.Text = 'HOME APK IMPORT'
            $statusText.Text = 'Import canceled - nothing was uploaded'
            $detailText.Text = 'Your selected APKs were validated, but no files were sent to the Quest.'
            $progress.Value = 0
            return
        }

        $results = @(Invoke-HomeImport $reviewedItems $updateStatus)

        $uploaded = @($results | Where-Object { $_.Status -eq 'Uploaded' }).Count
        $skipped = @($results | Where-Object { $_.Status -eq 'Skipped' }).Count
        $failed = @($results | Where-Object { $_.Status -eq 'Failed' }).Count
        $statusText.Text = "$uploaded uploaded, $skipped already present, $($rejected.Count) rejected, $failed failed"

        $lines = New-Object System.Collections.Generic.List[string]
        foreach ($result in $results) {
            if ($result.Status -eq 'Uploaded') {
                $lines.Add("UPLOADED  $($result.Local) -> $($result.Remote) ($($result.Verification))")
            } elseif ($result.Status -eq 'Skipped') {
                $lines.Add("SKIPPED   $($result.Local) - identical SHA-256 already exists as $($result.Remote)")
            } else {
                $lines.Add("FAILED    $($result.Local) - $($result.Verification)")
            }
        }
        foreach ($item in $rejected) {
            $lines.Add("REJECTED  $($item.Name) - $($item.Reason)")
        }
        $visibleLines = @($lines | Select-Object -First 12)
        if ($lines.Count -gt 12) { $visibleLines += "...and $($lines.Count - 12) more result(s)." }
        $detailText.Text = "Imported files are in Download/Quest Homes. Open Quest Home Switcher and refresh its list.`n`n" + ($visibleLines -join "`n")
        $progress.Value = if ($failed -eq 0) { 100 } else { 95 }
    } catch {
        $stageTitleText.Text = 'HOME IMPORT - ACTION NEEDED'
        $statusText.Text = 'Home APK import did not complete'
        $detailText.Text = $_.Exception.Message
        $progress.Value = 0
    } finally {
        $importButton.IsEnabled = $true
        $fastSwitcherButton.IsEnabled = $true
        $wirelessButton.IsEnabled = $true
        $setupButton.IsEnabled = $true
    }
})

$fastSwitcherButton.Add_Click({
    $importButton.IsEnabled = $false
    $fastSwitcherButton.IsEnabled = $false
    $wirelessButton.IsEnabled = $false
    $setupButton.IsEnabled = $false
    try {
        $stageTitleText.Text = 'OPTIONAL ADB-ONLY TOOL'
        $detailText.Text = 'Checking only the Quest connection and Quest Home Switcher package. Shizuku setup, pairing, and starter functions are not used by this action.'
        $installed = Invoke-SwitcherFastMode $updateStatus $confirmSwitcherMigration
        $statusText.Text = "Quest Home Switcher $($installed.Version) is ready and open"
        $detailText.Text = 'The Switcher payload was verified, its installed version was checked, and the app was opened. This optional action did not inspect, start, update, pair, or configure Shizuku.'
        $progress.Value = 100
    } catch {
        $stageTitleText.Text = 'SWITCHER TOOL - ACTION NEEDED'
        $statusText.Text = 'Quest Home Switcher could not be updated or opened'
        $detailText.Text = $_.Exception.Message
        $progress.Value = 0
    } finally {
        $importButton.IsEnabled = $true
        $fastSwitcherButton.IsEnabled = $true
        $wirelessButton.IsEnabled = $true
        $setupButton.IsEnabled = $true
    }
})

$wirelessButton.Add_Click({
    $wirelessButton.IsEnabled = $false
    $importButton.IsEnabled = $false
    $fastSwitcherButton.IsEnabled = $false
    try {
        $quest = Get-ReadyQuest $updateStatus
        Open-QuestWirelessDebugging $quest.Serial
        $stageTitleText.Text = 'WIRELESS DEBUGGING OPENED'
        $statusText.Text = 'Continue inside the Quest'
        $detailText.Text = 'The Wireless debugging page is open. Use Pair device with pairing code for first-time setup, or toggle the main switch OFF and ON if Shizuku keeps searching.'
    } catch {
        $stageTitleText.Text = 'ACTION NEEDED'
        $statusText.Text = 'Wireless debugging could not be opened'
        $detailText.Text = $_.Exception.Message
    } finally {
        $wirelessButton.IsEnabled = $true
        $importButton.IsEnabled = $true
        $fastSwitcherButton.IsEnabled = $true
    }
})

$setupButton.Add_Click({
    if ($script:SetupComplete) {
        $window.Close()
        return
    }
    $setupButton.IsEnabled = $false
    $wirelessButton.IsEnabled = $false
    $importButton.IsEnabled = $false
    $fastSwitcherButton.IsEnabled = $false
    try {
        Invoke-StateBasedSetup
    } catch {
        $stageTitleText.Text = 'ACTION NEEDED'
        $statusText.Text = 'Setup is not complete yet'
        $detailText.Text = $_.Exception.Message
        $progress.Value = 0
        $setupButton.Content = 'TRY AGAIN'
        try { Refresh-StatusCards } catch { }
    } finally {
        if (-not $script:SetupComplete) {
            $setupButton.IsEnabled = $true
        } else {
            $setupButton.IsEnabled = $true
        }
        $wirelessButton.IsEnabled = $true
        $importButton.IsEnabled = $true
        $fastSwitcherButton.IsEnabled = $true
    }
})

$window.ShowDialog() | Out-Null
