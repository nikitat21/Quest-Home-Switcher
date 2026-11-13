# v2.3.1 · Compatibility and download fixes

[← Overview](../README.md) · [Download](DOWNLOAD.md) · [Installation](INSTALLATION.md)

- **More flexible Horizon OS compatibility.** Newer builds can use the existing guarded adapter; older supported builds keep their established switching path.
- **Fixed a Home download/install failure**, including Quest Dome on Root, when Android assigns a duplicate filename. Official and Community downloads now use the final saved file.
- Improved connection diagnostics and Apply/reconnection feedback.

Runtime, file integrity and recovery checks remain active. This does not guarantee compatibility with every future Horizon OS change. Both Home Libraries are unchanged.

## Updating

Use the **in-app update**, or install over your current app. Settings, pairing and downloaded Homes stay in place.

**First PC installation:** `adb install -g Quest-Home-Switcher-2.3.1.apk`

**Manual update:** `adb install -r -g Quest-Home-Switcher-2.3.1.apk`

Android version **2.3.1**, versionCode **200250**, package `app.questhomeswitcher`, signed with the existing official release certificate. [Release files and checksums](https://github.com/nikitat21/Quest-Home-Switcher/releases/tag/v2.3.1).

[Previously: v2.3 seasonal effects →](RELEASE_v2.3.md)
