# Quest Home Switcher 1.0

Quest Home Switcher 1.0 is the stable first release for Meta Quest 2, Quest 3, and Quest Pro.

## Highlights

- Quest-friendly Home library with search, refresh, active-Home status, and clear actions.
- Root and Shizuku modes in one app.
- Reliable Shizuku status and permission handling.
- Fast Home scanning after the first scan.
- Only compatible Home APKs containing `assets/scene.zip` are shown.
- Duplicate scenes are grouped automatically.
- Every switch is validated and verified.
- If installation fails, the previous Home is restored automatically when possible.
- A successful switch reloads Horizon Home without a normal headset reboot.
- Guided Windows setup with Shizuku assistance and multi-file Home import.

## Import Homes

Open the Windows setup and select **IMPORT HOME APKS**. The picker opens a detected Quest Home Editor `Cooked` folder when available, otherwise the last location or Downloads. Select one or more NoRoot-Spoof Home APKs, review the editable names, confirm the import, and then select **Refresh** in the headset app.

## Requirements

- Meta Developer Mode and USB debugging for installation.
- A running Shizuku service on an unrooted Quest, or working Magisk/`su` root access.
- Compatible user-provided Home APKs.

This project is unofficial and is not affiliated with Meta, Shizuku, Quest Home Porter, or Quest Home Editor. Home APKs are not included.
