# Quest Home Switcher 1.1

Version 1.1 is a reliability hotfix for Home activation on different Meta Quest OS builds.

## Fixed

- Home activation no longer fails merely because Android reports the installed carrier path late.
- The verifier supports all known `pm path` and `cmd package path` variants and checks every reported APK path.
- Scene hashes are compared case-insensitively and still must match exactly.
- The same robust integrity check runs after Horizon Home reload.
- Failure details now include expected and observed scene hashes plus Android's reported package paths.
- Automatic rollback remains enabled. Single-APK Homes are backed up, while split-package carriers safely fall back to Meta's installed system Home instead of creating an incomplete backup.

## Also improved

- Shizuku state refresh is more reliable after startup.
- Home operations cannot overlap and leave stale status behind.
- Meta Shell Debug settings can be opened directly from the app.

Users of setup 1.0 must download setup 1.1 once. Setup 1.1 introduces the verified update path used by later releases.
