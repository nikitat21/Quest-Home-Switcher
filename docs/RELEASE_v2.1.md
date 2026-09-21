# v2.1 · A small reliability update

[← Overview](../README.md) · [Download v2.1](DOWNLOAD.md) · [Installation](INSTALLATION.md)

- Fixed a signing conflict that could stop the first Home switch.
- Hardened ADB setup and recovery without resetting healthy connections after an installation error.
- Added safer base-Home checks and clearer feedback when a Home cannot load.
- Improved Library download progress and error handling, and simplified the active-Home message.

**Already on v2?** Use **Check Update** in the app. Keep your app and Home folders; no uninstall is needed.

**New installation from a PC:** `adb install -g Quest-Home-Switcher-v2.1.apk`

Same app, same Libraries. [Explore the full v2 features →](RELEASE_v2.md)

<details>
<summary>Technical version</summary>

The public release is **v2.1**. The tested APK retains Android version `2.0.2`, versionCode `200214`, package `app.questhomeswitcher` and the official release signing certificate. The updater uses versionCode to detect this update.

</details>
