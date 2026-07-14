# Privacy, Attribution, And Logging

## Privacy

- The app has no account, sign-in, proprietary backend, cloud database, or custom server.
- Produce tracking, alert rules, cached data, model artifacts, and catalog state remain on device.
- User preferences and local notification history are not uploaded by the app.
- Network requests go directly to the configured official government sources over HTTPS.

## Data Attribution

- MOA wholesale observations: [農產品交易行情](https://data.gov.tw/dataset/8066), licensed under the
  Government Data Open License, version 1.
- MOA crop metadata: `https://data.moa.gov.tw/api/v1/CropType`.
- Taipei retail calibration: [臺北市公有零售市場行情](https://data.taipei/dataset/detail?id=54d9d492-1e2e-40d1-ae7b-fbce6f271bf1).
- Historical retail data is calibration input and must not be described as live store pricing.

## User-Facing Claims

- Wholesale values are labeled as wholesale observations.
- Model-derived values are labeled `估算` and `Taipei retail reference estimate`.
- The estimate is not an observed store price, guaranteed price, lowest price, or live retail quote.
- Generated catalog art is labeled `AI 生成示意圖，非實物照片。`.
