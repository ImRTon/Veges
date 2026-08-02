# Catalog Illustration Review

Reviewed: 2026-07-29

Reviewers: project owner (original vegetable set), `codex-image-review` (fruit expansion)

Decision: all 175 exact release hashes approved for recognizability, botanical plausibility,
distinct produce identity, transparent-background rendering, and package use.

## Review Criteria

- The household produce concept is recognizable at catalog-card size.
- Similar leafy vegetables, the five `SX0`-`SX4` sprout concepts, and visually related fruit
  families remain distinct at catalog-card size.
- Form, leaves, stems, and fruit arrangement are botanically plausible for an illustration.
- No logo, packaging, certification, origin, farm, store, grade, or quality claim is shown.
- Assets remain labeled `AI 生成示意圖，非實物照片。` in taxonomy metadata and UI semantics.

## Approved Release Set

- 175 unique transparent WebP files: 120 vegetable concepts and 55 fruit concepts.
- Maximum edge: 512 pixels.
- Total package size: 5,475,618 bytes.
- Largest asset: 88,506 bytes.
- Duplicate SHA-256 hashes: none.
- Transparent-corner check: pass for every asset.

The complete concept-to-file list, exact SHA-256 values, dimensions, byte sizes, alpha checks, and
per-asset approval metadata are recorded in
`release-tool/audits/2026-07-29-catalog-illustration-audit.json`.

The approval applies only to those exact hashes. Replacing, editing, or re-encoding an asset
requires a new illustration review and updated taxonomy image metadata.

The original 121-asset vegetable review remains available as the historical
`2026-07-27-catalog-illustration-audit.json` audit.

## Fruit Carousel Category Banners

Seven purpose-drawn opaque WebP banners were approved for the fruit-market Carousel:
`fruit-citrus`, `fruit-melon`, `fruit-tropical`, `fruit-orchard`, `fruit-stone`,
`fruit-berry`, and `fruit-taiwan`.

- Dimensions: 768 × 512 pixels.
- Total package size: 264,838 bytes.
- Largest asset: 43,174 bytes.
- Duplicate SHA-256 hashes: none.
- Composition check: category produce remains on the center-right with a dark left-side text area.
- Content check: no text, logo, container, watermark, collage seam, or out-of-category produce.

The compressed-output contact sheet is recorded at
`release-tool/audits/illustration-review/2026-07-29-fruit-categories.jpg`.
