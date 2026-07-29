## Why

Taipei household shoppers lack a trustworthy, easy-to-read way to judge whether current fruit and vegetable prices are reasonable. This change delivers an Android MVP that turns official wholesale and historical retail data into clearly qualified Taipei retail reference estimates without operating a proprietary backend or misrepresenting wholesale observations as live retail prices.

## What Changes

- Add a Kotlin Android application for browsing, searching, and tracking every vegetable type represented by the official variety codes observed in the Taipei First or Taipei Second `N04` vegetable feed at the release-audit cutoff, plus the reviewed fruit selection. Reviewed semantic grouping keeps origin, packaging, color, and other non-identity modifiers under one produce concept while preserving all exact official identities, but suffixes that name distinct edible produce such as green-bean, soybean, pea, and alfalfa sprouts remain separate concepts. Every produce concept has its own visibly rendered transparent-background illustration. The installed app bundles only display-sized optimized WebP assets; high-resolution generation sources remain outside the APK.
- Add direct, resilient synchronization from official government data sources into an on-device database, including bounded recent-history bootstrap for the complete vegetable catalog, on-demand one-year history completion for viewed or tracked varieties, and explicit freshness, closure, missing-data, and provenance states.
- Add a reviewed taxonomy that preserves every official vegetable variety mapping, permits grouping only when variants share the exact official base name and review confirms they remain the same household produce, maps reviewed household aliases to those concepts, and never silently resolves ambiguous names.
- Add an on-device Taipei market-reference pipeline that temporarily converts valid Taipei wholesale prices to NTD per `台斤` and multiplies by the explicitly approved factor `2.0`.
- Gate the temporary estimate on reviewed mappings, valid source data, exact source-basis availability, a checksummed factor artifact, and 36-hour wholesale freshness. The UI identifies the value as a rough multiplier-based `估算`, never an observed store or retail transaction. Historical calibration models, intervals, and confidence remain disabled until a later audited change replaces the temporary factor.
- Add tracked produce, local threshold alerts, and on-device background refresh without a custom cloud service.
- Add Material 3 market browse, watchlist, detail, methodology, source-selection, tracking, and alert experiences with an accessible produce-market K-line presentation and explicit estimate disclosures; all user-facing prices use NTD per `台斤`, wholesale observations are visibly identified as converted from the official NTD/kg source unit, and every model-derived price carries the visible `估算` tag and full `Taipei retail reference estimate` label.
- Exclude camera recognition, retailer comparisons, nationwide estimates, observed-live-retail claims, and any multiplier other than the explicitly approved temporary `2.0` factor from the MVP.

## Capabilities

### New Capabilities

- `official-produce-data-sync`: Fetch, validate, persist, and refresh official wholesale and retail-calibration data on device with provenance and freshness states.
- `produce-catalog-search`: Maintain a reviewed fruit and vegetable taxonomy and support visual browsing, aliases, exact official mappings, and explicit ambiguity resolution.
- `retail-reference-estimation`: Audit launch eligibility and calculate qualified Taipei retail reference estimates from selected wholesale sources and versioned calibration models.
- `produce-tracking-alerts`: Persist tracked produce and threshold rules, evaluate only valid new estimates, and deliver local notifications.
- `produce-price-experience`: Present tracked items, estimates, trends, source controls, disclosures, and data states through a polished, accessible Material 3 Android UI.

### Modified Capabilities

None.

## Impact

- Introduces a new Kotlin/Jetpack Compose Android codebase, modular domain/data/UI boundaries, Room persistence, WorkManager synchronization, and local notifications.
- Connects directly to MOA and Taipei open-data endpoints; no proprietary server, cloud database, or remote scheduler is introduced.
- Adds version-controlled taxonomy, calibration inputs, model metadata, data-audit outputs, and generated catalog illustration assets to the release process.
- Requires automated unit, integration, database migration, API contract, model backtest, accessibility, and UI tests, plus documented privacy, data attribution, and release checks.
