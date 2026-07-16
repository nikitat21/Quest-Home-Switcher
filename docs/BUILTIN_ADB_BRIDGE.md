# Built-in local ADB bridge (phase 0)

## Status and non-goals

This document describes a possible **Shizuku-free, APK-only** privilege backend based on Android
Wireless debugging. Phase 0 only adds compile-safe contracts, state validation, identity validation,
and private key-storage boundaries. It is intentionally dormant:

- it is not connected to `PrivilegeCoordinator`, `ShellRunner`, `HomeSwitcherViewModel`, or the UI;
- it has no network implementation and the manifest does not request `INTERNET`;
- it does not pair, discover, connect, or execute a command;
- it does not change the existing root/Shizuku activation path;
- it exports no Android component and no generic shell API.

The feature must remain unavailable in release UI until the implementation and the Quest hardware
test gates below are complete. “APK-only” can remove the second Shizuku APK, but it cannot bypass
Android's user-controlled Developer mode, Wireless debugging, pairing, or firmware policy.

## Phase-0 boundaries

The new `shell.localadb` package contains:

| Boundary | Responsibility |
| --- | --- |
| `LocalAdbServiceDiscovery` | Discover only `_adb-tls-pairing._tcp` or `_adb-tls-connect._tcp`. |
| `LocalAdbPairingClient` | Perform one pairing attempt with a validated, redacted six-digit code. |
| `LocalAdbConnector` / `LocalAdbConnection` | Open authenticated TLS and expose only the fixed identity probe. |
| `LocalAdbShellIdentityValidator` | Parse `id` output strictly and accept only one unambiguous `uid=2000` shell identity. |
| `LocalAdbBridgeState` / `LocalAdbBridgeTransitions` | Make illegal jumps, especially `CONNECTING -> READY`, fail closed. |
| `LocalAdbTransactionBoundary` | Require connect, UID validation, one typed operation, and close as one serialized unit. |
| `LocalAdbKeyStorage` | Keep host identity material opaque and replaceable. |
| `NoBackupLocalAdbKeyStorage` | Atomically persist bounded key bytes below `noBackupFilesDir`. |

`VerifiedLocalAdbSession` exposes metadata only. It deliberately has no `exec`, `shell`, or string
command method. A later implementation must add narrowly typed home-management operations behind
the transaction boundary; it must not make the transport or a generic command channel public.

## Intended Quest pairing experience

The target experience, subject to real-device validation, is:

1. The user enables Meta-supported Developer mode and Wireless debugging. The app must not use a
   hidden API, exploit, accessibility automation, or silently alter a system setting.
2. For first-time setup, the user selects the system's “Pair device with pairing code” action. If a
   Quest firmware does not expose a supported pairing screen, the app reports the backend as
   unavailable; it must not pretend that APK-only setup works.
3. The app discovers the local `_adb-tls-pairing._tcp` service and asks for the six-digit code. The
   code is masked, never persisted, never included in analytics/crash reports, and cleared after the
   attempt.
4. After successful pairing, the app discovers `_adb-tls-connect._tcp`, connects with the stored
   host identity, runs the fixed `id` probe, and requires `uid=2000`.
5. Later home switches reuse the paired identity. Because adbd chooses dynamic ports, every new
   connection re-discovers the service; the user should not need another code unless Android revokes
   the pairing, Wireless debugging is reset, or firmware policy changes.

The normal home-selection UI should remain as fast as today after setup. Pairing/recovery belongs in
a separate setup flow, not in every switch. A disabled Wireless debugging service is a recoverable
user-action state, not an invitation to fall back to unsafe TCP port 5555.

## Security requirements

These requirements are release blockers, not suggestions:

### Network and protocol

- Use the TLS Wireless-debugging services only. Never enable or connect to legacy plaintext
  `_adb._tcp` / port 5555.
