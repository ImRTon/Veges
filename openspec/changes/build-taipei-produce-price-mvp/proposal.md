## Why

Taipei household shoppers lack a trustworthy, easy-to-read way to judge whether current fruit and vegetable prices are reasonable. This change delivers an Android MVP that turns official wholesale and historical retail data into clearly qualified Taipei retail reference estimates without operating a proprietary backend or misrepresenting wholesale observations as live retail prices.

## What Changes

- Add a Kotlin Android application for browsing, searching, and tracking Taipei vegetables and fruit using familiar Taiwan names and representative catalog illustrations.
- Add direct, resilient synchronization from official government data sources into an on-device database, including explicit freshness, closure, missing-data, and provenance states.
- Add a reviewed taxonomy that maps household concepts and aliases to exact official commodity variants without silently resolving ambiguous terms.
- Add an on-device Taipei retail reference estimation pipeline based on Taipei First and Taipei Second wholesale inputs and versioned, backtested calibration artifacts.
- Gate catalog eligibility, estimates, ranges, and confidence labels on documented data-quality and model-validation thresholds; the currently approved minimum is 30 valid observation days, while coverage and recency thresholds remain audit decisions.
- Add tracked produce, local threshold alerts, and on-device background refresh without a custom cloud service.
- Add Material 3 home, search/browse, detail, methodology, source-selection, tracking, and alert experiences with accessible trend presentation and explicit estimate disclosures; every model-derived price carries the visible `估算` tag and full `Taipei retail reference estimate` label.
- Exclude camera recognition, retailer comparisons, nationwide estimates, observed-live-retail claims, and fixed wholesale-to-retail multipliers from the MVP.

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
