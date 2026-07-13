# Quest Home Switcher

Quest-native Android app for Meta Quest 2, 3 and Pro that safely switches between compatible Quest Home APKs.

> Unofficial community project. Not affiliated with or endorsed by Meta, Shizuku, or Quest Home Porter. Use it at your own risk.

## Features

- Shows root, Shizuku server and Shizuku permission as separate states.
- Automatically detects root through Magisk/`su`.
- In root mode, scans installed environment packages and switches them directly with `oculuspreferences`.
- Reloads VR Shell after a root switch; no headset reboot is normally required.
- Without root, scans `Download`, `Quest Homes`, `QuestHomes`, and `Homes` (including subfolders) for verified APKs containing `assets/scene.zip`.
- Caches scene metadata so an unchanged Home library does not need to be re-hashed on every refresh.
- Groups duplicate APKs by their actual scene content.
- Detects and marks the currently installed Home.
- Shows the APKs in a spatial, Quest-friendly landscape interface.
- Validates the selected APK again before changing the active Home.
- Creates a temporary rollback copy, verifies the installed scene and automatically restores the previous Home if installation fails.
- Reloads Horizon Home after a successful switch; a normal headset reboot is not required.
- Keeps an install log for diagnosing failed activations.
- Uses lightweight layered glass styling without runtime blur shaders.

## Requirements

- Meta Quest 2, Quest 3, or Quest Pro.
- Developer Mode enabled.
- Either Magisk/root, or Shizuku running through Wireless ADB.

## Install and use

The recommended Windows `Quest Home Switcher Setup` checks the connected headset first. An already-running Shizuku server is never downgraded or re-paired; the setup installs or updates the Switcher and opens it automatically. First-time users are guided through the Shizuku pairing path only when it is actually required.

Manual use:

1. Install the latest signed APK.
2. Rooted Quest: approve the Magisk prompt; installed environments are discovered automatically.
3. Unrooted Quest: start Shizuku and put compatible NoRoot-Spoof Home APKs in `Download` or `Quest Homes`.
4. Open Quest Home Switcher and approve its Shizuku permission once.
5. Select a Home and press `Apply Home`.

## Build from source

Android Studio is optional. Java 17 and the included Gradle wrapper are sufficient:

```powershell
.\gradlew.bat :app:assembleDebug
```

To create a signed release APK, follow the English [release guide](RELEASE_GUIDE.md). Never upload a keystore or its passwords.

## Privacy

Quest Home Switcher has no analytics, advertising, accounts, or network communication of its own. It reads local APK files and uses either local root commands or the locally installed Shizuku service.

## Important notes

The app does not include Home APKs. Users must provide compatible APKs themselves.

Horizon OS has no public Home-switching API. Quest Home Switcher therefore verifies every file and keeps a rollback point around the short Package Manager replacement step.

See [CHANGELOG.md](CHANGELOG.md) for version history. Release APKs should be attached to the matching GitHub Release instead of being committed to the source repository.
