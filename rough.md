# Taipei Produce Price App

## Confirmed Direction

- Android app for Taipei household shoppers.
- Cover vegetables and fruit.
- Let users search and browse using Taiwan daily names, not only official market names or codes.
- Use images to help users recognize produce; do not include camera-based produce recognition.
- Let users track recurring produce and set price alerts.
- Aim for a polished finance-app-like experience with Material 3 components.
- Show an estimated Taipei retail reference price, not a wholesale price presented as a retail price.
- Default to a Taipei combined reference. Advanced users can choose the underlying wholesale-market source.
- Use GPT-generated images as representative catalog illustrations.

## Product Positioning

The app answers:

> "What is a reasonable estimated Taipei retail price for the fruit and vegetables I usually buy, and has it changed enough to matter?"

The app must clearly distinguish:

- `Retail reference estimate`: model output, not an observed store or market price.
- `Wholesale source`: the market data used by the estimate.
- `Historical retail observation`: an official monthly aggregate used for calibration when available.

## Data Model

```text
Taipei retail reference estimate
  <- selected wholesale source
     - Taipei combined (default)
     - Taipei First
     - Taipei Second
  <- official wholesale price and volume
  <- historical Taipei retail averages for calibration
  <- produce concept and official commodity mappings
```

### Primary data

- MOA wholesale transaction data: daily market, crop code/name, upper/middle/lower/average prices, and volume.
- Taipei First and Taipei Second are the default inputs for the Taipei combined reference.
- Taipei public retail-market data: monthly citywide averages in NTD/台斤. It is useful for calibration, but the latest verified data currently ends in December 2025.

### Estimation rules

- Default output: `Taipei combined retail reference estimate`.
- Default source: Taipei First and Taipei Second, combined using a disclosed method such as transaction-volume weighting.
- Advanced setting: switch the estimate basis to Taipei First or Taipei Second.
- Display prices in NTD/台斤 for household shopping, alongside the estimate date and source-market date.
- Show a central estimate, an uncertainty range, and a confidence level only after backtesting validates the model.
- Do not directly convert wholesale NTD/kg to retail NTD/台斤 with a fixed multiplier.
- Do not call the result an observed retail price, lowest price, store price, or guaranteed price.
- If a concept lacks enough retail calibration data or a reliable commodity mapping, do not show a retail estimate.

### Data states

- A closed-market or zero-value record is not a zero price.
- On closed days, show the latest valid source date and do not trigger an alert.
- Show data freshness separately for wholesale inputs and retail calibration.

## Produce Names

Use a maintained taxonomy between official data and the app UI.

```text
Household concept: 高麗菜（甘藍）
  aliases: 高麗菜, 甘藍
  official variants: LA1 甘藍・初秋, LA2 甘藍・改良種
  image: representative catalog illustration
  prices: exact source observations and optional concept-level estimate
```

Rules:

- Show daily names first and official names/codes as context.
- Keep exact price observations tied to official code, market, and date.
- Ask users to choose when a term is ambiguous, such as `白菜`.
- Do not silently map an ambiguous daily name to one official commodity.
- Maintain separate mappings for vegetables and fruit.

LLMs may help propose aliases or find unmatched search terms, but reviewed taxonomy data is authoritative. Do not use an LLM to guess every user search.

## Core Experience

### Home: tracked produce

The main screen centers on items a household buys repeatedly.

```text
Your tracked produce
Taipei combined retail reference estimate

[image] 高麗菜       NT$52 / 台斤     +8%
        estimated range NT$46-58

[image] 香蕉         NT$45 / 台斤     -5%
        estimated range NT$40-51

Price alerts
高麗菜 below NT$40 / 台斤
```

### Search and browse

- Search daily names and aliases.
- Browse visual fruit and vegetable categories.
- Use representative images to disambiguate similar produce.
- Show a choice sheet instead of guessing ambiguous names.

### Produce detail

```text
[AI-generated illustrative image]
高麗菜（甘藍）

Taipei retail reference estimate
NT$52 / 台斤
Estimated range NT$46-58
Confidence: medium

[7D] [30D] [90D] [1Y]
Estimated retail trend
Wholesale input trend and volume

Data basis: Taipei combined
[Change source]
[Track] [Set alert]
```

Do not draw conventional stock candlesticks. The wholesale source provides upper/middle/lower/average prices and volume, not stock open/high/low/close data. Use a price-range and average trend visualization instead.

### Alerts

An alert is defined against an estimated retail reference, for example:

```text
Produce: 高麗菜（甘藍）
Basis: Taipei combined
Metric: estimated retail reference price
Condition: below NT$40 / 台斤
```

Only evaluate after valid new source data. Do not trigger alerts from missing, closed, or zero-value records.

## Visual Direction

- Use Material 3 components such as `SearchBar`, `NavigationBar`, `FilterChip`, `ModalBottomSheet`, `ListItem`, and `SegmentedButton`.
- Borrow finance-app clarity: strong price hierarchy, fast trend scanning, compact watchlist behavior, and smooth chart interaction.
- Do not copy Robinhood branding, trade dress, or investment language.
- Avoid using color alone for price movement.

### Images

- Use GPT-generated images only as illustrative catalog art, never as an actual product, store, farm, package, variety, origin, or grade photo.
- Label images: `AI 生成示意圖，非實物照片。`
- Keep generated art free of logos, labels, packaging, certification marks, and misleading origin or quality cues.
- Review every image for recognizability and botanical plausibility before release.

## MVP Scope

Include:

- Taipei vegetables and fruit.
- Daily wholesale inputs from Taipei First and Taipei Second.
- A default Taipei combined retail reference estimate.
- Advanced source selection for Taipei First and Taipei Second.
- Curated daily-name aliases and official-code mappings.
- Visual browsing and search.
- Tracked produce, trend views, and threshold alerts.
- Transparent source, date, uncertainty, and confidence disclosures.

Exclude initially:

- Camera/image identification.
- Store-by-store or retailer price comparison.
- Claims of observed live retail prices.
- Automatic wholesale-to-retail fixed-rate conversion.
- Nationwide retail estimates.

## Open Questions

1. Which vegetables and fruit are in the launch catalog?
2. Which exact estimation method passes backtesting well enough to show a confidence range?
3. How much historical retail data is sufficient before an item is eligible for an estimate?
4. Should users be able to see raw wholesale prices, or only an expandable methodology view?
5. What alert conditions are useful beyond a simple price threshold?

## Sources

- MOA agricultural transaction data: https://data.gov.tw/dataset/8066
- MOA crop catalog: https://data.moa.gov.tw/api/v1/CropType
- AMIS product selector: https://amis.afa.gov.tw/Selector/VegProductSelector.aspx
- Taipei public retail-market history: https://data.taipei/dataset/detail?id=54d9d492-1e2e-40d1-ae7b-fbce6f271bf1
- Taipei monthly food supply and sales: https://data.taipei/dataset/detail?id=925ba4ba-9e3c-4ae9-98fc-95ddfe14ea90
