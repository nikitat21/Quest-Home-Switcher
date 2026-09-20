# Compatibility & limits

[← Overview](../README.md) · [Installation](INSTALLATION.md) · [Report a problem](SUPPORT.md)

## What we can honestly say

| Area | Current position |
| --- | --- |
| Headsets | v2 targets Quest 2 / 3 / 3S on compatible Horizon OS firmware. Primary hands-on testing has been on Quest 3; Quest 2 and 3S have not been separately validated. |
| Quest Pro | Not part of the current v2 acceptance claim. Historical documentation is not proof of v2 compatibility. |
| Horizon OS | Home/package behavior changes with firmware. Use the current release notes and report your exact OS version with a problem. |
| NoRoot | Built-in local Wireless ADB; no Shizuku requirement for v2. Setup still depends on permissions and system UI support. |
| Root | Working `su` and a compatible Root solution are required. Root improvements have been tested with Lumi; this is not a guarantee for every exploit or firmware. |
| Home files | Must be compatible with the current Home system. Merely containing a scene or having an `.apk` extension is not a universal compatibility/safety guarantee. |

## Expected behavior, not automatically a bug

- A Home switch can reload Horizon Home/VR Shell. The panel may briefly disappear or return as the system settles; there is no promise of a seamless, flicker-free system reload.
- After a reboot, Root/ADB and the active Home must be verified again. Cached cards can appear before actions are available.
- Android can require Wi-Fi, debugging or installation confirmations. Automatic setup does not remove all OS prompts.
- Internet is needed for online catalog refresh, artwork not already cached, downloads and app-update checks. Local installed Homes have a separate inventory path.
- The entire online catalog is not promised to be persistently browseable after an offline cold start.
- NoRoot and Root use different APK variants. The app must verify the backend before offering the corresponding mutation path.
- No on-headset conversion of arbitrary old SideQuest Home APKs is included in v2.

QHS is unofficial and provided without warranty. It does not guarantee a Root exploit, compatibility with every firmware, successful recovery from every interruption or the safety of third-party APK code. Keep a known-good Home and appropriate backups.
