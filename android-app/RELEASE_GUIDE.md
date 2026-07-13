# Quest Home Switcher release guide

This guide creates a signed Quest Home Switcher APK for a GitHub Release or another trusted distribution channel. Keep the permanent signing key offline and backed up: Android accepts an in-place update only when it is signed with the same key as the installed app.

## Requirements

- Java 17.
- The Android SDK and the build tools used by the project.
- The permanent Quest Home Switcher JKS file.
- The matching key alias and passwords.

Never commit a JKS file, `keystore.properties`, passwords, or a signed APK to the source repository.

## 1. Update the version

Before every release:

1. Increase `versionCode` in `app/build.gradle.kts` by at least one.
2. Set `versionName` to the release version.
3. Update `CHANGELOG.md` and create matching release notes.
4. Update the output name and expected version checks in `Build-Release.ps1` when the release number changes.

## 2. Verify the source build

Run the same checks as the GitHub Actions workflow:

```powershell
.\gradlew.bat --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

All tasks must finish successfully before signing a release.

## 3. Create the signed APK

The repository contains `Build-Release.ps1`, which builds, aligns, signs, and verifies the APK. It expects the official key alias `questhomeswitcher` and prompts for the key password instead of storing it in source control.

```powershell
.\Build-Release.ps1 -KeyStore "$env:USERPROFILE\AndroidKeys\QuestHomeSwitcher.jks"
```

The script verifies all of the following before reporting success:

- the APK signature;
- the permanent certificate fingerprint;
- package ID `dev.codex.questhomeswitcher`;
- the expected version code and version name; and
- the final SHA-256 checksum.

The verified APK is written to the ignored `release` directory. Do not change the official signing certificate between releases.

### Android Studio alternative

Android Studio can also create the APK through `Build > Generate Signed App Bundle or APK > APK`. Select the permanent JKS, the matching alias, and the `release` build variant. Verify the resulting APK with Android SDK `apksigner` before distribution.

## 4. Test the exact release file

Install the final signed APK on a Quest that already has the previous permanent release installed. Confirm:

1. Android updates the app in place.
2. The app shows the intended version.
3. Root detection still works on a rooted test device, when available.
4. Shizuku status and the one-time permission flow work on an unrooted test device.
5. Refresh discovers compatible Home APKs.
6. A Home can be activated and the rollback path remains available when activation fails.

Also test the Windows setup with this exact APK embedded as its payload. Never use a debug-signed APK as a release payload.

## 5. Publish the GitHub Release

1. Create an annotated release tag such as `v1.3.0` from the verified source commit.
2. Use the matching title, for example `Quest Home Switcher v1.3.0`.
3. Copy the user-facing changes and known limitations into the release description.
4. Attach the signed APK, the Windows setup EXE, and `SHA256SUMS.txt`.
5. State that the project is unofficial and that no Meta Home APKs are bundled.
6. Download the uploaded files once and verify their checksums before announcing the release.

GitHub automatically provides source archives. Do not upload the signing key, passwords, local configuration, or third-party Home APKs.

## Signing-key safety

- Keep at least two encrypted backups of the JKS in separate locations.
- Do not send the JKS or its passwords through Discord, email, issues, or pull requests.
- Do not place secrets in GitHub Actions unless a future, reviewed release workflow explicitly requires them.
- If the key is lost, existing installations cannot receive a normal in-place update signed by a replacement key.
