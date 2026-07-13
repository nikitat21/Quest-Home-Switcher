# Contributing

This is currently a private pre-release repository. Contributions are welcome from invited collaborators while public licensing and distribution are being resolved.

## Before starting

- Open or comment on a private issue before a large change.
- Keep changes focused on one problem.
- Never commit signing keys, passwords, pairing codes, device identifiers, local user paths, or generated release binaries.
- Do not commit Quest Home APKs, extracted Meta assets, paid assets without redistribution rights, or other proprietary content.
- Preserve the state-aware safety behavior: a verified running Shizuku server must not be restarted, downgraded, re-paired, or uninstalled.

## Repository layout

```text
android-app/       Kotlin/Jetpack Compose Quest application
windows-setup/     PowerShell/WPF setup and C# launcher
docs/              User and release documentation
```

## Android changes

Use Java 17 and the included Gradle wrapper:

```powershell
cd android-app
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Add or update unit tests when changing activation, rollback, root, Shizuku, validation, caching, or naming behavior. Keep shell commands bounded by real timeouts and collect both output streams safely.

## Windows setup changes

The setup must continue to work on stock Windows PowerShell without requiring Android Studio.

```powershell
cd windows-setup
powershell -NoProfile -ExecutionPolicy Bypass -File .\QuestHomeSwitcherSetup.ps1 -SelfTest -DistributionRoot .
```

When building the one-file EXE, use the documented build script and verify that the embedded APK version and SHA-256 pins match the intended signed release. Do not replace the pinned APK with a debug build.

## Pull requests

A pull request should include:

- a short problem statement;
- the user-visible behavior before and after the change;
- safety or rollback impact;
- tests that were run;
- Quest model, Horizon OS version, and Root/Shizuku mode for device-tested changes; and
- screenshots only when they contain no personal information.

Keep generated Gradle output, setup EXEs, APKs, local caches, and temporary device files out of commits. Release binaries belong on the matching GitHub Release after signing and verification.

## Documentation style

- Write user-facing documentation in simple English.
- Use the exact labels shown in the app or setup.
- Separate required steps from optional troubleshooting.
- Do not imply that Android security confirmations can or should be bypassed.
