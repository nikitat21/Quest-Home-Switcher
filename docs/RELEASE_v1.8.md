# Quest Home Switcher v1.8

## Recommended download

> **Windows users: download `Quest-Home-Switcher-Setup-v1.8.exe`. Do not download the raw APK unless you intentionally want to sideload it yourself.**

The setup EXE already contains the permanently signed Quest Home Switcher APK. It guides normal users through device detection, Shizuku setup, app installation or update, Home import, and the optional Official Meta Home Library.

| Platform | Download |
| --- | --- |
| Windows 10/11 | **`Quest-Home-Switcher-Setup-v1.8.exe`** |
| Linux x64 | `Quest-Home-Switcher-CLI-v1.8-linux-x64.tar.gz` |
| Apple-silicon Mac | `Quest-Home-Switcher-CLI-v1.8-macos-arm64.tar.gz` |
| Intel Mac | `Quest-Home-Switcher-CLI-v1.8-macos-x64.tar.gz` |
| Manual sideload/root users only | `Quest-Home-Switcher-v1.8.apk` |

Linux and macOS use their own self-contained command-line packages, not the Windows EXE. Their archives already include the verified v1.8 APK and an `install-switcher.sh` helper. Android Platform Tools (`adb`) must be installed separately. The first CLI release does not perform Shizuku installation or pairing; unrooted users run it after Shizuku is already working on the headset.

## Updating from v1.5, including the homes-v1.5.1 Library hotfix

- Existing Windows users can run their current setup and select **UPDATE / OPEN SWITCHER** after v1.8 is published. The updater verifies GitHub's published SHA-256 digest before opening the new setup.
- If the automatic setup update is unavailable, download `Quest-Home-Switcher-Setup-v1.8.exe` once from this release and run it directly.
- The Android app updates in place because v1.8 uses the same permanent signing certificate. Home files and an existing Shizuku pairing remain untouched.

## What changed

- Fixed duplicate Home cards when the same official Home exists as a personal import, an Official Library copy, or an installed Root package.
- Added canonical official-Home identity across known scene revisions, while unknown Custom Homes still merge only on exact scene identity.
- Added clear source labels for **Root direct**, **Official Library**, and **Imported** Homes.
- Strengthened Root detection: Root mode is accepted only after a real `uid=0` result and no longer mistakes Android shell UID 2000 for root.
- Root mode prefers a real installed environment package and safely falls back to the stock carrier scene only for active-state detection.
- Improved Shizuku/No-Root scanning and preserved active detection across corrected Library revisions.
- Setup now detects an identical APK anywhere under `Download/Quest Homes` by exact SHA-256, avoids duplicate uploads, and never replaces personal imports.
- Remote Home inventory is fail-closed when hashing is unavailable or malformed.
- The embedded offline Library keeps Futurescape and Mogu Hall disabled together with the other entries still awaiting safe Quest testing.

## Linux and macOS preview

Version 1.8 adds a tested, dependency-free cross-platform setup core and self-contained CLI packages. They can:

- verify that the authorized ADB device is a Meta Quest;
- report Quest, Switcher, and Shizuku state;
- install the pinned v1.8 APK without any automatic uninstall;
- open Quest Home Switcher; and
- validate and safely import compatible Home APKs with atomic, hash-verified, no-clobber uploads.

The Linux/macOS release is a CLI preview, not yet the full Windows graphical setup. It does not install, pair, update, or start Shizuku. macOS packages are not Apple-notarized. A native cross-platform GUI remains planned for v2.0.

## Shizuku and built-in Wireless ADB

- Root users still do not need Shizuku.
- Unrooted users still need Shizuku in v1.8.
- v1.8 contains only dormant security boundaries for a future built-in Wireless ADB backend. It has no network permission and is not connected to the UI or switching path.
- The Shizuku-free one-APK path will ship only after encrypted pairing, protocol, reboot, network-change, and real-Quest tests pass.

## SHA-256

| File | SHA-256 |
| --- | --- |
| `Quest-Home-Switcher-Setup-v1.8.exe` | `05AF7D107A2AC67AEE930D30795AB37F56533B217122C6F20392F8F0E1AB71FC` |
| `Quest-Home-Switcher-v1.8.apk` | `CFF3676D81209A2BC30C56A4587ECFC04789F6F0AF18733D84ADE04917362A50` |
| `Quest-Home-Switcher-CLI-v1.8-linux-x64.tar.gz` | `F56324770A6022C97CA6CE89F522A34781A843BAA621712775B260D8A171C5AA` |
| `Quest-Home-Switcher-CLI-v1.8-macos-arm64.tar.gz` | `975A0009C002670CA03FAA9980B0FBEE25210ECA0E1EB18B0926377D20EEA0BB` |
| `Quest-Home-Switcher-CLI-v1.8-macos-x64.tar.gz` | `8555088337289BE00DCF5E390D8ACC938CCBC91266D02F9AE60D7223566FB8C7` |

The same list is attached as `SHA256SUMS.txt`.

<!-- quest-home-switcher-version-code: 16 -->
