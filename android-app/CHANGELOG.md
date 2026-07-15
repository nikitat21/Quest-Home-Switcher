# Changelog

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
