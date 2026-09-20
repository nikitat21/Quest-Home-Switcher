# Moving to v2

[← Installation](INSTALLATION.md) · [Home folders](HOMES.md) · [Help](TROUBLESHOOTING.md)

## Which app are you using now?

| Current app | What changes |
| --- | --- |
| **Older stable app (v1.8)** | v2 installs as a separate app. Complete v2’s own setup; the old Shizuku setup does not become QHS’s local ADB pairing. |
| **Old v2 Beta** | The Beta and release/RC have different app identities. Install the release/RC separately and set it up once. |
| **v2 release candidate** | The final v2 keeps `app.questhomeswitcher` and its official certificate. A higher, correctly signed version is an ordinary update. No uninstall should be needed. |

Get the signed app from the [v2 download page](DOWNLOAD.md). Do not replace an installed RC with an unsigned build, a different signing key or an APK from the GitHub source tree.

## Keep your Homes

The shared **Downloads → Quest Home Switcher** folders are not app-private data. QHS reuses them after backend setup. An ordinary in-place update does not reset them.

Older files may be in **Downloads → Quest Homes** or other legacy scan locations. v2’s NoRoot scan is deliberately limited to its own Official/Custom folders. Keep a backup and copy compatible personal NoRoot APKs into:

```text
Download/Quest Home Switcher/Custom Homes/
```

Use the in-app Library for its managed Home variants. A filename, or the fact that a Home once worked in another tool, is not proof of compatibility with the current firmware.

## What an uninstall removes

Uninstalling QHS removes its app-private settings, pairing identity and custom presentation data. Existing shared Home folders are intended to remain; do not treat that as a substitute for your own backup. Reinstalling requires setup again.

In Root mode, environment packages installed separately are also separate from the Switcher app. Uninstalling QHS is not the same as removing those packages.

## A safe order

1. Keep your working app and a known-good Home while trying v2.
2. Install the intended signed v2 APK. For a PC first install, include **`-g`**.
3. Complete Root or ADB setup and wait for inventory verification.
4. Confirm your Homes, then test one switch.
5. Remove an old app only when you have confirmed you no longer need its settings or workflow.

Do not delete existing folders or bulk-uninstall environment packages as a migration shortcut.
