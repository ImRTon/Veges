## 1. Project Foundation

- [x] 1.1 Bootstrap the Kotlin Gradle project with app, domain, data, design-system, release-tool, home, catalog, detail, and alerts modules using a version catalog and reproducible build settings
- [x] 1.2 Configure Android application identity, supported SDK range, Traditional Chinese resources, light/dark Material 3 themes, adaptive navigation, and release-safe manifest defaults
- [x] 1.3 Add Hilt, coroutines/Flow, Compose navigation, Room, Retrofit/OkHttp, Kotlin serialization, WorkManager, and test dependencies behind module-appropriate interfaces
- [x] 1.4 Configure CI checks for build, unit and instrumented tests, lint, formatting, static analysis, dependency verification, and release artifact validation
- [x] 1.5 Add deterministic clock, dispatcher, fake repository, and fixture utilities so date-sensitive and asynchronous behavior is testable without live services

## 2. Domain And Persistence

- [x] 2.1 Define domain types for stable produce concepts, aliases, categories, official variants, market bases, scaled prices, units, dates, provenance, freshness, and explicit unavailable reasons
- [x] 2.2 Design and implement the Room schema for taxonomy, source observations, source-day states, sync runs, model metadata, estimates, tracked concepts, alert rules, and notification events with required indexes and uniqueness constraints
- [x] 2.3 Implement DAO and transaction boundaries for idempotent staged imports, observable repository queries, estimate history, and atomic alert transitions
- [x] 2.4 Add Room schema export, migration tests, transaction rollback tests, query tests, and a policy prohibiting destructive migration fallback
- [x] 2.5 Implement repository interfaces and data/domain mappers that prevent network DTOs and persistence entities from leaking into feature modules

## 3. Official Data Synchronization

- [x] 3.1 Verify the current MOA wholesale and Taipei retail dataset contracts, attribution and usage terms, pagination, market identifiers, units, date formats, closure signals, and practical request limits; capture reviewed fixtures and document any blocking ambiguity
- [x] 3.2 Implement isolated HTTPS clients and DTO mappers for Taipei First, Taipei Second, crop metadata, and Taipei historical retail observations with bounded timeouts and retries
- [x] 3.3 Implement validation for schemas, dates, markets, commodity identifiers, numeric ranges, units, and referential integrity, including distinct valid, closed, missing, invalid, and failed states
- [x] 3.4 Implement incremental paginated synchronization with conditional requests where supported, atomic publication, prior-valid-data preservation, and source/run provenance
- [x] 3.5 Add contract and integration tests for success, pagination, duplicate observations, closure, empty data, zero values, malformed responses, schema drift, retry exhaustion, and interrupted atomic import
- [x] 3.6 Implement unique network-constrained WorkManager refresh, foreground stale-data catch-up, bulk recent-history bootstrap for the complete Taipei `N04` vegetable catalog, on-demand exact-mapping one-year completion, last-attempt/status reporting, and bounded history retention for the 1-year trend
- [x] 3.7 Verify synchronization, storage growth, network use, and battery behavior on representative emulator/device profiles and record acceptable operating bounds

## 4. Taxonomy And Catalog Audit

- [x] 4.1 Define versioned reviewed taxonomy and catalog-audit schemas with checksums, stable IDs, review metadata, aliases, explicit ambiguity sets, exact official mappings, category, publication state, and image metadata
- [x] 4.2 Implement deterministic release-tool commands that ingest reviewed taxonomy and source snapshots, validate referential integrity, calculate mapping/coverage/recency metrics, and emit machine-readable artifacts plus an exclusion report
- [x] 4.3 Generate an exhaustive candidate taxonomy covering every official `N04` vegetable variety observed for Taipei First or Taipei Second in the declared release-audit window, grouping exact official base-name variants only when reviewed semantics confirm they remain the same household produce, keeping distinct edible identities such as `SX0`-`SX4` sprouts separate, and retaining reviewed Taiwan household aliases without automatically accepting LLM-proposed mappings
- [x] 4.4 Present the exhaustive mapping and history evidence to the user, record the approved all-vegetable breadth decision, publish sparse official varieties with explicit insufficient-history state, and retain the 30-valid-day minimum only for history/estimate eligibility
- [x] 4.5 Finalize the exhaustive approved vegetable launch taxonomy plus reviewed fruit taxonomy, including explicit ambiguity choices, and update golden, checksum, deterministic-output, invalid-artifact, completeness, and stable-ID tests
- [x] 4.6 Generate and visibly apply one unique transparent-background claim-free illustration for every added launch produce concept without atlases, shared images, blank assets, or text-initial fallback; package only at-most-512-pixel transparent WebP assets within the 128-KiB per-file and 6-MiB complete-catalog budgets; obtain user review and record exact-hash approval metadata
- [x] 4.7 Implement fail-closed, idempotent bundled taxonomy import and Room-backed normalized household-name/alias search with fruit and vegetable category queries

## 5. Estimation Audit And Runtime

