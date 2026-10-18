# v2.1.7 · Quest 2 Home loading fix

[← Overview](../README.md) · [Download](DOWNLOAD.md) · [Installation](INSTALLATION.md)

- **Quest 2 on Horizon OS v2.9:** fixes the NoRoot scene connection that could leave you in the default environment or show only parts of Calming instead of the selected Home.
- **More resilient ADB setup:** waits for the connection service before opening Android Settings and handles Android background-start refusals without crashing. Automatic setup and manual code entry keep their existing security checks.
- **Clearer Home updates:** keeps Updating or Verifying visible after the download reaches 100%, until the new Home is checked and added to the library. Update Home no longer briefly reappears during that step.
- **No Library re-download needed:** your Home files, pairing and app data stay in place. Root and the existing v207 switching path are unchanged.

**Already using QHS?** Update in the app, then apply the Home again. If a previously failed Home still says Active, choose another Home first, then return. This is an app fix; the Official and Community Libraries are unchanged.

**First install from a PC:** `adb install -g Quest-Home-Switcher-2.1.7.apk`

## Validation

All 22 Official inputs passed local preparation, signing and Quest 2/3 scene-reference checks. Five representative Homes also loaded correctly in the controlled native Quest-2 Prism setup after both Apply and a separate shell restart, with a Quest-3 regression check. Physical Quest-2/v209 validation and a complete on-device test of every Home are still outstanding. Firmware and Root solutions can differ; please include your exact versions when reporting a problem.

<details>
<summary>Technical version</summary>

Android version `2.1.7`, versionCode `200232`, package `app.questhomeswitcher`, signed with the existing official release certificate. [Release files and checksums](https://github.com/nikitat21/Quest-Home-Switcher/releases/tag/v2.1.7).

</details>
