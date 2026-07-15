# Installation

The recommended path is the guided Windows setup. It checks the actual state of the connected Quest and only performs the steps that are needed.

## Before you begin

You need:

- a Meta Quest 2, Quest 3, or Quest Pro;
- a Windows 10 or 11 PC;
- a USB data cable;
- Meta Developer Mode enabled for the headset;
- the latest setup EXE from the [GitHub Releases page](https://github.com/nikitat21/Quest-Home-Switcher/releases); and
- compatible Home APKs that you are legally allowed to use, unless you use an available entry from the optional Home Library.

Home APKs are not embedded in the setup EXE. The optional Home Library downloads only the entries selected by the user from a separately verified GitHub release.

## Recommended setup on an unrooted Quest

### 1. Connect and authorize the Quest

1. Connect the headset to the PC with a USB data cable.
2. Put on the headset.
3. Approve **Allow USB debugging**.
4. On your own PC, enable **Always allow from this computer** so the authorization survives reconnects.

The normal storage-access prompt is not the same as USB debugging. If setup cannot detect the Quest, check the headset for the USB debugging prompt.

### 2. Start the setup

1. Download `Quest-Home-Switcher-Setup-v1.5.exe` from the latest final application release.
2. Run the EXE and select **SET UP / REPAIR**.

The Windows EXE is not Authenticode-signed yet, so Windows may show an **Unknown publisher** or reputation warning. Continue only when the file came from this repository's Releases page. The project does not ask you to disable antivirus protection.

### 3. Let setup detect the current state

Setup distinguishes between these states:

- **Shizuku already running:** it is left completely untouched. Setup installs or updates Quest Home Switcher and opens it.
- **Shizuku installed but stopped:** setup first tries the starter already included in the installed Shizuku package. If Android still needs user interaction, Shizuku is opened with a clear instruction.
- **Shizuku missing:** setup starts the one-time Quest-compatible pairing path described below.

### 4. Complete first-time Shizuku pairing only when requested

The first-time flow temporarily installs Shizuku 11.7 because its pairing screen works on the supported Quest firmware. Pairing data is then preserved while setup updates Shizuku in place from the official RikkaApps release.

Inside the headset:

1. In **Wireless debugging**, select **Pair device with pairing code**.
2. In Shizuku 11.7, select **Pairing**.
3. Enter the six-digit code shown by Android and wait for the success message.
4. **Do not press Start in Shizuku 11.7.**
5. Return to the Windows setup and select **PAIRING COMPLETE - CONTINUE**.

Setup now updates Shizuku without deleting its pairing data and attempts to start it. Follow the on-screen instruction only if Android requests one final action. If Shizuku keeps searching for Wireless debugging, turn the main Wireless debugging switch off, wait three seconds, and turn it on again.

Android deliberately requires the pairing code to be entered by the user. The setup never bypasses this security confirmation.

### 5. Finish the Switcher installation

Setup verifies its embedded APK, installs or updates Quest Home Switcher, verifies the installed version, and opens the app.

When the setup screen says **SETUP COMPLETE**, select **SETUP COMPLETE - CLOSE**. If Android reports that an installed Switcher with the current package ID uses a different signing key, setup shows a detailed confirmation before removing only that conflicting Switcher installation and retrying the verified release. Declining leaves the installed app unchanged. Shizuku, its pairing, and Home files are never removed by this migration.

### 6. Approve the app permission

1. Open Quest Home Switcher.
2. If the status says that Shizuku permission is missing, approve the Shizuku permission prompt once.
3. Wait for the status to show **Shizuku connected**.

After a full headset reboot, Shizuku itself must be started again. The Switcher permission normally remains approved.

## Add Home APKs

### Choose from the Official Meta Home Library

1. Connect and authorize the Quest over USB.
2. Open setup and select **OFFICIAL HOME LIBRARY**. This action does not start, stop, pair, or update Shizuku.
3. Search or browse the catalog. It shows **Not installed**, **Installed - up to date**, **Update available**, or **Coming soon** for each Home.
4. Select one or more new or updated Homes and continue. Updates are never installed without this confirmation.
5. Setup verifies the dedicated Library prerelease, exact asset name, published SHA-256, and file size before downloading.
6. Each APK is cached locally, uploaded through a temporary `.part` file, verified on the Quest, and committed inside `Download/Quest Homes/Official Library` only after the transfer is complete. An update keeps a temporary backup until the replacement verifies successfully.
7. Open Quest Home Switcher and select **Refresh**.

The initial v1.5 catalog contains 16 tested Homes. Cascadia, Meta Horizon Terrace, Oceanarium, and Storybook remain visible but unavailable until their individual device tests are complete. Later catalog versions can make a corrected Home available without replacing the setup EXE.

Homes imported manually remain outside the managed Library folder and are never overwritten or deleted. A user may install the Library variant alongside an older personal copy and remove the personal copy later only if desired.

### Import from Windows

1. Reopen the setup EXE.
2. Select **IMPORT HOME APKS**. This action is independent of Shizuku setup.
3. The picker opens the Quest Home Editor `Cooked` folder when setup can detect it. Otherwise it uses the last location or your Downloads folder.
4. Select one or more compatible **NoRoot-Spoof Home APKs**.
5. Review the detected Home names. Edit the permanently highlighted **Name on Quest** field when you want a different display name.
6. Select **CONTINUE TO IMPORT**. Setup automatically adds `.apk` and removes unsafe filename characters on the same click. It stops only when two selected Homes would have the same name.
7. Review the result window. It shows clear totals and one status row for every selected file.
8. Select **DONE**. In the headset, open Quest Home Switcher and select **Refresh**.

Only APKs with a verified `assets/scene.zip` and the expected rootless environment target are accepted. Files are uploaded to:

```text
/sdcard/Download/Quest Homes
```

Existing files are not silently replaced. Identical files are skipped; different filename collisions receive a numeric suffix.

### Copy manually

Compatible Home APKs can also be copied to any of these folders:

```text
/sdcard/Download
/sdcard/Download/Quest Homes
/sdcard/Quest Homes
/sdcard/QuestHomes
/sdcard/Homes
```

Subfolders are scanned recursively.

## Apply a Home in Shizuku mode

1. Start Shizuku if the headset was rebooted.
2. Open Quest Home Switcher.
3. Wait for the Home library scan to finish, or select **Refresh**.
4. Use search when the library is large.
5. Select a Home and choose **Apply Home**.
6. Wait for validation, installation, scene verification, and the Horizon Home reload to finish.

Do not disconnect power or force-close the app while a Home is being replaced. If activation fails, open the technical details before trying again.

## Root mode

Rooted users do not need Shizuku:

1. Install `Quest-Home-Switcher-v1.5.apk` manually through ADB or a trusted sideloading tool.
2. Open the app and approve the Magisk/`su` request.
3. Let the app scan installed environment packages.
4. Select an environment and apply it.

The app updates the selected Oculus preference and reloads VR Shell. Root behavior depends on the firmware and root implementation; test carefully and keep a known-good Home available.

## Updating later

- Run the newest setup EXE and choose **UPDATE / OPEN SWITCHER** for an ADB-only Switcher update that does not inspect or modify Shizuku.
- Choose **SET UP / REPAIR** when Shizuku itself also needs diagnosis.
- A verified running Shizuku server is never restarted or updated by the normal setup flow.
- Always download the setup or manual APK from this repository's Releases page.