- DNS-SD is untrusted input. Resolve the address, then prove that it is loopback or assigned to one
  of the device's active interfaces. Do not accept an arbitrary host/port text field.
- Bind a connection service to the GUID/peer identity learned during pairing. A matching service
  name alone is not authentication.
- Apply explicit discovery, connect, handshake, read, write, and whole-transaction deadlines.
  Cancellation must close sockets and erase temporary secrets in `finally`.
- Reject oversized packets, invalid lengths, unknown protocol versions, duplicate terminal frames,
  and trailing data. Fuzz the packet decoders before enabling the feature.
- Do not log pairing codes, private keys, certificates containing user/device identifiers, raw
  protocol frames, `id` output, home paths, or command output.

### Identity and capability

- The first and only phase-0 service request is the fixed `id` identity probe.
- Accept exactly one syntactically valid UID token. `uid=0`, application UIDs, missing/malformed
  tokens, conflicting lines, and `uid=2000` with a different account name all fail closed.
- Construct `READY` only after `LocalAdbShellIdentityValidator` returns `Valid`.
- Revalidate on every newly opened transport before a privileged transaction. Do not cache “shell”
  status across reconnections.
- UID 2000 is Android's constrained `shell` identity, not root. Each typed home operation still
  needs a capability test on supported Quest firmware and must use the minimum command set.
- Serialize mutating transactions. A transaction is: discover/connect -> authenticate -> validate
  UID -> one typed operation -> close. Partial activation must surface as failure and use the
  existing recovery semantics; never run two activation transactions concurrently.
- Never expose `execute(String)`, an interactive shell, a localhost command server, an exported
  Binder service, or a broadcast receiver that accepts commands.

### Key material and Android component surface

- Generate a separate ADB host identity for this app. Never import the user's desktop `adbkey`.
- Prefer a non-exportable Android Keystore key if the final ADB certificate/signature flow can use
  it. Otherwise encrypt the serialized identity with a non-exportable Keystore wrapping key before
  writing it; app sandboxing alone is the phase-0 floor, not the final target.
- Persist only under `Context.noBackupFilesDir`. Phase 0 also sets `allowBackup=false`,
  `fullBackupContent=false`, and excludes all storage domains from cloud backup and device transfer.
- Writes must be atomic; key blobs must be size-bounded; temporary byte arrays must be zeroed; a
  reset/unpair action must delete the key and invalidate any Keystore alias.
- No bridge Activity, Service, Provider, or Receiver may be exported. Network permissions are added
  only with the actual implementation and reviewed together with its component surface.
- Release builds need dependency locking, an SBOM/third-party notice, and reproducible hashes for
  any bundled native library.

## Apache-2.0 source attribution and mapping

The protocol reference is the Android Open Source Project (AOSP) ADB module:

