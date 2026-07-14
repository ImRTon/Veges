# Reviewed Source Fixtures

These small fixtures are reviewed contract examples, not production snapshots. They preserve the
field names, units, date representations, market identifiers, and missing-value semantics needed by
adapter tests without bundling the full upstream payload.

- `moa-wholesale-success.json` mirrors the MOA `FarmTransData` JSON shape for Taipei First (`109`)
  and Taipei Second (`104`). Prices are NTD/kg and volume is kg. The source date is a Republic of
  China calendar date (`115.07.14`), not an ISO date.
- `moa-crop-metadata.json` mirrors `https://data.moa.gov.tw/api/v1/CropType` and keeps crop code to
  official crop name mapping separate from transaction observations.
- `taipei-retail-december-2025.csv` mirrors the Taipei public retail-market CSV. The average column
  is NTD/台斤. A dash in the upstream file means unavailable, not zero.
