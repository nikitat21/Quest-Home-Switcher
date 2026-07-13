# Quest Home Switcher v1.3.0

Release date: 2026-07-13

Version 1.3.0 is the first cleaned, permanently signed release of the redesigned Quest Home Switcher and its state-aware Windows setup.

## Highlights

- Redesigned Quest UI with a calmer two-pane Home library, search, explicit status, and clear actions.
- Reliable separation of Root, missing Shizuku, stopped Shizuku, missing permission, and ready states.
- Faster library refresh through scene metadata caching and duplicate grouping by actual scene content.
- Strict `assets/scene.zip` validation before activation.
- Rootless install verification and automatic rollback to the previous Home when possible.
- Direct root switching through `oculuspreferences` with VR Shell reload.
- Guided Windows setup that leaves a verified running Shizuku server untouched.
- One-time Quest-compatible Shizuku pairing flow with in-place update from the official RikkaApps release.
- Optional multi-file Home import with official-name detection, editable filenames, collision protection, and upload verification.
- Safe migration from an older debug-signed Switcher only after explicit user approval.

## Release files

| File | SHA-256 |
| --- | --- |
| `Quest-Home-Switcher-Setup-v1.3.0.exe` | `2F36A994860B0BA3A479494ECE658EC995EDA100718668C0B83E6849C8FE83F1` |
| `Quest-Home-Switcher-v1.3.0.apk` | `A500F308DB4B997BC8BE8C555963D76B201114FF04F39790C50288CAEF7B34F8` |
| `Quest-Home-Switcher-v1.3.0-SOURCE.zip` | `5223F617A532288429206F3E181A764913E65441C6F7E0FA62F5F67107FB15C7` |

The Android release certificate SHA-256 fingerprint is:

```text
85569394C59B355E850C540AC8B3247E27FBDE16235CE20E95BCEAD337D93F75
```

Verify a downloaded file in PowerShell:

```powershell
Get-FileHash -Algorithm SHA256 .\Quest-Home-Switcher-Setup-v1.3.0.exe
Get-FileHash -Algorithm SHA256 .\Quest-Home-Switcher-v1.3.0.apk
```

## Recommended installation

1. Download the setup EXE and `SHA256SUMS.txt` from the same private GitHub Release.
2. Verify the EXE hash.
3. Connect and authorize the Quest over USB.
4. Run setup and choose **SET UP / REPAIR**.
5. Follow [Installation](INSTALLATION.md) for first-time Shizuku pairing, or use the APK directly on a rooted Quest.

## Upgrade note

Version 1.3.0 uses the permanent release signing certificate and Android `versionCode` 12.

An older debug-signed build cannot be updated in place even when its version number is lower. The Windows setup first attempts a normal update. Only when Android reports a signing-key mismatch does it offer to remove exactly `dev.codex.questhomeswitcher` and install the signed release. Declining leaves the old installation unchanged.

## Validation completed

- Signed APK version and certificate verified.
- Android unit tests and lint completed successfully.
- Windows setup self-test completed successfully.
- Embedded APK hash pin verified in the setup launcher and controller.
- Upgrade installation verified without stopping the already-running Shizuku server.
- Source archive checked for signing keys, passwords, local user paths, APKs, and EXEs.

## Known limitations

- Shizuku must be started again after a full headset reboot.
- Some Horizon OS builds require the Wireless debugging switch to be toggled off and on once when Shizuku keeps searching.
- Rootless switching requires compatible NoRoot-Spoof Home APKs.
- Third-party Home compatibility varies and cannot be guaranteed.
- Root behavior depends on the installed root framework and firmware.

No Home APKs or Meta proprietary Home content are included in this release.
