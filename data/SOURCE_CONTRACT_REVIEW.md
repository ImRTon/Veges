# Official Source Contract Review

Reviewed: 2026-07-14

## MOA Wholesale

- Dataset: 農產品交易行情, data.gov.tw dataset 8066, unit 037.
- Bulk endpoint: `https://data.moa.gov.tw/Service/OpenData/FromM/FarmTransData.aspx?IsTransData=1&UnitId=037`.
- Crop metadata: `https://data.moa.gov.tw/api/v1/CropType`.
- License: 政府資料開放授權條款-第1版.
- Update cadence: daily.
- Observed response type: JSON array, approximately 2.3 MB at review time.
- Observed fields: `交易日期`, `種類代碼`, `作物代號`, `作物名稱`, `市場代號`, `市場名稱`, `上價`,
  `中價`, `下價`, `平均價`, `交易量`.
- Observed numeric units: price fields are NTD/kg; transaction volume is kg.
- Observed date format: Republic of China calendar, `YY.MM.DD`, for example `115.07.14`.
- Reviewed Taipei market identifiers: `104` / `台北二` and `109` / `台北一`.
- Crop code authority: the crop metadata endpoint returns `{CropCode, CropName}` records.
- Attribution: retain source name, endpoint, retrieval timestamp, market, crop code, and original
  values in persisted provenance.

## Taipei Retail Calibration

- Dataset: 臺北市公有零售市場行情, Taipei dataset `54d9d492-1e2e-40d1-ae7b-fbce6f271bf1`.
- Reviewed December 2025 resource: `rid=6cac6912-0e92-47f3-bd3e-d97a9a991626`.
- Resource format: CSV.
- Fields: `序號`, `縣市名`, `縣市別代碼`, `項目`, `平均（元/台斤）`.
- Unit: NTD/台斤.
- Missing value: upstream dash (`-`) means unavailable and must not be parsed as zero.
- Current review observation: the resource list exposes data through 114年12月份, corresponding to
  December 2025. This is a calibration history, not a live retail feed.
- License/terms: the dataset page describes the resource as public (`公開`); release attribution
  must still link back to the Taipei Data Platform and preserve the resource identifier.

## Findings And Limits

- The reviewed MOA bulk endpoint does not expose a pagination contract in its URL or response shape.
  The adapter must treat the response as a bounded daily snapshot and fail closed if the shape changes.
- The reviewed MOA transaction records do not contain an official market-closure field. Missing
  rows therefore mean `MISSING`, never `CLOSED`; closure requires a separate reviewed official signal.
- No request-rate limit or conditional-request header contract was documented by the reviewed dataset
  page. The client must use bounded timeouts, capped retries, and conservative request frequency.
- The MOA sample uses NTD/kg while retail calibration uses NTD/台斤. Conversion may support display
  normalization, but retail estimates must still come from a validated calibration model rather than
  a fixed multiplier.
