## Context

This is a greenfield Android application for Taipei household shoppers. It must combine two daily wholesale markets, historical monthly retail observations, reviewed produce mappings, local tracking, and local notifications while clearly presenting model output as an estimate. There is no proprietary backend: the app reads government endpoints over HTTPS, stores data in Room, calculates estimates on device, and relies on WorkManager for best-effort background refresh.

The main engineering constraints are unstable external schemas, incomplete or closed-market data, Android background-execution limits, and the risk of presenting a statistically weak estimate as an observed retail price. The initial catalog and every user-visible estimate must therefore be controlled by reproducible audit artifacts rather than hard-coded product assumptions.

## Goals / Non-Goals

**Goals:**

- Deliver a production-quality Kotlin and Jetpack Compose MVP with maintainable boundaries between external data, domain rules, and presentation.
- Remain useful offline after initial synchronization and preserve the last valid data when refreshes fail.
- Make source provenance, dates, estimate qualification, and unavailable states explicit and testable.
- Select launch produce and estimation models through time-aware data audits and versioned release artifacts.
- Support on-device recurring refresh, tracking, and local threshold alerts without transmitting user preferences to a custom service.
- Meet Android accessibility, privacy, migration, observability, and release-quality expectations.

**Non-Goals:**

- Camera recognition, user-submitted prices, retailer comparisons, purchases, investment metaphors, or nationwide estimates.
- A custom API, remote database, cloud scheduler, remote push notification service, or server-side user account.
- Runtime LLM search or image recognition, automatic taxonomy decisions, or generated images presented as actual produce photos.
- Guaranteed execution at a precise daily time; Android controls background scheduling.
- Showing an estimate, interval, or confidence label before its configured validation gate passes.

## Decisions

### 1. Use an offline-first, layered Android architecture

The Gradle project will use `:app`, pure Kotlin `:domain`, Android `:data`, reusable `:designsystem`, and feature modules for home, catalog, detail, and alerts. Features depend on domain interfaces; `:data` implements repositories with Room, Retrofit/OkHttp, Kotlin serialization, and WorkManager. Hilt provides dependency injection, coroutines and `Flow` provide asynchronous state, and Compose navigation uses typed routes.

Room is the observable source of truth. Screens never render network DTOs directly: a sync coordinator fetches and validates pages, maps them to domain-safe persisted records in a transaction, and repository flows expose stable UI models. This costs some initial module setup but prevents government API details, estimation rules, and Compose state from becoming coupled. A single-module prototype was rejected because the MVP spans data ingestion, statistical policy, background work, and several independently testable screens.

### 2. Treat external feeds as untrusted adapters

Each source has a dedicated DTO and mapper behind a common synchronization contract. Contract fixtures capture representative success, pagination, closure, empty, malformed, and schema-drift responses. Network calls use HTTPS, bounded timeouts, conditional requests where supported, capped retries with backoff, and no logging of full payloads in release builds.

Synchronization is idempotent on source, market, commodity code, and observation date. New data is staged, validated, and committed atomically; a failed refresh leaves prior valid records intact. `SyncRun` and `SourceDayState` records distinguish valid, officially closed, missing, invalid, and failed data. Zero or nonsensical price/volume fields never become a zero-price observation, and absence is not labeled as a closure without an official signal. Foreground refresh complements unique periodic WorkManager work because background execution is not exact.

Raw source values retain their original units and provenance. Normalized scaled-decimal values support calculations without binary rounding in stored currency. On first successful initialization, the app fetches a bounded recent window for Taipei First and Taipei Second using the official `StartDate`, `EndDate`, `Market`, `$top`, and `$skip` contract, discovers every `N04` vegetable variety observed in those Taipei feeds, and publishes the reviewed deterministic catalog snapshot. It then uses incremental daily bulk refreshes. Opening or tracking a variety requests any missing portion of its one-year history by exact commodity/market mapping. This staged strategy makes the complete vegetable catalog immediately browsable without issuing hundreds of serial one-year requests or downloading unrelated national-market history. For the temporary MVP policy approved on 2026-07-26, the app converts valid NTD/kg wholesale averages to NTD/`台斤` and multiplies by exactly `2.0`. This is a rough market-reference estimate, not an observed retail price or a statistically calibrated retail model.

### 3. Ship reviewed, versioned catalog and calibration artifacts

