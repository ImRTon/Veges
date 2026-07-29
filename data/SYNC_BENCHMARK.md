# Synchronization And Device Baseline

Reviewed: 2026-07-17; final representative-device review: 2026-07-28

## Device

- Model: Xiaomi 24129PN74G
- Android: 16
- Connection: ADB over TLS
- Build variant: debug

## Baseline Measurements

- Cold launch measured with `adb shell am start -W`: 635 ms.
- Debug APK size: approximately 15 MB.
- Resident process total from `dumpsys meminfo` after launch: approximately 122 MB.
- Room database size before first successful import: 0 bytes/not created.
- WorkManager jobs: unique periodic and foreground catch-up jobs were observed through
  `dumpsys jobscheduler` after app startup.

## Operating Bounds And Gaps

- The reviewed MOA endpoint returned a roughly 2.3 MB daily JSON snapshot during contract review.
- The current adapter uses a 45 second call timeout and at most three attempts with 250 ms/1 s
  backoff. A successful end-to-end import duration and battery cost still require a connected
  network run against the official source.
- A one-year wholesale retention policy is implemented, but storage growth after repeated imports
  is not yet measured with a representative history.
- Battery and offline/online transition testing require a controlled device test session; no
  numeric release budget is inferred from this single baseline.

## 2026-07-17 Follow-up

- Device: Xiaomi 24129PN74G, Android 16, ADB over TLS, unplugged at 56% with a
  2,592,000 microamp-hour charge counter.
- Cold launch of the installed debug package completed in 613 ms.
- The app database files occupied 387,096 bytes before the foreground catch-up and 420,056
  bytes afterward, a 32,960 byte WAL increase from scheduling/recording the failed attempt.
- The foreground catch-up reached the official MOA source but failed closed before publication.
  The live payload contains a `null` where `MoaWholesaleRecordDto` currently requires a string;
  Kotlin serialization rejected record 344 before per-record validation. Prior valid data was
  preserved and no partial import was published.
- Per-UID cumulative network counters were 20,491,432 received bytes and 188,642 transmitted
  bytes before and after the sampled run. Connectivity diagnostics confirmed the app network
  request, but the platform counters did not expose a reliable per-run delta.
- Battery level and charge counter were unchanged after the sampled failed run; this is not a
  successful-import energy bound.
- A freshly built 16,053,880 byte APK could not replace the installed package because the
  signatures differ. The installed package was preserved; no app data was cleared.

At that point, task 3.7 remained blocked until the nullable upstream field was classified, the
adapter was updated without weakening required-field validation, and a successful controlled
import could be measured. The later sections record the completed run and accepted bounds.

## 2026-07-26 Successful Import And Provisional Bounds

- A freshly installed debug APK cold-launched in 594 ms on the same Xiaomi 24129PN74G.
- The official MOA payload contained four exact `rest` / `休市` closure sentinels. After the
  reviewed closure contract was implemented, each import completed with `VALID`, accepted 635
  observations, and published two `VALID` plus two `CLOSED` market/day states. No zero-price
  observations were created.
- Three unintentionally overlapping imports took 2,958 ms, 2,778 ms, and 2,243 ms. Room remained
  idempotent at 635 observations after all three runs.
- The main database, WAL, and SHM files occupied 1,122,304 bytes after the three imports. Total
  process PSS was 137,195 KiB in the debug build.
- The UID received 3,699,922 bytes and transmitted 45,370 bytes across the three imports, or about
  1.18 MiB received and 14.8 KiB transmitted per import. The charge counter declined by 6,000
  microamp-hours across the 45-second, screen-on observation window.
- WorkManager logs identified the overlap: the initial periodic work and foreground catch-up ran
  together, followed by another foreground entry. The scheduler now delays the first periodic run
  by one day, throttles automatic foreground catch-up to once per 15 minutes, and leaves explicit
  manual refresh immediate.

Provisional release bounds for a single fresh-snapshot import on this reference device are:

- cold launch at or below 1 second;
- synchronization at or below 10 seconds;
- received network traffic at or below 2 MiB and transmitted traffic at or below 50 KiB;
- database/WAL/SHM total at or below 2 MiB after the first successful import;
- charge-counter decline at or below 10,000 microamp-hours in a 45-second screen-on import window;
- debug-build total PSS at or below 180 MiB.

The successful content, closure, atomicity, and idempotency evidence is complete. The Xiaomi
installer later blocked a scheduler-deduplicated replacement run with
`INSTALL_FAILED_USER_RESTRICTED`, so the final low/mid profile matrix below uses isolated API 34
AVDs and retains the Xiaomi run as the physical-device battery reference.

## 2026-07-28 Final Low/Mid Profile Matrix

Both profiles used the official API 34 Google APIs x86_64 system image, airplane-mode and runtime
notification-permission checks, standard and 200% font-scale visual review, and the shipped
software rendering path. App instrumentation passed 8/8 and data instrumentation passed 14/14 on
both profiles. The data suite includes Room migrations, fail-closed import, alert transitions, and
taxonomy re-import preserving a tracked stable concept.

### Low profile

- AVD: Pixel 2 profile, 2 cores, 2,048 MiB RAM, 1080 × 1920, software GPU.
- Five cold launches: 1,830, 2,030, 1,724, 1,857, and 2,122 ms; median 1,857 ms.
- End-to-end official-code search over the 121-concept Room catalog: 2,670 ms for `LP2` to visible
  `九層塔`.
- Manual incremental refresh: 19.74 seconds.
- Per-run traffic delta: 3,283,943 bytes received and 31,524 bytes transmitted.
- One-year retained database set: approximately 9.4–9.6 MiB.
- Post-refresh memory: 111,752 KiB total PSS, 197,188 KiB total RSS, 260 KiB swap PSS.
- Chart scroll on the deliberately constrained software GPU: 79 frames, 29 deadline-janky
  (36.71%), p50 36 ms, p90 53 ms, p95 85 ms.

### Mid profile

- AVD: Pixel 5 profile, 4 cores, 4,096 MiB RAM, 1080 × 2400, software GPU.
- Four cold launches: 886, 969, 832, and 832 ms; one additional sample was reported warm.
- End-to-end official-code search: 1,175 ms for `LP2` to visible `九層塔`.
- One-year retained database set: 9,192 KiB.
- Loaded market memory: 122,086 KiB total PSS, 241,388 KiB total RSS, zero swap.
- Chart scroll: 91 frames, 4 deadline-janky (4.40%), p50 9 ms, p90 24 ms, p95 25 ms.

### Accepted operating bounds

- Cold launch: at most 2.5 seconds on the low profile and 1.25 seconds on the mid/physical profiles.
- End-to-end catalog search: at most 3 seconds on the low profile and 2 seconds on the mid profile.
- Manual refresh: at most 25 seconds on the low profile and 10 seconds on the physical reference
  device; at most 4 MiB received and 100 KiB transmitted.
- One-year Room database/WAL/SHM: at most 12 MiB.
- Debug-build total PSS: at most 180 MiB.
- Chart scroll: p90 at most 60 ms / 40% deadline-janky on the low software-GPU profile and p90 at
  most 30 ms / 10% deadline-janky on the mid profile.
- Battery: at most 10,000 microamp-hours decline in a 45-second screen-on import window on the
  physical reference device.

Android emulator battery accounting reported no meaningful drain and is not used as energy
evidence. The physical Xiaomi measurement remains the accepted battery bound. With the combined
physical and isolated-AVD evidence, task 3.7 is complete.
