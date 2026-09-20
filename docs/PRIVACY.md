# Privacy & permissions

[← Overview](../README.md) · [Setup](INSTALLATION.md) · [Security](../SECURITY.md)

This describes the current v2 implementation, not the legacy app. QHS has no analytics or advertising SDK in this build, but it is **not a network-free app**.

## What connects where

| Feature | Data path |
| --- | --- |
| Catalogs, Home APKs and app updates | Requests to the configured GitHub repositories, API and release-asset delivery hosts. |
| Community artwork | Requests to supported image hosts, including SideQuest and Vimeo image CDNs, when an image is not cached. |
| Wireless ADB | A local authenticated connection to the headset's Android debugging service. |
| Optional support logs | An export you start from your computer. Nothing is automatically sent to the developer. |

Network providers can see normal request information, such as the source IP and requested resource. Their own privacy policies apply. Opening an external link uses your browser and the destination's policies.

## What automatic setup can access

Automatic pairing temporarily enables QHS's own accessibility helper for an active setup session. Its package scope is **Android Settings** (`com.android.settings`). It navigates the relevant setup controls and reads the visible pairing-code widget; it is not a general keyboard logger or a system-log reader.

The code is used for pairing in memory. The helper stops after capture, cancellation or timeout and removes its own temporary enablement while preserving other accessibility services. Automatic setup can still require Android Wi-Fi/system confirmations.

## Why settings permission is requested

`WRITE_SECURE_SETTINGS` supports the automatic setup path. On tested Quest firmware, a PC installation with `adb install -g` grants the declared permission. After a direct Quest install, QHS can instead attempt a verified grant through its own successfully paired ADB connection.

That later grant is optional: refusal must not disable an otherwise healthy manual connection. Deliberate revocation after a known previous grant is respected. This permission does not root the device or bypass Meta account/device Developer Mode requirements.

## What stays on the headset

App-private data includes preferences, ADB pairing identity and saved Home details. Any retained GitHub access credentials are encrypted using Android Keystore-backed AES-GCM storage. Encryption is not a guarantee against a compromised rooted device.

Home APKs in the shared **Downloads → Quest Home Switcher** folders are separate from app-private storage. Updating QHS reuses them; uninstalling QHS clears its private state, not an intentional cleanup of those Home folders. [Files and migration](MIGRATING-TO-V2.md)

## Installation, Root and diagnostics

Home operations use the verified ADB or Root backend. App updates use Android's package installer and its confirmation. QHS does not provide a Root exploit.

The app writes operation diagnostics to Android Logcat; it does not need `READ_LOGS` or read system logs to automate setup. The optional PC helper collects version information and app-filtered events/crash output. Review an export before choosing where to send it. [Support logs](RELEASE-SUPPORT-LOGS.md)