A JVM release-tool entry point reads source snapshots and reviewed taxonomy files, performs mapping and data-quality audits, runs time-ordered backtests, and emits machine-readable catalog and model artifacts plus a human-readable report. Inputs, schema version, generation time, code revision, data cutoff, thresholds, per-concept metrics, and checksums are recorded. Generated outputs are deterministic for identical inputs and are validated during CI and app startup before import.

The taxonomy assigns stable concept IDs, daily Traditional Chinese names, category, aliases, image metadata, and exact official code mappings. Every official `N04` vegetable code observed for Taipei First or Taipei Second at the release-audit cutoff is preserved. Sharing the exact base text before the first hyphen is a necessary but not sufficient condition for grouping: reviewed origin, packaging, color, grade, and other non-identity modifiers may remain sub-varieties of one produce concept, while a suffix that identifies a different edible household produce remains a separate concept. The reviewed `SX0` through `SX4` sprout codes therefore represent other sprouts, mung bean sprouts, soybean sprouts, pea shoots, and alfalfa sprouts as five separate concepts. Official identities remain visible in methodology and are never dropped. Sparse concepts remain browseable and explicitly show insufficient history rather than being deleted. Ambiguous household aliases explicitly target multiple concepts and force user selection. LLM suggestions may be reviewed outside the app but never become authoritative automatically. Every produce concept has its own bundled, reviewed, recognizable, plausibly shaped transparent-background illustration with the required AI-image disclosure. Atlas tiles, shared images, logos, text initials, packaging, and origin or quality claims are prohibited. Release assets use transparent WebP with a maximum 512-pixel edge, maximum 128 KiB per file, and maximum 6 MiB for the complete bundled catalog illustration set. High-resolution generation and chroma-key sources are audit inputs only and SHALL NOT be packaged in the Android app.

Catalog publication and estimation eligibility are separate recorded outcomes. A concept is publishable only when its identity and mappings pass review. The temporary factor artifact records factor `2.0`, approval metadata, supported bases, unit conversion, version, and checksum. An estimate requires a published concept, exact reviewed mapping, valid positive wholesale input, required same-date markets for the combined basis, a known intact factor artifact, and wholesale freshness within 36 hours. The previous 18-period/90%/45-day/MAE/RMSE audit remains retained as evidence explaining why calibrated families are not shipped, but those calibration gates do not block the explicitly approved temporary factor. Interval and confidence outputs remain disabled.

### 4. Execute only known, approved estimator versions on device

The release tool retains the family-specific historical audit, but the current runtime artifact selects only the temporary `WHOLESALE_MULTIPLIER_REFERENCE` estimator with factor `2.0`. The artifact contains approval metadata, supported source bases, unit-conversion contract, eligibility, generation time, and checksum. Calibrated families remain ineligible until a future change supplies current retail targets and passes the historical gates.

The app implements a small registry of known model evaluators; it does not execute arbitrary code from an artifact. For `Taipei combined`, same-date valid Taipei First and Taipei Second observations are volume-weighted before model evaluation. The app does not silently substitute one market when a required combined input is unavailable. Taipei First and Taipei Second are separate advanced bases with independently qualified model entries.

Every calculated estimate stores the concept, basis, estimator version, wholesale source dates, calculation time, factor-artifact version, optional interval/confidence outputs, and the mandatory estimate disclosure tag. The short tag is `估算`; the accessible/full label remains `Taipei retail reference estimate`, while methodology also states `批發價換算後 × 2 的粗略參考`. Unsupported versions, failed integrity checks, stale or invalid inputs, missing mappings, and failed qualification produce an explicit unavailable reason.

### 5. Keep tracking and alert evaluation local and deterministic

Tracked concepts and below-threshold rules are Room records. An alert captures its source basis rather than following later global source-setting changes. After a successful sync, an evaluator processes only estimates backed by a newer valid source date. A notification fires when a rule transitions from not-met to met, remains deduplicated while met, and rearms after a later valid estimate clears it. Missing, invalid, zero-value, closed, stale, or unchanged data cannot trigger an alert.

WorkManager owns a unique sync-and-evaluate chain with network constraints; opening the app also catches up. Notification permission denial leaves tracking active and exposes corrective UI. This cannot promise exact delivery times, but it is the only architecture consistent with no backend and Android power policy.

### 6. Model UI state explicitly and disclose the estimate everywhere

