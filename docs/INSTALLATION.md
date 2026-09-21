# Install Quest Home Switcher v2

[← Overview](../README.md) · [Get v2.1.1](DOWNLOAD.md) · [Help](TROUBLESHOOTING.md)

> Start with the signed **[Quest-Home-Switcher-v2.1.1.apk](DOWNLOAD.md)**. The same APK supports Root and NoRoot.

## Choose your route

| Install from a PC · recommended | Install directly on Quest |
| --- | --- |
| **Use `adb install -g`.** QHS can attempt automatic Wireless ADB setup from the first launch. | One manual Wireless ADB pairing first. QHS then tries to enable the same setup convenience for later use. |
| [Start the PC guide ↓](#install-from-a-pc-recommended) | [Start the Quest guide ↓](#install-directly-on-quest) |

Already have working Root? [Read the Root path](#already-rooted). Coming from an older Switcher? [Read the migration guide](MIGRATING-TO-V2.md).

## Install from a PC (recommended)

### 1. Get ready

- A compatible Quest, connected to Wi-Fi. [Compatibility](COMPATIBILITY.md)
- Meta Developer Mode enabled for the headset, plus an authorized USB data connection.
- A Windows, macOS or Linux computer with [Google’s Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools).
- The signed **v2 app APK** from [Get v2](DOWNLOAD.md), not a Home APK or the GitHub source-code ZIP.

Enable headset Developer Mode using [Meta’s current device setup instructions](https://developers.meta.com/horizon/documentation/android-apps/enable-developer-mode/). This Meta account/device step is different from the Android Developer settings that QHS can help open later.

### 2. Connect and authorize

Connect the USB cable. Put on the headset and approve **Allow USB debugging** for your own computer. Allowing file access alone is not enough.

Open a terminal in the extracted `platform-tools` folder. For a simple first install, put the downloaded APK in that folder too. Then check the connection:

**Windows PowerShell**

```powershell
.\adb.exe devices
```

**macOS / Linux**

```sh
./adb devices
```

The Quest must show **`device`**, not `unauthorized` or `offline`. If more than one device/connection is listed, [select the correct one first](TROUBLESHOOTING.md#my-computer-cannot-find-or-authorize-the-quest).

### 3. Install — keep the -g

> [!IMPORTANT]
> **For the PC route, include `-g`.** A normal APK install can succeed without it, but the first automatic ADB setup will not have its required settings permission.

**Windows PowerShell**

```powershell
.\adb.exe install -g Quest-Home-Switcher-v2.1.1.apk
```

**macOS / Linux**

```sh
./adb install -g Quest-Home-Switcher-v2.1.1.apk
```

If `adb` is already on your PATH, the short form is:

```sh
adb install -g Quest-Home-Switcher-v2.1.1.apk
```

Wait for **`Success`**. Use the actual APK filename if it differs; quote a path that contains spaces.

<details>
<summary>What do -g and -r mean?</summary>

`-g` requests the permissions Android permits this installation path to grant. On the Quest firmware tested with QHS, it grants the declared `WRITE_SECURE_SETTINGS` permission used for automatic setup. It does **not** root the headset, enable Meta Developer Mode for your account or remove Android’s installation confirmations.

`-r` means reinstall/update an existing app while retaining its app data. It is not needed for a clean first install. For a manual update over the same v2 app, use:

```sh
adb install -r -g Quest-Home-Switcher-v2.1.1.apk
```

Do not uninstall a working RC first just to update it. A different package or signing certificate is not an ordinary update. [Migration details](MIGRATING-TO-V2.md)

References: [Android’s ADB options](https://developer.android.com/tools/adb#pm) and [Meta’s ADB installation guide](https://developers.meta.com/horizon/documentation/spatial-sdk/ts-adb/). QHS-specific permission behavior is based on its tested implementation, not a guarantee for all firmware.

</details>

### 4. Open QHS on the headset

Find **Quest Home Switcher** in your launcher or the Library’s **Unknown Sources** view, depending on Horizon OS.

QHS checks Root first. Without Root, its ADB setup opens when needed and attempts automatic pairing and connection. Keep the headset awake and follow any Android Wi-Fi/system confirmation. A system pairing screen may appear briefly; this is expected.

After a successful connection, setup closes and the **ADB** status turns green. QHS reads your Homes and creates or reuses its Home folders. If automatic setup cannot finish, the app opens the manual instructions rather than pretending it connected.

[Choose your first Home →](HOMES.md#choose-a-library-home)

## Install directly on Quest

No PC `-g` grant is available when an APK is installed through the headset’s normal package installer.

1. Download the signed v2 app APK from the release source and open it with a trusted file manager or launcher that supports APK installation.
2. Approve Android’s install-source permission if requested, then install and open **Quest Home Switcher**. Keep the Quest connected to Wi-Fi.
3. Without verified Root access, QHS shows the manual ADB setup. It tries to open **Wireless debugging** directly; if the firmware does not allow that, it opens Android Developer settings instead.
4. Turn on **Wireless debugging** and allow the current Wi-Fi network if Android asks. Select **Pair device with pairing code**. Keep that system dialog open.
5. Return to QHS and enter the current six-digit code. Use **Connect** if shown; a complete valid code may be submitted automatically. No IP address or port needs to be typed into QHS.
6. Once connected, QHS closes setup and attempts to grant its own settings permission through that authenticated local ADB connection. It verifies the result before calling automatic setup enabled.

**You can keep using QHS even if that optional permission grant is refused.** Manual connection remains available. If you deliberately revoke a permission later, QHS does not silently override that decision.

If Android Developer settings cannot be enabled on your firmware, this route may require help from the PC route. It is not a promise that every locked-down device supports a PC-free first setup.

[Manual setup help →](TROUBLESHOOTING.md#automatic-or-manual-adb-setup-does-not-finish)

## Already rooted?

Install the same v2 app APK. QHS does not supply a Root exploit or root the headset.

1. Make sure your existing Root solution provides working `su` access.
2. Open QHS and approve its Root request if your Root manager requires one.
3. Wait for the verified **ROOT** status. QHS selects the Root Home variants and reads installed environment packages.

Wireless ADB setup is unnecessary while Root is ready. If Root is not available, QHS can fall back to the NoRoot setup path. Root access, Home verification and download availability are separate checks; cached cards alone do not authorize changes.

Actual Root compatibility depends on the Root solution and firmware. [Current validation limits](COMPATIBILITY.md)

## Updating later

Use **Check Update** in the app. A verified update is handed to Android’s installer; approve its confirmation. QHS attempts to reopen after completion when the OS permits it.

An app update does not need to redownload your Home Library. If you already use an older Switcher or a v2 RC, check the migration guide before reinstalling or removing anything.

[What stays when updating or reinstalling? →](MIGRATING-TO-V2.md)
