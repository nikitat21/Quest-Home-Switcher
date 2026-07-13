# Quest Home Switcher Setup 1.0

Guided, state-aware Windows setup for Quest Home Switcher. It detects the connected headset's current state and performs only the steps that are still required.

## Distribution file

For normal installation, download and run only:

- `Quest-Home-Switcher-Setup-v1.0.exe`

The Switcher APK is embedded in the EXE. At launch, both the controller and APK are extracted to a unique isolated directory below the Windows temporary folder. The launcher verifies the embedded APK SHA-256 before PowerShell starts and removes the temporary runtime directory after setup closes.

`Quest-Home-Switcher.apk` remains beside the source only as a build input. It is not required beside the finished EXE.

## State-based behavior

1. Detects one connected and USB-authorized Quest.
2. Checks whether Shizuku is missing, installed but stopped, or verifiably running.
3. A running server is accepted only when `/proc/<pid>/cmdline` identifies `shizuku_server` and `/proc/<pid>/status` reports Android shell UID 2000.
4. If verified running, Shizuku is not restarted, updated, downgraded, force-stopped, or otherwise modified. The setup goes directly to Quest Home Switcher.
5. If installed but stopped, the setup tries the native starter included in the installed Shizuku APK. If that does not succeed, it opens Shizuku for the user. It never uninstalls Shizuku.
6. If missing, it uses the Quest-compatible Shizuku 11.7 one-time pairing screen, upgrades in place to the latest official Shizuku, and preserves pairing data.
7. Extracts the embedded `Quest-Home-Switcher.apk`, installs or updates it, verifies its installed package/version, and opens it.

### Safe signing-key migration

An older test Switcher may use a legacy package or a different signing key. Setup installs and verifies the release first, then shows a clearly scoped **Yes/No warning** before removing only the legacy test app. If the user declines, the old Switcher remains installed.

This migration never uninstalls, stops, updates, or reconfigures Shizuku. Shizuku pairing and Home APK files under `Download/Quest Homes` remain untouched. Removing the old Switcher does clear only that app's settings, which is stated before confirmation.

## Optional Home APK import

`IMPORT HOME APKS` is independent from Shizuku setup. It lets the user select multiple APKs on Windows and uploads only content-verified Quest Homes to:

```text
/sdcard/Download/Quest Homes
```

The file picker opens a detected Quest Home Editor `Cooked` folder when available. If no `Cooked` folder is found, it uses the previous selection or the user's Downloads folder. Select one or more compatible NoRoot-Spoof Home APKs, review the editable names, confirm the import, and then select **Refresh** in Quest Home Switcher.

An APK is accepted only when:

- it is a readable ZIP/APK containing the exact entry `assets/scene.zip`; and
- its `AndroidManifest.xml` bytes contain `com.meta.shell.env.footprint.haven2025` as UTF-8 or UTF-16LE.

Before any upload, a review window shows every selected APK, its detected Home name, how it was identified, and its editable target filename. Multi-selection remains active. Target names are normalized to a safe, readable `.apk` filename and must be unique inside the selected batch.

The setup carries the same Home catalog as the Android app (`OfficialHomeCatalog.kt`): 44 known SHA-256 values of the **decompressed `assets/scene.zip` entry** resolve to their real display names, such as Blue Hill Gold Mine, Crystal Atrium, Cyber City, Cascadia, and Meta Horizon Terrace. This scene hash remains reliable for spoofed APKs that all share the same target package. A valid unknown Custom Home receives a readable suggestion derived from its original filename, which the user can change before continuing.

Existing files are never silently replaced: an identical remote APK SHA-256 is skipped, while different or unverifiable collisions receive `-2`, `-3`, and so on. Failed or ambiguous remote existence checks abort safely. Every uploaded file is size-verified and also SHA-256-verified when `sha256sum` is available on the Quest.

`UPDATE / OPEN SWITCHER` is a second optional ADB-only tool. It checks the embedded payload, installs it only when the Quest has an older or missing Switcher, verifies the installed package, and opens it. If Android detects the old debug signing key, the same explicit migration confirmation appears. Both optional tools require only an authorized USB/ADB Quest connection. They never call Shizuku detection, pairing, upgrade, or native-starter functions.

Android deliberately requires the one-time pairing code and Shizuku permission confirmation to be completed by the user inside the headset.

## Build

No Android Studio is needed to build the Windows setup:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Build.ps1
```

`Build.ps1` uses the Windows .NET Framework C# compiler and embeds both the PowerShell UI/controller and APK. For its local-only self-test, it copies **only the EXE** to an isolated temporary directory. The EXE must extract and validate its own embedded APK there. The test does not contact or alter a Quest.

## Safety rules

- No automatic Shizuku uninstall exists in this version.
- A conflicting or legacy Switcher is removed only after a clearly scoped warning and explicit user approval; legacy cleanup runs only after the release install is verified.
- The migration command is restricted to the known legacy Quest Home Switcher package; declining it performs no uninstall.
- The Home importer does not rely on APK file names to decide compatibility.
- Shizuku comes only from official `RikkaApps/Shizuku` GitHub releases.
- Platform Tools come only from Google.
- The embedded Switcher payload is the permanently signed `1.0` build. Its version and SHA-256 are pinned in both the setup script and one-file launcher.
