# A little help

[← Overview](../README.md) · [Installation](INSTALLATION.md) · [Send a report](SUPPORT.md)

Start with the status QHS shows. Reinstalling the app or repeatedly pairing again is usually not the first step. v2 does not need Shizuku or the old desktop setup tool.

## My computer cannot find or authorize the Quest

Run `adb devices` from Android Platform-Tools. In Windows PowerShell, use `.\adb.exe` if that folder is not on PATH.

- **No device:** use a USB data cable, check Meta Developer Mode and reconnect.
- **`unauthorized`:** put on the headset and approve **Allow USB debugging** for your computer. File access is a different permission.
- **`offline`:** reconnect USB and check the headset's debugging setting.
- **More than one entry:** select your Quest explicitly with `adb -s <serial> install -g Quest-Home-Switcher-v2.apk`. Replace `<serial>` with its entry from `adb devices`; do not type the angle brackets.

[Return to PC installation →](INSTALLATION.md#install-from-a-pc-recommended)

## Automatic or manual ADB setup does not finish

1. Keep the headset awake and connected to Wi-Fi. Finish any Android confirmation for the current network.
2. If automatic setup fails, use the manual instructions in QHS. **Android dev settings** tries to open Wireless debugging directly, with a Developer settings fallback.
3. Enable **Wireless debugging**. Select **Pair device with pairing code** and keep that dialog open.
4. Enter the current six-digit code in QHS. A code from a closed or expired dialog will not work.
5. Wait for a verified green **ADB** status. Do not start another pairing attempt while the current one is running.

If you installed directly on Quest, a manual first pairing is expected. After connecting, QHS tries to enable automatic setup for later use. If Android refuses that optional permission, manual pairing still works.

If Developer settings opens at the wrong place, select Wireless debugging manually. QHS cannot guarantee identical Android Settings behavior on every Horizon OS version. The [PC route](INSTALLATION.md#install-from-a-pc-recommended) is the fallback when the firmware restricts PC-free setup.

A green connection does not need to be paired again just because you opened its status panel. After a headset restart, let QHS recheck the connection before making changes.

## Root is not available

QHS requires an existing, working `su` solution. It does not install an exploit.

Check the Root manager's approval for QHS and whether Root is available after this headset reboot. The green **ROOT** status means QHS verified access; a cached Home card does not. If Root is unavailable, the app can offer NoRoot setup instead.

Mention your Root solution and Horizon OS version in a report. A successful test on one Root implementation does not establish compatibility with all of them.

## My Home is missing

For a manually added **NoRoot** Home, check:

- The complete APK is in **Downloads → Quest Home Switcher → Custom Homes**.
- It is a compatible, already-converted Home APK, not an old incompatible Home, a Root-only package, or an ordinary Android app.
- The file finished copying. Return to QHS so its inventory can refresh.
- The backend is connected. A display cache is not a substitute for a fresh file check.

The scanner deliberately does not search every folder on the headset. Root mode reads installed environment packages; copying a Root APK into Custom Homes does not install it.

[Folder and import details →](HOMES.md#add-your-own-noroot-home)

## A Community Home cannot be downloaded

**Before launch:** the public app and both Libraries are intentionally unavailable. This is not an ADB fault. [Check download status](DOWNLOAD.md).

A catalog search match is not proof that a compatible downloadable APK exists. Availability depends on the selected Root/NoRoot variant and its verified catalog entry. Check the connection, available headset storage and the exact message.

If an existing local Home works but its download does not, report the Library/Home name and download message. Do not reset a healthy ADB connection just to repair an internet download.

## Apply, Update or Remove is unavailable or fails

Let a download, verification or current change finish first. A Home must have a valid current selection and the required verified backend before it can be changed.

- **Apply:** wait for fresh inventory if access has changed. A very large Home can take longer to verify/install; do not start competing package changes.
- **Update Home:** new catalog metadata and installed-file checks must complete. Renaming a Home or changing its artwork alone is not an APK update.
- **Remove:** switch away from an active Custom Home first. Read the confirmation: Root removal uninstalls its environment package; NoRoot removal deletes the selected APK file.
- **Failure/rollback message:** record the exact text. Do not keep retrying if recovery is unconfirmed. Capture logs and ask for help.

The system may reload its Home shell during a switch. QHS tries to return to the panel where supported; a shell reload is not by itself proof of an app crash.

## An app update does not install or reopen

A v2 in-place update needs the same application identity and signing certificate, plus an appropriate newer version. A legacy app or Beta with a different identity is not that same installation.

Follow Android's installer confirmation. QHS attempts to reopen after the update, but Horizon OS may prevent this; open it from your launcher if necessary. A refused signature check is a reason to investigate, not to disable verification or uninstall blindly.

[Migration guide →](MIGRATING-TO-V2.md) · [Optional support logs →](RELEASE-SUPPORT-LOGS.md)
