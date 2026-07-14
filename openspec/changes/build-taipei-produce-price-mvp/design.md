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

Raw source values retain their original units and provenance. Normalized scaled-decimal values support calculations without binary rounding in stored currency. Physical unit normalization may convert kilograms to `台斤`, but retail estimation is always performed by a validated calibration model; it is never a fixed markup or unit-conversion result.

### 3. Ship reviewed, versioned catalog and calibration artifacts

A JVM release-tool entry point reads source snapshots and reviewed taxonomy files, performs mapping and data-quality audits, runs time-ordered backtests, and emits machine-readable catalog and model artifacts plus a human-readable report. Inputs, schema version, generation time, code revision, data cutoff, thresholds, per-concept metrics, and checksums are recorded. Generated outputs are deterministic for identical inputs and are validated during CI and app startup before import.

The taxonomy assigns stable concept IDs, daily Traditional Chinese names, category, aliases, image metadata, and one or more exact official code mappings. Ambiguous aliases explicitly target multiple concepts and force user selection. LLM suggestions may be reviewed outside the app but never become authoritative automatically. Illustration assets are bundled, reviewed for recognizable and plausible content, and always carry the required AI-image disclosure.

Catalog publication and estimation eligibility are separate recorded outcomes. A concept is publishable only when its identity and mappings pass review. A concept/source pair is estimate-eligible only when configured coverage, recency, sample-size, and backtest gates pass. The approved minimum calibration gate is at least 30 paired periods where both the wholesale input and historical retail target are valid; the current audit has only 18 monthly retail periods, so no retail estimate is eligible yet. Coverage, recency, point-error, interval, confidence, and staleness thresholds remain independently configurable release gates pending audit approval. Valid wholesale observations may be displayed immediately as wholesale data, but they do not become retail estimates merely because a price exists. This allows the release process to explain exclusions instead of silently inventing values.

### 4. Execute only known, validated model versions on device

The release tool compares predeclared candidate estimators using rolling or expanding-window evaluation so future observations cannot leak into training. Metrics and thresholds are set in reviewed configuration before the final release audit. The selected artifact contains coefficients/parameters, applicability dates, supported source bases, eligibility, and separate validation results for point estimates, uncertainty intervals, and confidence labels.

The app implements a small registry of known model evaluators; it does not execute arbitrary code from an artifact. For `Taipei combined`, same-date valid Taipei First and Taipei Second observations are volume-weighted before model evaluation. The app does not silently substitute one market when a required combined input is unavailable. Taipei First and Taipei Second are separate advanced bases with independently qualified model entries.

Every calculated estimate stores the concept, basis, model version, wholesale source dates, calibration cutoff, central value, optional interval/confidence outputs, and the mandatory estimate disclosure tag. The short user-facing tag is `估算`; the accessible/full label is `Taipei retail reference estimate`. The tag must remain attached in home, detail, alerts, and notifications. Unsupported model versions, failed integrity checks, stale or invalid inputs, missing mappings, and failed qualification produce an explicit unavailable reason. This conservative behavior favors trust over catalog size.

### 5. Keep tracking and alert evaluation local and deterministic

Tracked concepts and below-threshold rules are Room records. An alert captures its source basis rather than following later global source-setting changes. After a successful sync, an evaluator processes only estimates backed by a newer valid source date. A notification fires when a rule transitions from not-met to met, remains deduplicated while met, and rearms after a later valid estimate clears it. Missing, invalid, zero-value, closed, stale, or unchanged data cannot trigger an alert.

WorkManager owns a unique sync-and-evaluate chain with network constraints; opening the app also catches up. Notification permission denial leaves tracking active and exposes corrective UI. This cannot promise exact delivery times, but it is the only architecture consistent with no backend and Android power policy.

### 6. Model UI state explicitly and disclose the estimate everywhere

Each feature has an immutable state model covering loading, current, stale, unavailable, offline, and error cases. The home screen is a compact watchlist; catalog search uses reviewed names and aliases; ambiguous terms open a choice sheet; detail shows the estimate, source dates, methodology, tracking, alerts, and interactive 7D/30D/90D/1Y trends. Charts use average lines, observed source ranges, and volume, not OHLC candlesticks.

Material 3 components and semantic typography establish finance-like scanability without investment language or copied branding. Direction uses text/icons in addition to color, charts have textual summaries and accessibility semantics, touch targets and contrast meet platform guidance, and dynamic type is supported. Every catalog illustration is labeled `AI 生成示意圖，非實物照片。`; every estimated value is labeled as `估算` with the full `Taipei retail reference estimate` context rather than an observed market or store price.

### 7. Test boundaries and release gates, not only screens

Pure domain tests cover unit normalization, source combination, eligibility, estimator versions, date handling, and alert state transitions. Data tests cover API fixtures, pagination, idempotency, atomic failure, Room migrations, and WorkManager orchestration. The release tool has golden-output and no-look-ahead backtest tests. Compose tests cover navigation, ambiguity, unavailable states, disclosures, notification permission, dynamic type, and accessibility semantics. A small set of end-to-end tests runs against a deterministic fake data source; live-source smoke checks are non-blocking diagnostics because external availability must not make CI flaky.

## Risks / Trade-offs

- [Government APIs change schema, throttle clients, or become unavailable] -> Isolate adapters, validate contracts, cap requests, retain last valid data, expose freshness, and fail closed on unknown payloads.
- [No backend means refresh and alert delivery are delayed by Android] -> Use unique constrained WorkManager jobs, foreground catch-up, last-attempt status, and user-facing best-effort wording.
- [Bundled calibration becomes stale between app releases] -> Show calibration cutoff separately, make staleness an eligibility rule, and require a new audited artifact for release when thresholds expire.
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

No product or architecture decision currently blocks implementation. Numeric catalog coverage, model accuracy, interval calibration, confidence, and staleness thresholds must be proposed from the initial audit, reviewed, and committed before the release artifact is generated; they are release evidence, not assumptions embedded in this design.
