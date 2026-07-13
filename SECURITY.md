# Security policy

Quest Home Switcher performs privileged local operations through Root or Shizuku. Treat reports involving package installation, shell commands, path validation, update integrity, or rollback as security-sensitive.

## Supported version

Only the newest stable release is actively reviewed:

| Version | Supported |
| --- | --- |
| 1.0 | Yes |
| Development builds | No |

## Reporting a vulnerability

Use **Security -> Report a vulnerability** when vulnerability reporting is available. Otherwise contact the repository owner before sharing exploit details.

Do not publish a proof of concept, device identifier, personal path, pairing code, password, signing key, keystore, non-redistributable Home APK, or proprietary Meta content in an issue or pull request.

Please include:

- affected component and version;
- Root or Shizuku mode;
- Quest model and Horizon OS version;
- exact prerequisites and reproduction steps;
- expected and observed impact; and
- a minimal sanitized log or test case when possible.

Allow time for the report to be reproduced and a fix to be prepared before disclosure.

## Security boundaries

- The Quest app has no analytics, advertising, account system, or network communication of its own.
- The Windows setup downloads Platform Tools only from Google and Shizuku only from the official RikkaApps GitHub release source when required.
- A verified running Shizuku server must be left untouched by setup.
- Release APK and setup payload hashes are pinned and verified.
- Release signing keys and passwords must remain offline and outside the repository.
- Home APKs are user-supplied and outside the project's trust boundary. Validation proves expected structure, not that third-party code is harmless.
- Root frameworks, Horizon OS, Shizuku, ADB, and third-party Home packages have their own security models and are not maintained by this project.
