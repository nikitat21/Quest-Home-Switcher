# Quest Home Switcher v1.0

Release date: 2026-07-13

Version 1.0 is the stable, permanently signed release of Quest Home Switcher and its guided Windows setup.

## Highlights

- Quest-friendly two-pane Home library with search, status, and clear actions.
- Separate Root, Shizuku server, Shizuku permission, and ready states.
- Fast library refresh through scene metadata caching and duplicate grouping.
- Strict `assets/scene.zip` validation before activation.
- Rootless install verification and automatic rollback to the previous Home when possible.
- Direct root switching through `oculuspreferences` with VR Shell reload.
- Guided Windows setup that leaves a verified running Shizuku server untouched.
- One-time Quest-compatible Shizuku pairing flow with an in-place update from the official RikkaApps release.
- Multi-file Home import with real-name detection, editable filenames, collision protection, and upload verification.
- Safe migration from an earlier development-signed Switcher only after explicit user approval.

## Download

- **Recommended:** `Quest-Home-Switcher-Setup-v1.0.exe` guides Windows users through installation, Shizuku setup, updates, and Home import.
- **Manual installation:** `Quest-Home-Switcher-v1.0.apk` is for users who already know how to sideload, including rooted Quest users.

No additional text or checksum file is required for normal installation. Download the setup or APK from this repository's GitHub Release.

## Recommended installation

1. Download and run `Quest-Home-Switcher-Setup-v1.0.exe`.
2. Connect the Quest over USB and approve **Allow USB debugging** in the headset.
3. Choose **SET UP / REPAIR** and follow the on-screen steps.
4. Follow [Installation](INSTALLATION.md) for first-time Shizuku pairing, or install the APK manually on a rooted Quest.

## Import Home APKs

1. Open the setup and select **IMPORT HOME APKS**.
2. The picker opens a detected Quest Home Editor `Cooked` folder. If none is found, it uses the last location or Downloads.
3. Select one or more compatible NoRoot-Spoof Home APKs.
4. Review the detected names, edit them if needed, and confirm the import.
5. Open Quest Home Switcher in the headset and select **Refresh**.

## Upgrade note

An earlier development build may use a legacy package or a different signing certificate and cannot always be updated in place. The Windows setup installs and verifies the release first, then offers to remove the legacy test app. Declining leaves the old installation unchanged. Shizuku, its pairing, and Home files are not removed.

## Known limitations

- Shizuku must be started again after a full headset reboot.
- Some Horizon OS builds require the Wireless debugging switch to be toggled off and on once when Shizuku keeps searching.
- Rootless switching requires compatible NoRoot-Spoof Home APKs.
- Third-party Home compatibility varies and cannot be guaranteed.
- Root behavior depends on the installed root framework and firmware.

No Home APKs or Meta proprietary Home content are included in this release.