- upstream: [`platform/packages/modules/adb`](https://android.googlesource.com/platform/packages/modules/adb/)
- reference revision: immutable commit
  [`1cf2f017d312f73b3dc53bda85ef2610e35a80e9`](https://android.googlesource.com/platform/packages/modules/adb/+/1cf2f017d312f73b3dc53bda85ef2610e35a80e9/)
- license marker: [`MODULE_LICENSE_APACHE2`](https://android.googlesource.com/platform/packages/modules/adb/+/1cf2f017d312f73b3dc53bda85ef2610e35a80e9/MODULE_LICENSE_APACHE2)
- required notices: [`NOTICE`](https://android.googlesource.com/platform/packages/modules/adb/+/1cf2f017d312f73b3dc53bda85ef2610e35a80e9/NOTICE)
- architecture reference: [`docs/dev/adb_wifi.md`](https://android.googlesource.com/platform/packages/modules/adb/+/1cf2f017d312f73b3dc53bda85ef2610e35a80e9/docs/dev/adb_wifi.md)

The intended source-to-component mapping is concrete and deliberately narrow:

| Future component | AOSP reference files at the pinned revision |
| --- | --- |
| DNS-SD names, GUID behavior, reconnect rules | `docs/dev/adb_wifi.md`, `adb_mdns.cpp`, `client/adb_wifi.cpp` |
| Pairing packet/state semantics | `pairing_connection/pairing_connection.cpp`, `pairing_connection/include/adb/pairing/pairing_connection.h`, `proto/pairing.proto` |
| SPAKE2 and pairing cipher semantics | `pairing_auth/pairing_auth.cpp`, `pairing_auth/aes_128_gcm.cpp` |
| TLS channel behavior | `tls/tls_connection.cpp`, `tls/include/adb/tls/tls_connection.h` |
| ADB transport framing and `A_STLS` | `adb.h`, `adb.cpp`, `sockets.cpp`, `transport.cpp` |
| RSA host key and certificate format | `crypto/key.cpp`, `crypto/rsa_2048_key.cpp`, `crypto/x509_generator.cpp` |

No AOSP implementation code is copied in phase 0; the Kotlin contracts and validators are original
project code. Before any future copy, translation, or bundled native build lands, the change must:

1. pin the exact upstream commit (do not build from a moving branch);
2. add per-file “modified from AOSP” notices where required;
3. include the Apache License 2.0 and applicable AOSP `NOTICE` text in the distribution;
4. record every copied/transformed file and local destination in this table;
5. audit transitive crypto/native dependencies and their licenses separately.

An independently implemented Android `NsdManager` adapter that only follows the published service
names should be marked “protocol behavior referenced” rather than falsely claiming copied source.

## Remaining implementation plan and gates

1. **Quest feasibility probe:** on each supported Quest/Quest OS version, verify that the system
   exposes pairing with a code, that an app can discover its own adbd service, and that self-connect
   is permitted. Record pass/fail evidence. Stop if this needs an exploit or unsupported setting.
2. **Choose the protocol implementation:** either port the pinned AOSP pairing/TLS subset into a
   small audited NDK library or select a maintained Apache-2.0 library after a source/license/security
   review. System `libadb_*` libraries are not stable public NDK APIs and must not be assumed usable.
3. **Host identity:** implement key/certificate generation, Keystore protection, rotation, unpair,
   corruption recovery, and tests. Add an on-device instrumentation test for backup exclusion.
4. **Discovery:** implement lifecycle-safe `NsdManager` adapters for both TLS services, local-address
   validation, GUID matching, deduplication, timeouts, and network-change handling.
5. **Pairing:** implement the AOSP-compatible TLS + SPAKE2 pairing exchange. Keep the six-digit code
   in memory only and add wrong-code, timeout, cancellation, replay, and hostile-packet tests.
6. **Connection:** implement authenticated ADB-over-TLS framing and only the fixed `id` probe first.
   Add protocol corpus/fuzz tests and compare behavior with the pinned AOSP host client.
7. **Verified transactions:** implement a serialized boundary that owns and always closes the
   transport. Add narrowly typed operations needed by the existing activation use case; do not pass
   shell strings through public APIs and do not rewrite the existing activation logic during this
   gate.
8. **Quest matrix:** validate uid=2000 and every required `cmd package`/`pm`/filesystem capability on
   all supported firmware. Include disconnects and reboot/network-change recovery. A firmware that
   cannot safely perform the existing operation remains on root/Shizuku.
9. **Opt-in integration:** add a build-time feature flag defaulting to off, then coordinator wiring,
   setup UI, accessibility review, telemetry redaction, and explicit user reset. Do not advertise the
   backend as ready while any gate is incomplete.
10. **Release review:** threat model, dependency/NOTICE audit, SBOM, static analysis, instrumentation
    tests, external security review for native protocol code, and signed Quest smoke tests.

Root and Shizuku remain supported until the built-in backend independently passes every gate. The
goal is equivalent fast home switching after a one-time supported pairing flow—not silent privilege
escalation and not a general-purpose shell.
