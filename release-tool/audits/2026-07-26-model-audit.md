# Initial model-family audit — 2026-07-26

## Decision

The approved publication policy was enforced without exceptions. No point-estimate model is publishable from this audit because the latest available Taipei retail calibration period is 2025-12-01, 237 days before artifact generation. That exceeds both the 45-day generation-recency gate and the 62-day on-device artifact lifetime.

The generated MVP artifact is intentionally fail-closed: it contains zero estimator entries and 15 explicit concept/source-basis exclusions. Interval and confidence output remain disabled.

## Inputs and reproducibility

- Generated at: `2026-07-26T00:00:00Z`
- Wholesale inputs: 6 MOA historical API snapshots, 15,982 valid source rows before aggregation
- Retail inputs: all 18 Taipei monthly CSV resources from 2024-07 through 2025-12
- Candidate families: `LINEAR_CALIBRATION`, `LOG_LINEAR_CALIBRATION`, `SEASONAL_BASELINE`
- Evaluation: expanding-window backtest with no future observations in each fitted period
- Machine-readable audit: `src/main/resources/model/historical-audit-2026-07-26.json`
- Checksummed artifact: `src/main/resources/model/mvp-model-artifact.json`
- Embedded artifact checksum: `e6d850d7dd984a42b26acea311a999b93560ee56e3030032c8c29b2a882ca760`
- Repeated generation produced byte-identical SHA-256: `37A03A58B235DA83EFD05B01D7B40DB201AD4F2FD382FAB9F40DB00A9C3BF2A1`

Every input filename and SHA-256 is recorded in both machine-readable files. The checked-in release-tool test validates the artifact checksum, expected exclusions, family coverage, data cutoff, and fail-closed state.

## Approved gates

| Gate | Approved value |
|---|---:|
| Paired calibration periods | at least 18 |
| Paired coverage | at least 90% |
| Generation recency | at most 45 days |
| MAE | at most NT$15/台斤 |
| RMSE | at most NT$22/台斤 |
| Live wholesale freshness | at most 36 hours |
| Calibration artifact lifetime | at most 62 days |
| Interval output | disabled |
| Confidence output | disabled |
| Unvalidated families | disabled |

## Family-level outcome

| Family | Audited concept/basis rows | Observed MAE range | Observed RMSE range | Eligible rows |
|---|---:|---:|---:|---:|
| `LINEAR_CALIBRATION` | 15 | 5.51–9.27 | 6.73–13.30 | 0 |
| `LOG_LINEAR_CALIBRATION` | 15 | 5.77–10.16 | 6.81–14.54 | 0 |
| `SEASONAL_BASELINE` | 15 | 8.69–25.38 | 9.30–29.09 | 0 |

Ranges exclude rows with no aligned calibration data. All otherwise accurate rows still fail `CALIBRATION_TOO_OLD_AT_GENERATION`. The cabbage seasonal baseline additionally fails both accuracy gates.

## Rows with aligned calibration data

| Concept | Basis | Family | Paired / backtest periods | MAE | RMSE | Eligibility reason |
|---|---|---|---:|---:|---:|---|
| Banana | Combined | Linear | 18 / 16 | 7.0598 | 8.8504 | calibration too old |
| Banana | Combined | Log-linear | 18 / 16 | 7.7776 | 10.7269 | calibration too old |
| Banana | Combined | Seasonal | 18 / 16 | 14.8125 | 21.0661 | calibration too old |
| Banana | First market | Linear | 18 / 16 | 6.6779 | 8.3085 | calibration too old |
| Banana | First market | Log-linear | 18 / 16 | 7.5043 | 10.2548 | calibration too old |
| Banana | First market | Seasonal | 18 / 16 | 14.8125 | 21.0661 | calibration too old |
| Banana | Second market | Linear | 18 / 16 | 7.7298 | 9.8705 | calibration too old |
| Banana | Second market | Log-linear | 18 / 16 | 8.2634 | 11.5643 | calibration too old |
| Banana | Second market | Seasonal | 18 / 16 | 14.8125 | 21.0661 | calibration too old |
| Cabbage | Combined | Linear | 18 / 16 | 9.0015 | 12.9517 | calibration too old |
| Cabbage | Combined | Log-linear | 18 / 16 | 9.4851 | 13.0185 | calibration too old |
| Cabbage | Combined | Seasonal | 18 / 16 | 25.3750 | 29.0893 | old; MAE and RMSE too high |
| Cabbage | First market | Linear | 18 / 16 | 8.9680 | 13.3021 | calibration too old |
| Cabbage | First market | Log-linear | 18 / 16 | 9.3491 | 13.1104 | calibration too old |
| Cabbage | First market | Seasonal | 18 / 16 | 25.3750 | 29.0893 | old; MAE and RMSE too high |
| Cabbage | Second market | Linear | 18 / 16 | 9.2733 | 13.1871 | calibration too old |
| Cabbage | Second market | Log-linear | 18 / 16 | 10.1557 | 14.5370 | calibration too old |
| Cabbage | Second market | Seasonal | 18 / 16 | 25.3750 | 29.0893 | old; MAE and RMSE too high |
| Small bok choy | First market | Linear | 18 / 16 | 5.5086 | 6.7287 | calibration too old |
| Small bok choy | First market | Log-linear | 18 / 16 | 5.7742 | 6.8082 | calibration too old |
| Small bok choy | First market | Seasonal | 18 / 16 | 8.6875 | 9.3039 | calibration too old |

The remaining 24 family rows have no validated retail-to-wholesale mapping or no valid wholesale input for that source basis. They fail paired-period, paired-coverage, metric-presence, and input-validity gates. Exact reason codes are preserved in the machine-readable audit and artifact.

## Release implication

This audit validates the estimator implementations and publication gates, but it does not authorize publishing estimates. A later operator run must supply a newly approved retail calibration source, regenerate the audit and artifact, and pass the same gates before estimator entries can ship.
