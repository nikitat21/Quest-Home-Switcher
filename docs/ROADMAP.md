# Quest Home Switcher roadmap

This roadmap separates release commitments from research. Features move into a release only after
their matching Quest hardware tests pass.

## 1.8 — reliability and platform foundation

Version 1.8 is the next technical update. Its current scope is:

- merge duplicate official Homes into one canonical card while keeping every known scene revision
  available for active-Home detection;
- prefer a managed Official Library copy over an identical personal import without deleting or
  overwriting the personal file;
- keep true installed Root environments separate from the NoRoot carrier package and prefer the
  Root-direct path when it is genuinely available;
- make Root readiness require a real Android `uid=0` response;
- fail closed when the setup cannot hash every existing Home during duplicate detection;
- keep the embedded Library fallback aligned with the online safety disables;
- provide the first dependency-free Windows, macOS, and Linux Core/CLI implementation with strict
  Quest detection and atomic Home imports; and
- include dormant security contracts for a future built-in Wireless-ADB backend.

The built-in Wireless-ADB backend is **not a user-facing 1.8 feature**. Unrooted users still use
Shizuku in 1.8; rooted users continue to work without Shizuku. The dormant code has no network
permission and cannot pair, connect, or execute commands.

## Candidate 1.9 — built-in Wireless ADB

An APK-only, Shizuku-free mode can be considered for 1.9 only after all of these gates pass:

- supported one-time Android Wireless-debugging pairing inside the Quest app;
- encrypted ADB pairing and connection compatible with current Quest firmware;
- app-private, non-backed-up host keys and a clear reset action;
- strict proof that every connection is Android shell `uid=2000`;
- typed Home operations rather than a general-purpose shell interface;
- reconnect, reboot, network-change, cancellation, and rollback tests on real hardware; and
- a dependency, license, threat-model, and release-security review.

If those gates are not complete, Shizuku remains the stable unrooted backend.

## 2.0 — visible product redesign

Version 2.0 is reserved for a substantial user-facing update instead of another technical patch.
The exact feature list remains open, but the intended direction is:

- a redesigned Quest UI with clearer Home cards, source/status badges, previews, and actions;
- a simpler first-run and recovery experience;
- a redesigned desktop setup and Official Library with the same information architecture on
  Windows, macOS, and Linux;
- responsive layouts, larger readable instructions, controller/hand-friendly targets, and improved
  accessibility; and
- removal of old UI paths only after feature parity and migration tests.

No 2.0 visual design is final yet. It will be prototyped and tested separately so the stable 1.x
switching path does not regress.
