# Support logs

[← Help](SUPPORT.md) · [Downloads](DOWNLOAD.md)

Normal use needs no diagnostic overlay or extra settings. QHS writes operation events to Android Logcat; it does not read system logs or need `READ_LOGS`.

## The simple Windows route

1. Reproduce the problem. **Do not clear logs or reinstall first.**
2. Connect the Quest by USB and authorize USB debugging.
3. Extract **[QHS-Support-Logs.zip](DOWNLOAD.md#optional-files)** from the current release. Keep its helper files together.
4. Double-click **Get-QHS-Logs.cmd**.
5. Send **QHS-Logs-…txt** from your PC’s Downloads folder, plus the action and approximate time.

Android Platform-Tools must be available: an installed Android SDK, PATH entry, or a `platform-tools` folder beside the scripts. No extra PowerShell installation is required.

The helper uploads nothing and changes no app permission. Review the file before sharing it.

<details>
<summary>Multiple devices or a custom output path</summary>

Open PowerShell in the extracted support folder:

```powershell
adb devices
.\Export-QhsSupportLog.ps1 -DeviceSerial '<serial-from-adb-devices>'
```

`-AdbPath` selects a particular ADB executable. `-OutputPath` selects a text file in an existing folder. The default package is the v2 app, `app.questhomeswitcher`.

</details>

The report includes the app version and app-filtered setup, Root, inventory, download/update and crash events. Capture soon after the failure: Android’s retained log buffer is finite. An empty log is not proof that no error occurred.

Include your Quest model, OS version, Root solution if applicable, and whether you installed from a PC with `-g`, directly on Quest, or through an in-app update. Maintainers retain the matching private R8 mapping and native symbols for each signed build.
