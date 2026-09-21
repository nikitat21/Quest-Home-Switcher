<p align="center"><sub>QUEST HOME SWITCHER</sub></p>
<h1 align="center">Hello, v2.</h1>
<p align="center"><strong>Your next Home is closer.</strong><br>A fresh panel, a built-in connection and a whole community to explore.</p>
<p align="center"><a href="INSTALLATION.md">Installation</a> &nbsp;·&nbsp; <a href="MIGRATING-TO-V2.md">Moving from an older version</a> &nbsp;·&nbsp; <a href="../README.md">Overview</a></p>

> **Latest: [v2.1.1 Home-switching fix →](RELEASE_v2.1.1.md)** · [Download](DOWNLOAD.md) · [Installation guide](INSTALLATION.md)

## A fresh start. Familiar places.

v2 brings browsing, downloading and switching Homes together on the headset. No separate desktop Home importer is needed for the everyday workflow, and the NoRoot connection is built into QHS.

### 01 · A calmer place to browse

Official and Custom Homes share a clear panel-based layout, with artwork, readable descriptions, installed/active status and direct actions. The Custom catalog has larger previews, search and sorting without opening a second QHS panel. Hand and controller navigation stay in the same interface.

### 02 · Less setup, more Home

The old Shizuku dependency is replaced by QHS’s own Wireless ADB connection. With a PC installation using **`-g`**, the app attempts automatic setup from the first launch. Install directly on Quest instead, and one manual pairing gives QHS the opportunity to enable that convenience afterwards.

Working Root is detected automatically. Root users get the matching Root Home packages and their own installation/switching path, not the NoRoot carrier in disguise.

> [!IMPORTANT]
> **Installing from a PC? Keep the `-g`.**
>
> `adb install -g Quest-Home-Switcher-v2.1.1.apk`
>
> [The short installation guide →](INSTALLATION.md)

### 03 · Classics and community, side by side

The Official collection contains **22 Meta Homes**. The Community catalog adds searchable names, pictures and per-Home downloads, while your compatible NoRoot APKs can live in an easy-to-find Custom Homes folder.

The launch catalog contains **250 downloadable Community Homes**, each with Root and NoRoot variants. New Homes and updates can arrive without a new app release.

### 04 · Updates without the whole download again

The app has a signed in-app update path. Individual Library Homes can also be updated or added separately, without updating QHS or uploading the entire Library again. Installed Home cards can offer **Update Home** when the verified package differs.

You stay in control of what you download. Partial downloads and failed verification are not treated as successfully installed Homes.

### 05 · The details that make it yours

- Edit a Custom Home’s visible name and picture.
- Remove an unused Custom Home from inside the app.
- See the active Home and the real connection state.
- Reuse existing Home folders after updating or reinstalling.
- Keep support diagnostics available without cluttering the normal interface.

---

## A special thank-you, Lumi

**[Lumi / @Lumince](https://github.com/Lumince)** has helped shape v2 through Root improvements, patient on-headset testing and detailed feedback. Thank you for helping make this release better for everyone.

[Visit her profile](https://github.com/Lumince) · [Explore her projects](https://github.com/Lumince?tab=repositories)

Thanks also to [AstroBoy](https://github.com/xAstroBoy), the Home creators and everyone testing QHS. Their research, tools and creativity make this community possible.

## Before you move over

**From the older Switcher or Beta:** v2 is a separate app identity. It needs its own first setup; do not uninstall the old app just to make the new APK install. [Migration guide](MIGRATING-TO-V2.md)

**From a v2 RC:** the final app retains the RC package and official signing certificate. It is a higher-version, in-place update. Keep your RC installed and use Check Update or a manual reinstall with `-r -g`.

**Compatibility:** Quest 2 / 3 / 3S are the v2 target; primary device testing has been on Quest 3. Root and Horizon OS behavior vary. QHS is unofficial, and Home validation cannot prove third-party content harmless. [Limits and known behavior](COMPATIBILITY.md)

<details>
<summary>Technical version & release status</summary>

The public name is **v2**. Android package/version metadata and signed manifests retain their technical values; the app identity is `app.questhomeswitcher`. The technical release version is `2.0.0`. The package is `app.questhomeswitcher`, versionCode `200212`. APK checksums and the exact download are included with the [v2 release](https://github.com/nikitat21/Quest-Home-Switcher/releases/tag/v2).

</details>
