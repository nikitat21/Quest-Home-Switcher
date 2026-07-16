# Changelog

## 1.8 - 2026-07-16

- Merge multiple copies or scene revisions of the same official Home into one canonical card.
- Prefer true installed Root environments, then managed Official Library copies, then personal imports.
- Keep all known official scene hashes as active-Home aliases even when only one APK copy remains.
- Require an actual Android root UID before entering Root mode and exclude the shared NoRoot carrier package from Root-direct scanning.
- Label Home sources clearly as Root direct, Official Library, or Imported.
- Harden app-private storage boundaries for the dormant built-in Wireless-ADB research path.
- Add the first cross-platform desktop Core/CLI foundation for Windows, macOS, and Linux.
- Keep Shizuku as the stable unrooted backend; built-in Wireless ADB is not enabled in 1.8.

## Home Library hotfix homes-v1.5.1 - 2026-07-16

- Hotfixed the verified Library catalog after the online fallback issue.
- Disabled unsafe Futurescape and Mogu Hall Library downloads without touching personal imports.

## 1.5 - 2026-07-16

- Added the optional searchable Official Meta Home Library to the Windows setup.
- Added 16 separately downloadable, hash-pinned Home builds; four unfinished entries remain safely disabled.
- Added a dedicated online Library catalog channel so corrected Homes can be enabled without replacing the setup EXE.
- Kept Home APKs outside the source tree and one-file setup; only selected assets are downloaded.
- Added exact GitHub digest/size checks, verified local caching, and atomic `.part` uploads to the Quest.
- Fixed the Library table so rows, text, search, selection, and status remain clearly readable with the dark theme.
- Separated final application releases from `homes-v…` Library prereleases so setup 1.1 can still discover v1.5 correctly.
- Verified an in-place device update from app 1.1 (code 14) to 1.5 (code 15) without touching Shizuku or Home files.

## 1.1 - 2026-07-14

- Fixed false verification failures on Quest builds that publish the installed Home package path late.
- Added compatibility fallbacks for older and newer Meta package-manager path commands.
- Verify every reported base or split APK path and accept only the selected `assets/scene.zip` SHA-256.
- Added clear expected/observed hash and package-path diagnostics while keeping automatic rollback enabled.
- Avoid repeated hashing of unchanged large carrier APKs and skip unsafe partial backups of split packages.
- Made post-reload integrity verification use the same robust multi-path checks.
- Improved Shizuku status refresh and serialized Home operations to avoid stale UI state.
- Kept internal debug shortcuts out of the official release UI.

## 1.0 - 2026-07-13

- Added a Quest-friendly two-pane Home library with search, refresh, active-Home status, and clear actions.
- Added separate Root, Shizuku server, Shizuku permission, and ready states.
- Added automatic Magisk/`su` root detection and direct switching through `oculuspreferences`.
- Added recursive discovery of compatible Home APKs in Download and common Home folders.
- Added strict `assets/scene.zip` validation and content-based duplicate grouping.
- Added persistent metadata caching for faster refreshes of unchanged libraries.
- Added pre-install validation, post-install verification, and automatic rollback.
- Added Horizon Home reload after a successful switch without a normal headset reboot.
- Added the guided Windows setup with state-aware Shizuku handling and multi-file Home import.