- [x] 5.1 Define candidate calibration models, feature/unit contracts, rolling or expanding-window backtest strategy, point-error metrics, interval-calibration metrics, confidence semantics, and artifact schema without embedding unreviewed acceptance thresholds
- [x] 5.2 Implement deterministic source normalization and same-date transaction-volume weighting for Taipei combined, with no silent single-market substitution
- [x] 5.3 Implement release-tool model fitting and no-future-leakage backtesting per concept and source basis, including per-period metrics, aggregate metrics, provenance, data cutoff, and exclusion reasons
- [x] 5.4 Run the initial data audit and family-specific backtests; record and enforce the approved gates of 18 paired periods, 90% paired coverage, 45-day generation recency, MAE at most NT$15 per `台斤`, RMSE at most NT$22 per `台斤`, 36-hour wholesale freshness, and 62-day artifact lifetime; keep interval, confidence, and unvalidated model families disabled
- [x] 5.5 Generate a checksummed versioned temporary-factor artifact for the explicitly approved `2.0` multiplier, with supported bases, unit conversion, approval metadata, and interval/confidence disabled; verify deterministic output and invalid-artifact rejection in CI
- [x] 5.6 Implement the on-device `WHOLESALE_MULTIPLIER_REFERENCE` evaluator, artifact integrity/version checks, exact-basis eligibility, 36-hour freshness, NTD/kg-to-`台斤` conversion, positive point estimation, and independent range/confidence gates
- [x] 5.7 Persist temporary-estimate provenance and auditable history, including concept, basis, source dates, estimator/factor-artifact version, factor approval date, formula, calculation time, optional interval/confidence, and unavailable reason
- [x] 5.8 Add domain tests for factor `2.0`, unit normalization, decimal rounding, variant aggregation, combined weighting, market-specific bases, invalid/closed/stale inputs, unsupported models, output gates, and historical provenance

## 6. Tracking And Alerts

- [x] 6.1 Implement track/untrack use cases with stable concept IDs, active-alert confirmation, persistence, and catalog-update behavior
- [x] 6.2 Implement validated create, edit, enable, disable, and delete flows for positive below-threshold alert rules whose source basis remains fixed until edited
- [x] 6.3 Implement transactional evaluation on only newer valid qualified estimates, transition deduplication, clear-state rearming, and retry-safe notification event identities
- [x] 6.4 Implement local notification channels, Android runtime permission handling, notification content and deep links, and best-effort scheduling disclosure
- [x] 6.5 Integrate synchronization, temporary-factor estimation persistence, alert evaluation, and notification delivery as a unique retry-safe WorkManager chain plus foreground catch-up
- [x] 6.6 Add tests for threshold boundaries, unchanged/closed/missing/invalid/stale data, source preference changes, duplicate worker execution, permission denial, transition rearming, and notification deep links

## 7. Compose Product Experience

- [x] 7.1 Build the adaptive Material 3 application shell, typed navigation, reusable loading/refreshing/current/stale/offline/unavailable/error components, price formatting, source labels, mandatory `估算` estimate tag/full disclosure, and AI-image disclosure component
- [x] 7.2 Implement the tracked-produce home with compact estimate hierarchy, movement text/icons, freshness and source dates, alert summaries, empty state, cached refresh behavior, and unavailable reasons
- [x] 7.3 Implement official-code and household-name search plus performant grouped/lazy browsing of the complete vegetable variety catalog and reviewed fruit catalog, with visibly rendered bundled illustrations, official identity context, no-reviewed-result state, sparse-history state, and mandatory ambiguity choice sheet
- [x] 7.4 Implement produce detail with estimate qualification, visibly rendered illustration and disclosure, source segmented control, source/calibration dates, track and alert actions, and expandable methodology/provenance
- [x] 7.5 Implement accessible 7D/30D/90D/1Y retail-estimate trends plus a produce-market interval K-line whose wick is official low/high and body is previous/current average, with volume, point inspection, non-visual summaries, factual produce-market definitions that do not label derived values as exchange open/close, and all user-visible prices consistently converted to NTD per `台斤`
- [x] 7.6 Implement alert list/editor screens, notification permission remediation, fixed-basis explanation, validation, and background-delay disclosure
- [x] 7.7 Add Compose navigation, state-restoration, disclosure, ambiguity, source-switching, unavailable-state, chart semantics, font-scaling, touch-target, contrast, light/dark theme, and screenshot tests

## 8. Privacy, Reliability, And Release

- [x] 8.1 Add data-source attribution, methodology, privacy, no-account/no-custom-backend disclosure, open-source notices, and release logging rules that exclude full payloads and local user preferences
- [x] 8.2 Add offline-first end-to-end tests using deterministic fake sources for initial import, failed refresh with cached data, catalog search, estimate display, tracking, and alert transitions
- [x] 8.3 Add non-blocking live-source smoke diagnostics and documented operator steps for detecting upstream schema, attribution, endpoint, or closure-signal changes without making CI depend on availability
- [x] 8.4 Benchmark startup, search, chart rendering, database size, synchronization duration, and memory on representative low/mid-range devices; fix regressions against recorded budgets
- [x] 8.5 Complete accessibility review, Traditional Chinese copy review including removal of redundant persistent slogans in catalog, home, and chart surfaces while retaining required progressive disclosure, generated-image review evidence, model/catalog audit evidence, Room migration verification, dependency/security review, and Play policy checklist
- [x] 8.6 Run the full release build and test matrix, perform closed-device testing across online/offline/closure/permission/update scenarios, and resolve all blocking findings
- [ ] 8.7 Document release, rollback, data/model refresh, taxonomy amendment, source incident, and database migration procedures, then produce the signed MVP candidate only after every gate passes
