# Homes, files & updates

[← Overview](../README.md) · [Setup](INSTALLATION.md) · [Help](TROUBLESHOOTING.md)

## Choose a Library Home

1. Wait for verified **ADB** or **ROOT** access.
2. Open **Official**, or **Custom → Browse all** for the Community catalog.
3. Select a Home. Download it if it is not installed.
4. Wait for verification, then choose **Apply Home**. **Applying** is progress; **Active** is the confirmed state.

Both Libraries are being prepared for the v2 launch and are not publicly downloadable yet. [Download status →](DOWNLOAD.md)

## Add your own NoRoot Home

Copy a complete, compatible **NoRoot-Spoof** Home APK into this headset folder:

```text
Download/Quest Home Switcher/Custom Homes/
```

Return to QHS with a working ADB connection and allow its inventory refresh to finish. The Home appears in **Custom** when it passes discovery/validation. A partially copied APK, arbitrary Android app or legacy unconverted Home is not a supported import.

Keep files directly in this folder for the simplest workflow. v2 does not scan the entire Downloads tree. It does not cook/convert old SideQuest APKs on the headset; use a compatible converted Home or the Library.

Use **Edit details** on a Custom Home to change its displayed name or picture. This changes presentation, not its underlying package or Library identity.

## Where things live

| Folder under `Download/Quest Home Switcher/` | Used for |
| --- | --- |
| `Official Homes` | NoRoot Official Library downloads. |
| `Custom Homes` | NoRoot Community downloads and compatible personal APKs. |
| `Official Root Homes` | Staged Root downloads, including Community Root variants. Only provisioned for Root mode. |

Folders are created/reused after the first successful privileged connection, **not simply when Android installs the Switcher APK**. Existing files are not wiped on updates. A later scan can repair a missing folder; there is no reason to repeatedly delete/recreate them yourself.

Root mode discovers **installed environment packages**. Merely dropping a Root APK into Custom Homes does not install it. Library Root downloads use the guarded Root installation path; independently installed compatible Root environments are discovered by their package/scene metadata.

## Update a Home

When verified Library metadata differs from an installed package, its card can offer **Update Home**. Refreshing metadata alone does not download every Home again. A changed name or picture alone is not an APK update.

The app checks file size and SHA-256, including selected-file checks when two versions have the same size. Discovery depends on catalog refresh and GitHub propagation; it is not a real-time push notification guarantee.

- **NoRoot:** the stored APK is updated. If that Home is currently running, apply the updated Home to activate its new content.
- **Root:** the installed package is updated through the Root transaction. Updating the active package can reload its scene.

Leave the app to finish. A failed check or interrupted transfer must not be treated as a completed update.

## Remove a Custom Home

Switch to a different Home first, then select the unused Custom Home and choose **Remove**. Read the confirmation: in NoRoot mode it removes the stored Home file; in Root mode it removes the eligible installed environment package. Official/system package protections remain in place.

Do not use Remove as an app reset or a bulk cleanup tool. Keep your own backup if you may want to restore a manually imported Home later.
