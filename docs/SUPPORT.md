# Report a problem

[← Overview](../README.md) · [Quick troubleshooting](TROUBLESHOOTING.md)

A clear example helps more than reinstalling everything. If a change or recovery is unconfirmed, keep the current state and capture what happened before trying again.

## Send these details

- QHS version and Quest model/Horizon OS.
- **Root** or **NoRoot**. For Root, name the Root solution.
- Installation route: **PC with `-g`**, direct Quest install, or in-app update.
- What you selected, what you expected and the exact message.
- Whether it happens after a headset reboot, an app restart or every time.
- For one Home, its name, Library listing/ID if known, and Official/Community/manual origin.

A short video or screenshot can show a UI problem clearly. Share relevant diagnostics, not Home APKs or account credentials.

## Optional logs — no extra app UI

On a Windows PC with an authorized USB debugging connection:

1. Reproduce the problem. Do not clear logs or reinstall first.
2. Extract **QHS-Support-Logs.zip** from the v2 release. Keep its two helper files together.
3. Run **Get-QHS-Logs.cmd**. Android Platform-Tools must be available.
4. Send the **QHS-Logs-…txt** file from your PC's Downloads folder, with the action and approximate time.

The helper exports only when you run it and uploads nothing. The optional support bundle is included with the v2 app release. [Full instructions and multiple-device options →](RELEASE-SUPPORT-LOGS.md)

## Where to report

For help, you can ping `@Nikita` on the **FreeXR** or **QuestHomes** Discord server.

You can also [open a bug report](https://github.com/nikitat21/Quest-Home-Switcher/issues/new?template=bug_report.yml).

For security-sensitive issues, follow [Security](../SECURITY.md) instead of posting exploit details publicly.
