# Quest Home Switcher Desktop Setup — Phase 1

This directory contains a dependency-free .NET 10 core and command-line interface for Windows, macOS, and Linux. It is intentionally isolated from the existing Windows PowerShell setup and Android application. Phase 1 does not replace either product and does not embed or download an APK automatically.

## Current capabilities

- Detects Windows, macOS, or Linux and the current CPU architecture.
- Finds `adb` through `--adb`, `QHS_ADB`, Android SDK environment variables, known SDK locations, or `PATH`.
- `doctor` reports host, ADB, and USB authorization state.
- `status` reports the connected Quest, Switcher package, and verified Shizuku server state.
- Installs a SHA-256-trusted Switcher APK without any automatic uninstall or signing-key migration.
- Opens Quest Home Switcher on the headset.
- Validates NoRoot-spoof Home APK contents before import.
- Imports Homes to `/sdcard/Download/Quest Homes` with safe filenames, existing-file SHA-256 detection, verified temporary upload, and no-clobber finalization.

The implementation uses only .NET libraries. There are no NuGet package references.

## Safety boundary

The CLI deliberately has no command that uninstalls an Android package. A signing-key conflict fails with an explanation and leaves the installed app unchanged.

For every Home import:

1. The APK must contain exactly one non-empty `assets/scene.zip` entry.
2. Its `AndroidManifest.xml` must contain the Quest Home target package marker.
3. The whole local APK is SHA-256 hashed.
4. It is pushed to a unique `.qhs-upload-<random>.part` path.
5. Quest size and SHA-256 must match before finalization.
6. Existing target names are only read. An identical hash is skipped; a different or unreadable hash gets `-2`, `-3`, and so on.
7. `mv -n` performs no-clobber finalization. Only the unique temporary `.part` file owned by the current operation may be removed.

Personal Home APKs are never deleted or overwritten.

## Prerequisites

All platforms require:

- A Meta Quest with developer mode and USB debugging enabled.
- USB debugging approval inside the headset.
- The [.NET 10 SDK](https://dotnet.microsoft.com/download/dotnet/10.0) to build from source.
- Google [Android SDK Platform Tools](https://developer.android.com/tools/releases/platform-tools), or an existing Android SDK installation.

The CLI does not silently download Platform Tools because Google's download is governed by the Android SDK license. If ADB is outside `PATH`, pass its executable with `--adb PATH` or set `QHS_ADB`.

### macOS

Google documents that macOS normally needs no additional ADB USB driver. Install Platform Tools, connect the Quest, approve USB debugging, and run `doctor`.

Future signed GUI releases will require separate `osx-arm64` and `osx-x64` app bundles, a Developer ID Application certificate, hardened runtime signing, notarization, and stapling. Those packaging steps are intentionally outside Phase 1.

### Linux

The initial support target is Ubuntu/Debian x64 with X11 or XWayland. ADB USB access commonly requires `plugdev` membership and udev rules:

```bash
sudo usermod -aG plugdev "$LOGNAME"
sudo apt install android-sdk-platform-tools-common
```

Log out and back in after changing group membership. Other distributions use their own Android udev-rule packages. The application never invokes `sudo`; `doctor` reports permission state and leaves system configuration to the user.

## Build and test

From `desktop-setup`:

```text
dotnet restore QuestHomeSwitcher.DesktopSetup.sln
dotnet build QuestHomeSwitcher.DesktopSetup.sln --configuration Release --no-restore
dotnet run --project tests/QuestHomeSwitcher.Core.Tests --configuration Release --no-build
```

The tests are a small executable harness rather than xUnit/NUnit so the Phase 1 tree remains NuGet-free. The GitHub Actions workflow builds and runs the same tests independently on Windows, macOS, and Ubuntu.

## Commands

```text
dotnet run --project src/QuestHomeSwitcher.Cli -- doctor
dotnet run --project src/QuestHomeSwitcher.Cli -- status
dotnet run --project src/QuestHomeSwitcher.Cli -- install-switcher Quest-Home-Switcher.apk
dotnet run --project src/QuestHomeSwitcher.Cli -- open
dotnet run --project src/QuestHomeSwitcher.Cli -- import "My Home.apk"
```

Useful options:

```text
--adb PATH          Explicit ADB executable or platform-tools directory
--serial SERIAL     Required when multiple authorized Android devices are connected
--sha256 HASH       Explicitly trust a verified official Switcher release digest
--name NAME         Override the Quest filename for one Home import
```

The v1.8 release APK digest is pinned in the core. A later APK must match the digest supplied through `--sha256`; verify that digest against the official release before using it.

## Planned Avalonia migration

Phase 1 establishes interfaces and behavior that do not depend on a desktop toolkit. The next structure should be additive:

```text
desktop-setup/
  src/
    QuestHomeSwitcher.Core/             domain rules, ADB orchestration, safety invariants
    QuestHomeSwitcher.Cli/              diagnostics and support fallback
    QuestHomeSwitcher.Desktop/          future Avalonia application and view models
    QuestHomeSwitcher.Infrastructure/   future release client, cache, and platform paths
  tests/
    QuestHomeSwitcher.Core.Tests/       deterministic command and safety tests
    QuestHomeSwitcher.Desktop.Tests/    future headless UI tests
```

The Avalonia UI should call the same Core services asynchronously instead of duplicating ADB commands. It can reproduce the existing status cards, import review, result window, Official Library, and setup flow while keeping platform-specific packaging at the outer edge.

Before replacing the Windows setup, the new implementation still needs release-catalog/update support, Avalonia UI parity, signed packages, and real-Quest smoke tests on all three operating systems.
