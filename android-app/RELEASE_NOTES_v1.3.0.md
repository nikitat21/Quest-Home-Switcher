# Quest Home Switcher 1.3.0

This is a community test build for Meta Quest 2, Quest 3 and Quest Pro.

## What changed

- Clearer headset UI with large controller-friendly actions.
- Reliable Shizuku status and permission handling.
- More robust root execution with bounded commands and safe timeout cleanup.
- Faster Home scanning after the first scan.
- Only real Home APKs containing `assets/scene.zip` are shown.
- Duplicate scenes are grouped automatically.
- Every switch is validated and verified.
- If installation fails, the previous Home is restored automatically.
- A successful switch reloads Horizon Home without asking for a full headset reboot.

## Test scope

Please test Shizuku detection, root mode where available, search, refresh, active-Home detection and switching between known NoRoot-Spoof APKs. If an error appears, expand the diagnostic details and include them with the report.

This project is unofficial and is not affiliated with Meta, Shizuku or Quest Home Editor.

## Signing note for earlier testers

Versions 1.0 and 1.1 used the same permanent release certificate as this build and update normally. Some private 1.2.x test APKs used a temporary Android debug certificate. Android requires those test APKs to be removed once before installing 1.3. Only remove **Quest Home Switcher**; do not remove Shizuku. Shizuku will ask for the Switcher's permission once after that migration.
