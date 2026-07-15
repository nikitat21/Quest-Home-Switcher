# Quest Home Switcher v1.5

Version 1.5 adds an optional online Official Meta Home Library while keeping the Windows setup a single, reasonably sized EXE. Home APKs are separate release assets and are downloaded only when selected.

## Main changes

- Searchable Home Library in the Windows setup.
- 20 known pre-v81 official Home entries: 16 available and 4 safely marked **Coming soon**.
- Exact GitHub asset name, byte-size, and SHA-256 verification.
- Verified local cache and atomic `.part` upload/rename on the Quest.
- Online catalog updates can enable a corrected Home without replacing the setup EXE.
- Library-managed files show Installed/Update status and use rollback-safe replacement; personal imports are never overwritten.
- App/setup releases and Home Library releases use separate channels.
- In-place upgrade from Switcher 1.1 (code 14) to 1.5 (code 15) verified on a connected Quest 3 without touching Shizuku.

## Files for the final `v1.5` application release

| File | SHA-256 |
| --- | --- |
| `Quest-Home-Switcher-Setup-v1.5.exe` | `944755DB12EC55E8BA3F7560E6D353DBD3BDD2710984860700166622EB218037` |
| `Quest-Home-Switcher-v1.5.apk` | `2E241D0C3F559E994631EB408D29A1F60206F3FD19A4BCE7967FC127F9E2B118` |

The final release body must contain exactly this marker:

```html
<!-- quest-home-switcher-version-code: 15 -->
```

## Separate `homes-v1.5.0` Library prerelease

Publish the Library as a public GitHub **prerelease**, not a normal final release. This keeps the old 1.1 updater pointed at the latest final application release.

- Catalog asset: `Official-Home-Library-Catalog.json`
- Catalog SHA-256: `7780962813A8F3AEAB55C195631A2C4DAB4F380B72CF79C514BFDDD0252D0019`
- Home assets: the 16 APKs whose exact names, sizes, and hashes are pinned in the catalog.
- Total selectable Home payload: 853,087,094 bytes. Users download only their selections.

The four disabled entries are Cascadia, Meta Horizon Terrace, Oceanarium, and Storybook. They can be enabled later through a newer `homes-vX.Y.Z` prerelease after their individual builds pass device testing.

## Planned after v1.5

A future opt-in Root Home catalog should use a separate channel and UI because Root mode selects installed environment packages rather than NoRoot-Spoof APK files. It is intentionally not included in v1.5 so the tested Shizuku/No-Root Library remains stable.

## Required publishing order

1. Create `homes-v1.5.0` as a public prerelease and attach the catalog plus all 16 matching Home APK assets.
2. Verify every asset has finished uploading and GitHub exposes its SHA-256 digest.
3. Publish `v1.5` as the final application release **after** the Library prerelease.

This order ensures setup 1.1 sees `v1.5` through GitHub's legacy `releases/latest` endpoint, while setup 1.5 independently discovers the newest `homes-v…` prerelease.

## Local validation completed

- Android unit tests, lint, assembly, signing, and APK signature verification passed.
- The one-file Windows setup built successfully and passed its isolated embedded-payload self-test.
- Catalog strict-schema and all 16 staged Home asset hash/size checks passed.
- Online Library release selection and digest-mismatch rejection passed.
- Old setup 1.1 self-test passed.
- Connected Quest update from app 1.1/code 14 to 1.5/code 15 succeeded and the activity started.
