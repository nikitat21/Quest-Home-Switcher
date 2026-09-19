# Security

QHS v2 performs privileged local Home operations through its built-in ADB connection or verified Root access. Package installation, command execution, file paths, credentials, update integrity and rollback belong to its security boundary.

## Reporting a vulnerability

Use **Security → Report a vulnerability** when GitHub private vulnerability reporting is available for the repository. Otherwise contact the repository owner privately before sharing exploit details.

Include the affected version, Quest model, Horizon OS, Root/NoRoot path, prerequisites and a minimal reproduction. Keep credentials, signing material and unrelated personal data out of issues and attachments.

v2 is being prepared for release; the documentation is public ahead of its downloads. This is not a launch announcement or a guaranteed support period for older builds.

## Boundaries worth preserving

- A green status requires verified backend access. Root must not be inferred from cached UI or an installed Root-manager app.
- Downloaded APKs are checked against their trusted metadata before use. App updates also require the expected package identity and signing certificate.
- Home structure and integrity checks are not a guarantee that a third-party APK is harmless.
- Automatic pairing is a short-lived Android Settings accessibility session, not unrestricted background input capture. It must clean up its own temporary service enablement.
- An optional post-pairing settings-permission grant must not turn a working manual ADB connection into a failure.
- Authenticated private downloads must not leak authorization to untrusted redirect hosts. Never embed private access tokens or signing secrets in an APK.
- Preserve exact previous artifacts and version-matched diagnostic symbols for recovery. Do not silently change a Root package's identity or certificate.

The app **does use the network** for catalog data, images, Home downloads, updates and private-test authorization. It has no analytics/advertising SDK in the current build. See [privacy and permissions](docs/PRIVACY.md) for the actual data paths.

Android, Horizon OS, GitHub, Root frameworks, ADB and third-party Homes have their own security models. This project does not control them.