Each feature has an immutable state model covering loading, current, stale, unavailable, offline, and error cases. The home screen is a compact watchlist; the market screen browses the complete reviewed vegetable variety catalog or reviewed fruit catalog before any query is entered, supports family grouping and lazy list rendering, visibly renders each approved illustration asset, and filters that list through official names, codes, reviewed household names, and aliases; ambiguous terms open a choice sheet. Catalog cards prioritize the household name, illustration, and concise official identity preview; variant counts, history-fetch implementation notes, and other non-actionable slogans are omitted from the primary surface, while complete official codes remain available in detail. Detail shows the illustration, estimate, source dates, expandable methodology, tracking, alerts, and interactive 7D/30D/90D/1Y trends, triggering bounded history completion when the requested range is not local. A sparse variety remains actionable and uses a concise state only when history is actually insufficient. Detail uses a produce-market interval K-line: each wick is the official daily low/high range, the body connects the previous trading-day average to the current trading-day average, color and signed text communicate direction, and volume remains a separate observed series. Raw wholesale prices remain NTD/kg in storage, but every user-visible price and chart value is deterministically converted to NTD per `台斤`. Chart definitions, conversion provenance, and non-exchange semantics remain available through point inspection, accessibility semantics, and an expandable data explanation instead of persistent instructional banners.

Material 3 components and semantic typography establish finance-like scanability without investment language or copied branding. Direction uses text/icons in addition to color, charts have textual summaries and accessibility semantics, touch targets and contrast meet platform guidance, and dynamic type is supported. Every catalog illustration is labeled `AI 生成示意圖，非實物照片。`; every estimated value is labeled as `估算` with the full `Taipei retail reference estimate` context rather than an observed market or store price.

### 7. Test boundaries and release gates, not only screens

Pure domain tests cover unit normalization, source combination, eligibility, estimator versions, date handling, and alert state transitions. Data tests cover API fixtures, pagination, idempotency, atomic failure, Room migrations, and WorkManager orchestration. The release tool has golden-output and no-look-ahead backtest tests. Compose tests cover navigation, ambiguity, unavailable states, disclosures, notification permission, dynamic type, and accessibility semantics. A small set of end-to-end tests runs against a deterministic fake data source; live-source smoke checks are non-blocking diagnostics because external availability must not make CI flaky.

## Risks / Trade-offs

- [Government APIs change schema, throttle clients, or become unavailable] -> Isolate adapters, validate contracts, cap requests, retain last valid data, expose freshness, and fail closed on unknown payloads.
- [No backend means refresh and alert delivery are delayed by Android] -> Use unique constrained WorkManager jobs, foreground catch-up, last-attempt status, and user-facing best-effort wording.
- [The temporary factor may diverge from actual retail prices] -> Label it prominently, expose the exact formula and source date, never show interval/confidence, and replace it through a later audited calibration change.
- [On-device history increases download, storage, and battery use] -> Fetch incrementally, paginate, index query paths, prune replaceable raw data while retaining at least the supported trend horizon, and benchmark representative devices.
- [Sparse retail history produces misleading confidence] -> Use time-aware validation and per-output gates; suppress unsupported concepts, intervals, and confidence rather than extrapolating.
- [Strict combined-source requirements create unavailable days] -> Show the latest valid estimate and source state, while avoiding undisclosed single-market substitution.
- [Reviewed taxonomy and generated images require ongoing editorial work] -> Version schemas, validate referential integrity in CI, store review metadata, and include a release checklist.
- [Additional Gradle modules add setup overhead] -> Keep interfaces capability-oriented and avoid one-class modules; accept the cost to protect high-risk domain logic from UI and transport changes.

## Migration Plan

1. Bootstrap the modular app, CI, static analysis, deterministic clocks/dispatchers, and test fakes.
2. Implement source adapters and Room schema against captured fixtures, then verify incremental sync and failure recovery on devices.
3. Build and run the taxonomy/data audit and backtest tool; review and approve the launch catalog and qualified model artifacts before enabling estimates.
4. Import signed-off artifacts, implement domain estimation and alert logic, then add Compose features and accessibility coverage.
5. Run closed testing with data freshness, storage, battery, crash, and model-output monitoring; publish only when all release gates pass.

Rollback uses the prior Play release while preserving forward-compatible Room data. Database upgrades require tested migrations and never use destructive fallback. Model or catalog regressions are corrected by a new app artifact because no remote configuration service exists; the UI fails closed if bundled artifact integrity or version checks fail.

## Open Questions

The temporary factor is explicitly approved as `2.0`. A future change must replace it with current retail calibration evidence and may separately propose validated interval or confidence outputs.
