# Local verification report

Date: 2026-07-13

The final setup UI was launched on Windows while the authorized Quest was connected. The window appeared as `Quest Home Switcher Setup`, remained responsive, and closed cleanly. The running Shizuku server was not stopped, restarted, updated, downgraded, paired, or reconfigured.

## Passed checks

- PowerShell parser accepted `QuestHomeSwitcherSetup.ps1` and `Build.ps1`.
- WPF XAML loaded successfully in STA mode.
- Required controller functions were present.
- The Switcher build-input APK matched the pinned SHA-256.
- The APK was embedded as a C# manifest resource.
- The launcher extracted the APK to a unique private temporary runtime and verified its SHA-256 before starting PowerShell.
- The finished EXE was copied by itself to an isolated directory with no sibling APK; its embedded-payload self-test returned exit code 0.
- Mocked state tests passed for:
  - verified `shizuku_server`, Android shell UID 2000 -> `Running`
  - installed package without server -> `InstalledStopped`
  - missing Shizuku package -> `Missing`
  - wrong process UID -> rejected as `InvalidProcess`
  - installed Switcher version code 12 and version `1.3.0` -> `Current`
  - older Switcher version code 10 -> `Outdated`
  - same version code with an unexpected version name -> `Outdated`
  - genuinely newer Switcher version code -> `Current`
- Mock Home APK validator tests passed for:
  - UTF-8 package identifier plus `assets/scene.zip` -> accepted
  - UTF-16LE package identifier plus `assets/scene.zip` -> accepted
  - missing `assets/scene.zip` -> rejected
  - wrong manifest package identifier -> rejected
  - streaming SHA-256 of the decompressed `assets/scene.zip` entry
  - known scene hash -> official Meta display name and filename
  - unknown valid scene hash -> cleaned Custom Home name from its original filename
  - official catalog contains all 44 hashes from `OfficialHomeCatalog.kt`
- Safe-name and collision tests passed for:
  - readable unsafe Windows filename cleanup
  - empty-name fallback
  - duplicate suggestions in a multi-select receive unique local suffixes
  - edited names are normalized and shown again before acceptance
  - duplicate user-edited target names are rejected
  - identical remote SHA-256 -> skip
  - different remote SHA-256 -> suffixed filename
  - failed/ambiguous remote existence lookup -> aborts without selecting an overwrite target
- The Home-name review WPF window and its editable multi-row DataGrid loaded successfully in STA mode.
- Isolation mocks replace `Get-ShizukuState` with a throwing sentinel and prove:
  - `UPDATE / OPEN SWITCHER` completes without calling it
  - `IMPORT HOME APKS` completes without calling it
- Signing-key migration mocks passed for:
  - normal `adb install -r` reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE`
  - explicit approval removes exactly `dev.codex.questhomeswitcher`, installs again, and verifies the result
  - explicit rejection leaves the old app installed and issues no uninstall command
  - the production Fast Mode -> Ensure -> migration path runs with a throwing `Get-ShizukuState` sentinel and never calls it
  - neither approved nor rejected migration invokes a Shizuku command
- The C# launcher compiled successfully and its embedded-script self-test returned exit code 0.
- Static command scan found no ADB Shizuku uninstall command.

Self-test marker:

```text
SELF_TEST_OK_XAML_OK_PAYLOAD_OK_STATE_MACHINE_OK_HOME_IMPORT_OK_PROFESSIONAL_NAMING_OK_SIGNATURE_MIGRATION_OK_FAST_MODES_NO_SHIZUKU_OK
```

## Artifact status

- The professional Home naming and explicit signing-key migration are included in the current source.
- The embedded build input is the permanently signed Quest Home Switcher `1.3.0` payload (version code 12).
- Embedded APK SHA-256: `A500F308DB4B997BC8BE8C555963D76B201114FF04F39790C50288CAEF7B34F8`
- Final one-file EXE SHA-256: `2F36A994860B0BA3A479494ECE658EC995EDA100718668C0B83E6849C8FE83F1`
- The final launcher was corrected to hide only the console allocation while allowing the WPF setup window itself to remain visible.

## Connected Quest verification

- Quest Home Switcher `1.3.0` installed successfully with the permanent release certificate.
- The Switcher recognized Shizuku immediately; the verified `shizuku_server` PID stayed `6980` through installation, switching, setup work, and Home organization.
- A real V12 -> Blue Hill Gold Mine -> V12 round trip passed. Both installed results were checked against the decompressed `assets/scene.zip` SHA-256.
- Fourteen verified Home APKs were moved into `/sdcard/Download/Quest Homes`, each with full APK and scene hash verification after the move.
- The Switcher then reported `14 found`, `Shizuku connected`, and `Active: Dampfstadt V12 Quest Test` from the new folder.
- Five unrelated APKs remained loose in Download and were not treated as Homes.

The one-file launcher mechanism was proven independently before this source-only naming pass:

```text
ISOLATED_FILES=Quest-Home-Switcher-Setup.exe
SELFTEST_EXIT=0
RUNTIME_CHILD_COUNT=0
```

## Remaining release work

- Ask an external tester to exercise the clean first-time pairing and installed-but-stopped Shizuku paths. Those paths remain covered by mocks; they were intentionally not forced on the owner's currently stable Shizuku installation.
- Code-signing the Windows EXE is optional for private testing but required to remove the Windows "Unknown publisher" warning.
- A GitHub update endpoint is intentionally not included yet; establish the repository and permanent signing identity first.
