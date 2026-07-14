# Quest Home Switcher v1.1

<!-- quest-home-switcher-version-code: 14 -->

Release date: 2026-07-14

Version 1.1 is a reliability hotfix for Home activation across different Meta Quest OS builds.

## Main fix

Version 1.0 could report **Verification failed** even after Android installed the selected Home successfully. Some Quest builds publish the carrier package path late, return it through a different package-manager command, or report more than one APK path.

Version 1.1 now:

- polls for up to 20 seconds while Android publishes the installed carrier, without repeatedly hashing an unchanged large APK;
- supports known `pm path` and `cmd package path` variants;
- checks every reported base or split APK path instead of trusting only the first path;
- compares the decompressed `assets/scene.zip` SHA-256 exactly and case-insensitively;
- repeats the same integrity check after Horizon Home reload; and
- records expected hash, observed hash, and package paths when verification genuinely fails.

Automatic rollback remains enabled. A readable single-APK Home is backed up and restored when installation or verification fails; split-package carriers safely fall back to Meta's installed system Home instead of creating an incomplete backup.

## Other improvements

- More reliable Shizuku state refresh after startup and foreground changes.
- Home actions are serialized so refresh and activation cannot leave stale state behind.
- Setup 1.1 introduces a verified GitHub Release update path while retaining its embedded offline APK.

## Updating from 1.0

The already-downloaded 1.0 setup cannot update its own program code retroactively. Download and run `Quest-Home-Switcher-Setup-v1.1.exe` once. Android updates the permanently signed 1.0 APK in place, preserving its app data and Shizuku permission. Later setup/APK updates can be checked from setup 1.1.

## Downloads

- **Recommended:** `Quest-Home-Switcher-Setup-v1.1.exe`
- **Manual installation:** `Quest-Home-Switcher-v1.1.apk`

No Home APKs or Meta proprietary Home content are included in this release.
