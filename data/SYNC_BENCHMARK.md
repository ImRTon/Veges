# Synchronization And Device Baseline

Reviewed: 2026-07-14

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
