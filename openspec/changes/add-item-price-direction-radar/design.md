## Context

The domain currently computes explainable per-item surge predictions from wholesale price, volume, and active CWA weather signals, then aggregates those predictions for the home screen. The detail repository already loads up to 31 days of source observations, but exposes only the currently selected chart period and the detail UI has no future-looking item signal.

The change crosses the domain, Room data, detail ViewModel, and Compose UI.

## Goals / Non-Goals

**Goals:**

- Produce one explainable 7–14 day direction evaluation for the selected produce concept and market basis.
- Represent rising, falling, neutral, stale, and insufficient-history states explicitly.
- Keep the home market-wide surge radar compatible.
- Present evidence in an accessible, glanceable Material 3 card.
- Use only already-available wholesale observations and official weather warnings.

**Non-Goals:**

- Predict an exact future retail price or guarantee a future outcome.
- Introduce opaque machine learning, RSI/MACD-style financial-market terminology, or a new network service.
- Generate a market-wide price-decline alert in this change.
- Persist predictions in Room; evaluations remain derived from current observations.

## Decisions

### Add a symmetric item predictor beside the market surge predictor

`ItemPriceDirectionPredictor` will accept one concept, basis-specific wholesale history, and active weather signals. It returns an evaluation with an explicit availability state and an optional directional outlook. Keeping the existing `PriceSurgePredictor` intact minimizes regression risk for the home experience while sharing compatible concepts such as the 7–14 day horizon and explainable evidence.

Alternative considered: replace `PriceSurgePredictor` immediately with a generic predictor. This would reduce duplication but couples the new detail feature to a broad rewrite of the uncommitted home radar and makes validation harder.

### Use signed market features with asymmetric weather evidence

The predictor compares the latest three valid trading days with the preceding seven, plus a longer median baseline:

- Rising evidence: positive price momentum, contracting volume, price above longer baseline, and active adverse weather.
- Falling evidence: negative price momentum, expanding volume, and price below longer baseline.

Weather can strengthen a rising signal but cannot create a falling signal. A direction requires price evidence plus a minimum score and projected magnitude. Conflicting or weak evidence produces a neutral evaluation.

Alternative considered: mirror weather warnings into both directions. This is not economically defensible because the supported warnings primarily indicate supply pressure.

### Expose a fixed analysis window independent of the chart period

`DetailSnapshot` will include basis-specific market history assembled from the repository's existing 31-day source window. Selecting a 7-day chart therefore does not make the radar ineligible. The chart remains controlled by the user's selected period.

Alternative considered: derive the signal from `trendPoints`. This would make results change or disappear when the user switches chart periods, even though the underlying market data is unchanged.

### Evaluate in the detail ViewModel

The ViewModel will combine detail snapshots with `MarketShockRepository.signals`, run the pure domain predictor, and publish the result in `DetailUiState`. It will refresh warning data when the detail screen becomes active.

This keeps Room responsible for observations, the domain responsible for analysis, and Compose responsible only for presentation.

### Use one card with explicit state hierarchy

The detail card will show:

1. Horizon and direction headline.
2. Signed projected magnitude and strength score for directional signals.
3. Secondary evidence chips only when they add information beyond the primary reason already shown in the summary.
4. Latest observation date and analyzed trading-day count.
Rising uses the app's red market color, falling uses green, and neutral/unavailable states use theme surfaces. Direction is always repeated through iconography and text so color is not the only cue.

## Risks / Trade-offs

- [Directional percentages need a clear meaning] → Keep the compact “預估變動” label beside the value without adding source or disclaimer copy.
- [Produce-level weather exposure is not geographically mapped] → Treat active agricultural-area warnings only as supporting evidence and still require item market evidence.
- [Thin or stale trading history can generate unstable signals] → Require ten valid trading days, reject observations older than four days, and expose specific unavailable states.
- [Price and volume signals may conflict] → Score rising and falling directions separately and return neutral when neither clearly dominates.
- [The home and detail predictors can diverge] → Preserve existing home behavior for now and cover the new predictor independently; a later change can consolidate them after product validation.

## Migration Plan

1. Add domain types and tests without changing existing home behavior.
2. Add basis-specific analysis history to the detail snapshot and repository.
3. Combine snapshot and warning flows in the detail ViewModel.
4. Add the Compose radar card and UI state previews/tests.
5. Validate domain tests and compile all affected Android modules.

Rollback removes the new state field, ViewModel evaluation, card, and snapshot history without requiring a database migration.

## Open Questions

None blocking. Thresholds are deliberately centralized in the pure predictor so they can be calibrated later without changing the UI contract.
