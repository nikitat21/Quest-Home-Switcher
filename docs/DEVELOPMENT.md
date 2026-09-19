# Development · v2

[← Overview](../README.md) · [Contributing](../CONTRIBUTING.md)

QHS is a native **Kotlin / Jetpack Compose** Android panel app, with a small C++ ADB transport. It does not use Unity or need a separate desktop installer.

## Release preparation

This public branch currently contains the new documentation. The validated v2 source and release files remain in private preparation until launch. Legacy implementation and setup tools are preserved in the [archive](ARCHIVE.md), not presented as the current app.

No app build or publication is triggered by this documentation update.

## What matters in a change

Keep Root and NoRoot paths independently testable. Preserve authenticated sessions, validate files before package operations, retain recovery evidence, and check what the interface shows while an operation is running or has failed.

Home downloads and app updates are separate systems. Stable Home IDs, unique Root package names, signed metadata and byte-exact hashes let one Home be updated without replacing the whole Library.

Use the pinned toolchain and the exact release build profile once the v2 source is available. Keep R8 mappings and native symbols matched to their APK. A successful host test is not a claim that every firmware or Root implementation has been tested.
