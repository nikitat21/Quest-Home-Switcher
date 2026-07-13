# Troubleshooting

Start with the status shown by the Windows setup or Quest app. Avoid repeatedly reinstalling Shizuku or Home APKs before the actual state is known.

## The Windows setup cannot find the Quest

1. Use a USB cable that supports data, not only charging.
2. Put on the headset and approve **Allow USB debugging**.
3. Disconnect and reconnect USB after approving the prompt.
4. In the headset, make sure Meta Developer Mode is enabled for this device.
5. Close other ADB tools such as SideQuest temporarily, then run setup again.
6. If several Android devices are connected, disconnect the others during setup.

The storage-access prompt is not USB debugging authorization. Setup needs ADB authorization.

## Developer options or Wireless debugging does not open correctly

The setup tries to open Android Developer options and select Wireless debugging automatically. Horizon OS layouts can differ.

1. Select **WIRELESS DEBUGGING** in setup again.
2. If only Developer options opens, scroll to **Wireless debugging** and select it manually.
3. Enable the main Wireless debugging switch.
4. For first-time setup, select **Pair device with pairing code**.

Do not disable Developer options or USB debugging after setup; Android stops Shizuku when either is disabled.

## Shizuku keeps searching for Wireless debugging

1. Leave Shizuku open.
2. Open **Wireless debugging**.
3. Turn the main switch **off**.
4. Wait three seconds.
5. Turn it **on** again.
6. Return to Shizuku and allow a few seconds for discovery.

Closing Shizuku is normally unnecessary. If the headset was fully rebooted, Shizuku must be started again.

## Shizuku asks to pair again

Use the guided **SET UP / REPAIR** path rather than uninstalling Shizuku manually.

1. In Wireless debugging, select **Pair device with pairing code**.
2. In Shizuku 11.7, select **Pairing** and enter the six-digit code.
3. Do not press Start in 11.7.
4. Return to setup and select **PAIRING COMPLETE - CONTINUE**.

Setup preserves pairing data while updating Shizuku in place. Uninstalling Shizuku deletes its pairing data and should not be the first troubleshooting step.

## Quest Home Switcher says Shizuku is offline

1. Confirm that Shizuku itself says it is running.
2. Return to Quest Home Switcher and select **Refresh** once.
3. If the state remains offline, reopen the Switcher from setup with **UPDATE / OPEN SWITCHER**.
4. After a headset reboot, start Shizuku again.
5. If Shizuku is searching, use the Wireless debugging off/on sequence above.

The app distinguishes a missing Shizuku app, a stopped server, and missing permission. Follow the exact status instead of reinstalling everything.

## The Shizuku permission prompt does not appear

1. Make sure the Shizuku server is running first.
2. Open Quest Home Switcher and select its Shizuku status/action button.
3. If necessary, open Shizuku and review the authorized applications list for Quest Home Switcher.
4. Return to the Switcher and select **Refresh**.

The permission is an Android security prompt and cannot be approved automatically by setup.

## No Home APKs are found

The scanner does not accept every APK. A valid rootless Home must contain the exact file:

```text
assets/scene.zip
```

Check that:

- the APK is in `Download`, `Quest Homes`, `QuestHomes`, or `Homes` on the headset;
- the file finished copying and is still a readable APK/ZIP;
- the APK is a compatible NoRoot-Spoof Home, not a normal Android application;
- the app has Shizuku permission; and
- the scan has finished before refreshing again.

Use **IMPORT HOME APKS** in the Windows setup for the clearest validation result. It rejects incompatible files before uploading them.

## A Home shows a filename instead of its real name

Official names are resolved from the decompressed `assets/scene.zip` hash. A scene that is not in the current catalog falls back to a cleaned filename.

This does not mean that the Home is invalid. Rename it in the setup import review for a clearer display name, and include the scene hash in a catalog update request if you can legally share that metadata.

## Applying a Home fails

1. Expand **Technical details** in the Switcher before leaving the screen.
2. Record whether validation, installation, verification, preference update, or rollback failed.
3. Do not repeatedly apply the same file when automatic rollback also failed.
4. Confirm that Shizuku is still running and Wireless debugging is enabled.
5. Re-import the APK if the copy may be incomplete.
6. Test with a known-good compatible Home to separate an app problem from an APK problem.

The app attempts to restore the previous Home after a rootless installation or scene-verification failure. A failed rollback needs careful review before another switch.

## The old Switcher cannot be updated

Android cannot update an app in place when the installed copy uses the same package ID but a different signing key. Setup handles this only after Android returns `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

Read the confirmation carefully. A signing-key migration removes only the conflicting Switcher package before retrying the verified release. Separately, after a successful release installation, setup may offer to remove a legacy test app. Neither action removes Shizuku, Shizuku pairing, or Home APK files.

## What to include in a bug report

Include:

- Quest model;
- Horizon OS version;
- Quest Home Switcher version;
- Root or Shizuku mode;
- whether the issue happens after a headset reboot;
- exact steps to reproduce;
- the complete status message; and
- the Switcher's expanded technical details, with personal paths or device identifiers removed.

Do not attach signing keys, passwords, pairing codes, paid/copyrighted Home APKs, or proprietary Meta content. See [SECURITY.md](../SECURITY.md) for sensitive reports.
