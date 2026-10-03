# v2.1.5 · Home tools and easier updates

[← Overview](../README.md) · [Download v2.1.5](DOWNLOAD.md) · [Installation](INSTALLATION.md)

- **Visual Boost:** a sharper resolution preset, automatic Home reload and a clear enabled state.
- **Restore Haven:** bring back the original Meta Home package with device-matched, verified files and a protected backup. Downloaded Library Homes are kept.
- **Automatic update notices:** available app updates and their changes appear when you open QHS. Installation still needs your approval.
- **Bounded update checks:** a stalled request ends with a retry option instead of remaining on Checking indefinitely.
- **Home-state fixes:** improved NoRoot loaded-Home reporting, a corrected City 17 preview, and fixes for curated Custom Home update/removal tracking.

**Already using v2?** Use **Check Update** in your existing app. No uninstall is needed; keep your settings, ADB pairing and downloaded Homes.

**New installation from a PC:** `adb install -g Quest-Home-Switcher-2.1.5.apk`

## About the new Home tools

Visual Boost lasts until the headset restarts. Higher resolution can affect performance and other VR apps; QGO may override the setting.

Restore Haven changes the current Home and restarts the Home environment. It checks the device, firmware and original file before making changes. It requires a supported Quest 2 / 3 / 3S on compatible v206-or-newer firmware. If it reports a recovery issue, keep its backup and use the status/recovery guidance rather than repeatedly deleting packages.

The APK is now approximately **137 MiB** because the original recovery files are included. The two Libraries are unchanged by this app release.

Hands-on acceptance for this release was on Quest 3 with NoRoot, including Restore and an actual in-app update. Root and Quest 2 / 3S paths were reviewed and covered by automated checks, not newly confirmed on those devices.

<details>
<summary>Technical version</summary>

Android version `2.1.5`, versionCode `200221`, package `app.questhomeswitcher`, signed with the existing official release certificate. Checksums are available on the [release page](https://github.com/nikitat21/Quest-Home-Switcher/releases/tag/v2.1.5).

</details>
