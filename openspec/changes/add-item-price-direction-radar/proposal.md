## Why

The existing price-surge model evaluates individual produce items, but the app only exposes a market-wide summary and offers no equivalent price-decline outlook. Users need an understandable item-level view on the detail screen so they can distinguish recent historical movement from a qualified 7–14 day directional signal.

## What Changes

- Add a symmetric, explainable item-level price-direction evaluation that can report rising risk, falling opportunity, or no material signal.
- Reuse wholesale price and volume history plus active official weather warnings, while keeping weather effects asymmetric and requiring market evidence.
- Expose the selected item's 7–14 day direction, projected change range, score, evidence, freshness, and data sufficiency through the detail state.
- Add a visually clear Material 3 radar card to each produce detail page with distinct rising, falling, neutral, and insufficient-data states.
- Preserve the market-wide price-surge radar.
- Add domain and UI tests covering rising, falling, neutral, stale, and insufficient-history behavior.

## Capabilities

### New Capabilities

- `item-price-direction-radar`: Explainable 7–14 day rising and falling price signals for an individual produce item, including evidence, eligibility, and detail-screen presentation.

### Modified Capabilities

None.

## Impact

- Domain price-signal models and predictor logic.
- Detail repository contracts, Room-backed detail data flow, ViewModel state, and Compose UI.
- Existing home market-wide surge integration may consume a compatibility projection of the new item evaluations.
- Domain unit tests and detail/UI semantics tests.
