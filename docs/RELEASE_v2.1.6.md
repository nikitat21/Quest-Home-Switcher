# v2.1.6 · Your Homes, closer

[← Overview](../README.md) · [Download](DOWNLOAD.md) · [Installation](INSTALLATION.md)

- **Quest Library shortcut:** add QHS to your normal Library through Meta Browser. A short, optional introduction appears after updating; the option remains in **Home tools**. Opening the shortcut brings you back to the wide app panel.
- **Refreshed Official Library:** 22 Homes with matching Root and NoRoot variants, 278 seating positions across the Homes that support them, and scene, animation and effect improvements. Update the Homes you want individually in QHS; there is no forced bulk download. Bubbles keeps its already accepted files.
- **Safer Home updates:** the last signed Official catalog survives app restarts. Older or incomplete online responses cannot replace an accepted newer catalog. An update offer also keeps **Apply installed** available for a verified local Home when no other operation is running.
- **Experimental Horizon OS v2.9 support:** a dedicated NoRoot preparation path for VrShell 209, including the final Cascadia and Cyber City scenes and a Space Station skybox correction. QHS still replaces Haven; it does not replace a Vista slot. Root and the existing v207 path stay separate.

**Already using v2?** Update in the app, or install over it with `adb install -r -g`. Keep your app data, pairing and Home folders. Afterward, choose individual **Update Home** actions inside the Library.

**First install from a PC:** `adb install -g Quest-Home-Switcher-2.1.6.apk`

## Compatibility note

Switching and the Library shortcut were checked on a physical Quest 3 with v207. The v209 path passed controlled Prism emulator tests for representative Homes; all 22 final Official inputs also passed local adapter preparation and signing checks. This is **not yet a physical v209 acceptance test**. Root, Quest 2 and Quest 3S were not newly hardware-tested for this release. Firmware, Root solutions and scene behavior can differ; please include your exact versions when reporting a problem.

Home updates are optional. NoRoot updates replace the downloaded file; apply it afterward to use the new scene. Root updates use the guarded package-install path. Existing verification and recovery protections remain enabled.

<details>
<summary>Technical version</summary>

Android version `2.1.6`, versionCode `200230`, package `app.questhomeswitcher`, signed with the existing official release certificate. See the [release files and checksums](https://github.com/nikitat21/Quest-Home-Switcher/releases/tag/v2.1.6).

</details>
