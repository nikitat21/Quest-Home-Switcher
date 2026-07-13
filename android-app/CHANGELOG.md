# Changelog

## 1.3.0 - 2026-07-13

- Reworked the headset UI around a calmer two-pane Home library and explicit actions.
- Split root, missing Shizuku, stopped server, missing permission and ready states.
- Replaced duplicate Shizuku listeners and aggressive polling with centralized binder events and bounded checks.
- Fixed Shizuku Manager launch retries after a failed first attempt.
- Hardened root commands with parallel output handling, real timeouts and safe process cleanup.
- Added strict `assets/scene.zip` validation and content-based duplicate grouping.
- Added persistent metadata caching for much faster refreshes of unchanged libraries.
- Detects the active Home from its embedded scene instead of hashing every complete APK.
- Added pre-install validation, post-install verification and automatic rollback.
- Removed the unnecessary reboot prompt after a successful Horizon Home reload.
- Prepared the companion Windows setup to preserve an already-running Shizuku server and install the Switcher automatically.

## 1.2.7 - 2026-07-12

- Fixed the refresh loop that cancelled large Home APK scans every 2.5 seconds.
- Shows Shizuku online immediately while the Home library is still being scanned.
- Coalesces refresh requests so one scan always finishes before another begins.
- Refreshes automatically after returning to the app.
- Waits for the Shizuku binder before opening Shizuku Manager.
- Converts Shizuku process failures into visible activation errors instead of terminating the refresh worker.

## 1.2

- Added automatic Magisk/`su` root detection.
- Added direct `oculuspreferences` switching with VR Shell reload and no normal reboot requirement.
- Added discovery of installed environment packages in root mode.
- Added automatic APK discovery in Download, Quest Homes, QuestHomes, Homes, and their subfolders.
- Kept the Shizuku install-over workflow as the rootless fallback.

## 1.1 - 2026-07-06

- Restores the proven rootless hot-switch behavior from version 1.0.
- Restores normal Quest background and minimize behavior.
- Detects and marks the currently installed Home using a local hash comparison.
- Adds lightweight layered glass and depth styling to the Home deck.
- Removes the experimental transition sound, fog, and task-card changes.
- Keeps the stable Android Gradle Plugin 8.13.2 toolchain.

## 1.0 - 2026-07-05

- Added a spatial, Quest-friendly environment browser.
- Added Shizuku permission handling and launch assistance.
- Added rootless streamed installation for spoofed Home APKs.
- Added automatic uninstall of the previously active `haven2025` package.
- Added activation logs and restart fallback.
- Fixed temporary panel overlap and persistent background-task behavior.
