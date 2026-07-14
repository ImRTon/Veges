# Historical Model Audit

Generated: 2026-07-14T04:00:00Z

This audit used official source downloads and did not publish a model artifact.

## Inputs

- MOA endpoint: `FarmTransData.aspx?IsTransData=1&UnitId=037&StartDate=113.01.01&EndDate=114.12.31&$top=9999&$skip=0`, queried separately for Taipei First/Second and `甘藍`, `香蕉`, `小白菜`.
- MOA files: 6 JSON snapshots, 2024-01-02 through 2025-12-31, with source checksums recorded in `release-tool/build/audit/history/historical-audit.json`.
- Taipei retail dataset: `54d9d492-1e2e-40d1-ae7b-fbce6f271bf1`, 18 monthly CSV resources from 113年7月 through 114年12月.
- Retail mapping: `release-tool/src/main/resources/catalog/retail-calibration-mappings.json`.
- Estimator: expanding-window seasonal median baseline; each validation prediction used only prior rows.

## Results

| Concept | Basis | Wholesale valid days | Retail periods | Backtest periods | MAE | RMSE |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| fruit.banana | combined | 586 | 18 | 17 | 14.82352941 | 20.398962 |
| fruit.banana | first | 586 | 18 | 17 | 14.82352941 | 20.398962 |
| fruit.banana | second | 586 | 18 | 17 | 14.82352941 | 20.398962 |
| vegetable.cabbage | combined | 586 | 18 | 17 | 21.58823529 | 26.971663 |
| vegetable.cabbage | first | 586 | 18 | 17 | 21.58823529 | 26.971663 |
| vegetable.cabbage | second | 586 | 18 | 17 | 21.58823529 | 26.971663 |
| vegetable.small-bok-choy | first | 586 | 18 | 17 | 7.00000000 | 8.14212 |

The remaining concept/basis pairs were excluded because the current reviewed MOA input set did
not contain a matching official code/mapping for that basis. The audit did not silently substitute
another market.

## Gate Status

- The approved minimum is 30 paired calibration periods, where each period has both valid wholesale
  input and a historical retail target. Only 18 monthly retail periods are currently available.
- Wholesale source history exceeds 30 days, but that does not by itself validate a retail calibration
  model.
- Interval coverage and confidence calibration were not calculated by this baseline and remain
  unavailable.
- Coverage, recency, point-error, interval, confidence, and staleness thresholds remain pending
  explicit approval.
- Every future model-derived price must retain the `估算` tag and `Taipei retail reference estimate`
  label.
