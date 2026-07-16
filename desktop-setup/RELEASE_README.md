# Quest Home Switcher CLI 1.8

This archive is the command-line setup for Linux or macOS. It already includes the permanently signed Quest Home Switcher 1.8 APK. Do not download the APK separately.

The CLI is self-contained and does not require a separate .NET installation. Android SDK Platform Tools (`adb`) must be installed separately and available through `PATH`, `QHS_ADB`, or `--adb PATH`.

## First install or update

1. Enable Meta Developer Mode and USB debugging.
2. Connect the Quest over USB and approve the debugging prompt inside the headset.
3. Extract this archive and open a terminal in its folder.
4. Make the files executable when required:

   ```sh
   chmod +x quest-home-switcher install-switcher.sh
   ```

5. Run the guided command sequence:

   ```sh
   ./install-switcher.sh
   ```

The script verifies that the connected Android device is a Meta Quest, installs the pinned APK without an automatic uninstall, and opens Quest Home Switcher.

Unrooted Quest users still need Shizuku running. This first Linux/macOS CLI does not install, pair, update, or start Shizuku; use it after Shizuku is working on the headset. Version 1.8 does not yet include the experimental built-in Wireless ADB backend. Root users do not need Shizuku.

## Useful commands

```sh
./quest-home-switcher doctor
./quest-home-switcher status
./quest-home-switcher open
./quest-home-switcher import "My Home.apk"
```

When ADB is not in `PATH`:

```sh
./quest-home-switcher --adb /path/to/adb doctor
```

The importer validates the Home APK and performs a verified, no-clobber upload to `Download/Quest Homes`.

## macOS note

These first CLI packages are not Apple-notarized. If macOS quarantines a file downloaded from GitHub, open **System Settings > Privacy & Security** and allow the app only after confirming that the archive came from the official Quest Home Switcher release and its SHA-256 matches `SHA256SUMS.txt`.

## Linux USB note

Linux may need Android udev rules and `plugdev` membership before ADB can access the Quest. The CLI never invokes `sudo` and reports the connection problem through `doctor`.
