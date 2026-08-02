## 1. Domain Direction Analysis

- [x] 1.1 Add item direction, availability, evidence, and outlook domain models with an explainable pure predictor
- [x] 1.2 Cover rising, falling, neutral, insufficient-history, stale-data, weather-only, and expired-weather behavior with unit tests

## 2. Detail Data Flow

- [x] 2.1 Expose a fixed basis-specific wholesale analysis history from `DetailRepository` without coupling it to the selected chart period
- [x] 2.2 Combine detail snapshots with market shock signals in `DetailViewModel` and publish the item evaluation in `DetailUiState`

## 3. Detail Experience

- [x] 3.1 Add an accessible Material 3 item price radar card for rising, falling, neutral, stale, and insufficient states
- [x] 3.2 Remove explanatory source, methodology, and disclaimer copy from the item radar while retaining core signal labels
- [x] 3.3 Remove the primary reason chip already repeated by the signal summary while retaining distinct secondary evidence chips

## 4. Verification

- [x] 4.1 Update detail UI semantics coverage so removed explanatory copy is not exposed visually or to accessibility services
- [x] 4.2 Run affected Android compilation, device UI tests, text search, and OpenSpec validation
- [x] 4.3 Update the radar UI test and rerun compilation, device tests, and strict OpenSpec validation
