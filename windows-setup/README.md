# Quest Home Switcher Setup 1.8

Guided, state-aware Windows setup for Quest Home Switcher. It detects the connected headset's current state and performs only the steps that are still required.

## Distribution file

For normal installation, download and run only:

- `Quest-Home-Switcher-Setup-v1.8.exe`

The Switcher APK is embedded in the EXE. At launch, both the controller and APK are extracted to a unique isolated directory below the Windows temporary folder. The launcher verifies the embedded APK SHA-256 before PowerShell starts and removes the temporary runtime directory after setup closes.

`Quest-Home-Switcher.apk` and `Official-Home-Library-v1.5.json` remain beside the source only as build inputs. They are not required beside the finished EXE. The small catalog is embedded as an offline fallback; the Home APKs themselves are separate downloads and are never embedded in the EXE.

## State-based behavior

1. Detects one connected and USB-authorized Quest.
2. Checks whether Shizuku is missing, installed but stopped, or verifiably running.
3. A running server is accepted only when `/proc/<pid>/cmdline` identifies `shizuku_server` and `/proc/<pid>/status` reports Android shell UID 2000.
4. If verified running, Shizuku is not restarted, updated, downgraded, force-stopped, or otherwise modified. The setup goes directly to Quest Home Switcher.
5. If installed but stopped, the setup tries the native starter included in the installed Shizuku APK. If that does not succeed, it opens Shizuku for the user. It never uninstalls Shizuku.
6. If missing, it uses the Quest-compatible Shizuku 11.7 one-time pairing screen, upgrades in place to the latest official Shizuku, and preserves pairing data.
7. Extracts the embedded `Quest-Home-Switcher.apk`, installs or updates it, verifies its installed package/version, and opens it.

### Safe signing-key migration

An installed Switcher with the current package ID may use a different signing key and therefore cannot be updated in place. Setup first attempts the normal non-destructive update. Only when Android explicitly returns `INSTALL_FAILED_UPDATE_INCOMPATIBLE` does it show a clearly scoped **Yes/No warning**. If the user agrees, setup removes only `io.github.nikitat21.questhomeswitcher` and retries the verified release payload. If the user declines, the installed Switcher remains unchanged.

This migration never uninstalls, stops, updates, or reconfigures Shizuku. Shizuku pairing and Home APK files under `Download/Quest Homes` remain untouched. Removing the old Switcher does clear only that app's settings, which is stated before confirmation.

## Optional Home APK import

`IMPORT HOME APKS` is independent from Shizuku setup. It lets the user select multiple APKs on Windows and uploads only content-verified Quest Homes to:

```text
/sdcard/Download/Quest Homes
```

The file picker opens a detected Quest Home Editor `Cooked` folder when available. If no `Cooked` folder is found, it uses the previous selection or the user's Downloads folder. Select one or more compatible NoRoot-Spoof Home APKs, edit the highlighted **Name on Quest** fields if needed, select **CONTINUE TO IMPORT**, review the result window, and then select **Refresh** in Quest Home Switcher.

An APK is accepted only when:

- it is a readable ZIP/APK containing the exact entry `assets/scene.zip`; and
- its `AndroidManifest.xml` bytes contain `com.meta.shell.env.footprint.haven2025` as UTF-8 or UTF-16LE.

Before any upload, the review window shows the original file, detected Home, and an always-visible, accent-bordered **Name on Quest** text field for every selected APK. Multi-selection remains active. On **CONTINUE TO IMPORT**, target names are normalized to a safe, readable `.apk` filename and accepted immediately; automatic cleanup does not require a second Continue click. Only duplicate names block the import until the user makes them unique.

After the transfer, a dedicated result window shows totals for imported, already-present, incompatible, and failed files. Its scrollable table gives every selected file a plain-language status and short result, followed by the instruction to open Quest Home Switcher and select **Refresh**.

The setup carries the same Home catalog as the Android app (`OfficialHomeCatalog.kt`): 44 known SHA-256 values of the **decompressed `assets/scene.zip` entry** resolve to their real display names, such as Blue Hill Gold Mine, Crystal Atrium, Cyber City, Cascadia, and Meta Horizon Terrace. This scene hash remains reliable for spoofed APKs that all share the same target package. A valid unknown Custom Home receives a readable suggestion derived from its original filename, which the user can change before continuing.

Existing files are never silently replaced: an identical remote APK SHA-256 is skipped, while different or unverifiable collisions receive `-2`, `-3`, and so on. Failed or ambiguous remote existence checks abort safely. Every uploaded file is size-verified and also SHA-256-verified when `sha256sum` is available on the Quest.

## Optional Official Meta Home Library

`OFFICIAL HOME LIBRARY` is an independent ADB-only tool. The embedded catalog provides a verified offline fallback and currently exposes 20 known pre-v81 official Homes: 14 tested entries are installable, while Cascadia, Futurescape, Meta Horizon Terrace, Mogu Hall, Oceanarium, and Storybook fail closed as **Coming soon**.

Opening the Library checks the project's public `homes-vX.Y.Z` prerelease channel. Library releases are intentionally GitHub prereleases so they never replace the latest final application release seen by older setup versions. The application updater accepts only final `vX.Y[.Z]` releases, while the Library accepts only non-draft `homes-vX.Y.Z` prereleases.

The catalog supplies exact asset names, byte sizes, and SHA-256 values. Setup verifies those fields against GitHub's published asset digests before enabling a Home. Selected APKs are downloaded individually into a verified local cache and transferred to `/sdcard/Download/Quest Homes/Official Library`. The UI distinguishes **Not installed**, **Installed - up to date**, and **Update available**. New files and confirmed updates use a temporary `.part` file; updates also keep a temporary rollback copy until the final Quest hash verifies. Personal imports outside this managed folder are never replaced. A corrected or newly completed Home can therefore be published through a newer Library catalog without replacing the setup EXE.

`UPDATE / OPEN SWITCHER` is a second optional ADB-only tool. It checks the official GitHub Release for a newer setup or APK, accepts only assets whose GitHub-provided SHA-256 digest verifies, and otherwise keeps using the embedded offline payload. It installs the APK only when the Quest has an older or missing Switcher, verifies the installed package, and opens it. A remotely downloaded payload never triggers automatic signing-key migration. Both optional tools require only an authorized USB/ADB Quest connection. They never call Shizuku detection, pairing, upgrade, or native-starter functions.

Android deliberately requires the one-time pairing code and Shizuku permission confirmation to be completed by the user inside the headset.

## Build

No Android Studio is needed to build the Windows setup:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Build.ps1
```

`Build.ps1` uses the Windows .NET Framework C# compiler and embeds both the PowerShell UI/controller and APK. For its local-only self-test, it copies **only the EXE** to an isolated temporary directory. The EXE must extract and validate its own embedded APK there. The test does not contact or alter a Quest.

## Safety rules

- No automatic Shizuku uninstall exists in this version.
- A conflicting installation of the current Switcher package is removed only after Android reports a signing-key mismatch and the user explicitly approves the clearly scoped warning.
- The migration command is restricted to `io.github.nikitat21.questhomeswitcher`; declining it performs no uninstall.
- The Home importer does not rely on APK file names to decide compatibility.
- Shizuku comes only from official `RikkaApps/Shizuku` GitHub releases.
- Platform Tools come only from Google.
- The embedded Switcher payload is the permanently signed `1.8` build. Its version and SHA-256 are pinned in both the setup script and one-file launcher.
- The embedded Library fallback is also SHA-256 pinned in the setup script, one-file launcher, and build script.
