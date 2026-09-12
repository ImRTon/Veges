## Why

Active typhoon-driven rain over agricultural areas is currently suppressed until wholesale prices or volumes have already moved, so the app misses the early supply-risk warning users need. Price refresh also performs avoidable consecutive and downstream work: repeated requests can append workers, catalog-history backfill can be requested again immediately, and even a successful sync with no changed source data recalculates estimates and alerts. On a constrained device this produces sustained garbage collection, skipped frames, and enough memory pressure for the process to be killed or appear to crash.

## What Changes

- Add a market-wide production-area weather risk that appears when an active official warning affects configured agricultural areas, even before price movement qualifies as a surge prediction.
- Preserve the distinction between an early weather risk and a price surge supported by observed market evidence; do not present the early warning as a guaranteed price increase.
- Attribute typhoon-driven heavy rain to the typhoon when the official warning text identifies that cause, and show concise affected production areas inside the existing typhoon price-surge radar.
- Keep weather-warning failures non-fatal and retain unexpired observations, while covering the current CWA CAP structures with tests.
- Coalesce repeated manual refresh requests, rate-limit catalog-history backfill, and keep all refresh work serialized.
- Skip estimate and alert recomputation when a published sync contains no changed source data, while preserving cached prices and pending-notification delivery.
- Contain incomplete Room market-history projections observed on-device so a refresh invalidation cannot crash the main coroutine.
- Add regression coverage for production-area risk classification, home-screen presentation, refresh scheduling, and change-aware downstream processing.
- Remove the user-visible animation test control and its synthetic reason gallery from the production home screen.
- Audit every price-rise reason so weather attribution respects the recorded cause, incomplete area data fails closed, and item-level forecasts are not adjusted without reviewed produce-to-origin mappings.
- Keep projected percentages explainable by excluding below-threshold price or volume inputs that are not shown as reasons.

## Capabilities

### New Capabilities

- `production-area-weather-risk`: Early, official-warning-based supply risk for agricultural areas, distinct from market-evidence price predictions.
- `price-refresh-resilience`: Safe, observable manual and background price refresh behavior across supported Android versions.

### Modified Capabilities


## Impact

- Domain weather-signal and home-state models.
- CWA warning parsing and market-shock repository behavior.
- Typhoon price-surge radar presentation and accessibility semantics.
- WorkManager queue policy, catalog-history scheduling, and refresh-pipeline tests.
- Existing predictor behavior remains conservative for item-level and market-evidence price forecasts.
- Price-rise reason qualification, attribution, and regression tests in both market-wide and item-level predictors.
- Market-history DAO projection nullability and repository validation.
