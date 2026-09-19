# Contributing

Small, well-tested improvements are welcome. Discuss larger changes first, especially anything affecting Root, ADB, package identities, signing or Library formats. Source visibility does not grant a general license; see [LICENSE.md](LICENSE.md).

## Work on v2 deliberately

Read [development notes](docs/DEVELOPMENT.md) first. This public repository contains the release downloads, Libraries and user documentation. Implementation work is reviewed separately; contact the maintainer before planning a code contribution.

Keep documentation and software publication separate. Larger implementation changes belong in their own reviewed and tested change.

## Keep the important guarantees

- Preserve working backend sessions; do not reconnect merely because someone opened a status panel.
- Treat cached cards as presentation, not authority to install, remove or activate packages.
- Test Root and NoRoot independently, including transitions, process death, reboot, lost connectivity and partial downloads.
- Keep user files and previous known-good packages recoverable. Refuse ambiguous paths, identities and stale selections.
- Verify the UI too: progress, completion, active state, disabled actions, empty results and errors must match the real operation.
- Keep keys, passwords, tokens, local device information, generated APKs and proprietary Home assets out of source commits.
- Library metadata edits must preserve stable IDs. Names and artwork can change without changing package identity or forcing every APK to be uploaded again.

## A useful change description

Explain the problem, the user-visible difference, any security/recovery impact and the checks actually performed. For device tests, give the Quest model, Horizon OS, installation route and Root solution where applicable.

Documentation uses simple English and the app's actual labels. Put the recommended path first, with optional technical details below. Avoid promises beyond tested behavior.

Before a documentation change is published, check relative links and anchors, install commands, light/dark readability and narrow screens. Do not hide required setup behind a collapsed technical section.

Generated reports, local caches and release binaries do not belong in a documentation commit. Keep legacy tools documented as legacy; do not recommend them as a v2 prerequisite.
