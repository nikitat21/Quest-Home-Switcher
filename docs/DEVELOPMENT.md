# Development · v2

[← Overview](../README.md) · [Contributing](../CONTRIBUTING.md)

QHS is a native **Kotlin / Jetpack Compose** Android panel app, with a small C++ ADB transport. It does not use Unity or need a separate desktop installer.

## Project layout

This public repository contains release downloads, Home Libraries and user guides. Implementation work is maintained in a separate private review repository. Contact the maintainer to discuss a code contribution or review access.

Legacy implementation and setup tools remain in the [archive](ARCHIVE.md), not in the current installation path. Source access does not grant a general license; see [LICENSE.md](../LICENSE.md).

## What matters in a change

Keep Root and NoRoot paths independently testable. Preserve authenticated sessions, validate files before package operations, retain recovery evidence, and check what the interface shows while an operation is running or has failed.

Home downloads and app updates are separate systems. Stable Home IDs, unique Root package names, signed metadata and byte-exact hashes let one Home be updated without replacing the whole Library.

Use the pinned toolchain and exact release build profile in the development workspace. Keep R8 mappings and native symbols matched to their APK. A successful host test is not a claim that every firmware or Root implementation has been tested.
