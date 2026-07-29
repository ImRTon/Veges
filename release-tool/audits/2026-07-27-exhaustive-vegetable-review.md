# Exhaustive Taipei Vegetable Catalog Review

Prepared: 2026-07-27

Mapping breadth decision: `APPROVED_BY_PROJECT_OWNER`

Illustration decision: `APPROVED_BY_PROJECT_OWNER` (`121/121` exact image hashes)

## Scope and reviewed grouping

- Official source: Ministry of Agriculture `FarmTransData.aspx`
- Dataset kind: `N04` vegetables
- Markets: Taipei First (`109`) and Taipei Second (`104`)
- Audit window: ROC `115.04.27` through `115.07.26`
- Retrieved records: 30,876 across 6 deterministic pages
- Distinct official commodity codes retained: 339
- Browseable vegetable concepts after reviewed semantic grouping: 120
- Reviewed fruit concepts retained: 1
- Sparse official varieties with fewer than 30 observed days: 78

The project owner explicitly requested all vegetable kinds. Every official code
remains mapped exactly once. Sharing the official name before the first hyphen is
a necessary but not sufficient grouping condition: reviewed origin, packaging,
color, grade, and other non-identity modifiers may remain under one household
concept, while suffixes that name distinct edible produce remain separate. The
official `SX0` through `SX4` codes are therefore published as `其他芽菜`, `綠豆芽`,
`黃豆芽`, `豌豆芽`, and `苜蓿芽`, each with independent search, tracking, history,
and illustration identities. For example, the household alias `高麗菜` still
resolves to the official `甘藍` concept and does not create a duplicate image or
price series. A sparse variety remains browseable; the 30-day threshold controls
only history and estimate eligibility.

Machine-readable evidence:

- `2026-07-26-exhaustive-vegetable-taxonomy.json`
- `../src/main/resources/catalog/moa-n04-vegetable-inventory-2026-07-26.json`

## Illustration and package audit

Each of the 121 concepts has a separate transparent WebP. There are no atlases,
shared files, blank assets, or duplicate SHA-256 hashes. The release set is
512×512 pixels, totals 3,752,362 bytes, and the largest file is 88,506 bytes.
High-resolution and chroma-key generation sources are excluded from the Android
asset directory.

Eight initially misleading rare-produce images were regenerated after visual
review: `其他花類`, `樊花`, `人參葉`, `藤川七`, `其他菇類`, `半天筍`,
`草石蠶`, and `半天花`. The corrected images use the documented edible plant
form rather than literal word associations.

Four additional transparent illustrations distinguish `綠豆芽`, `黃豆芽`,
`豌豆芽`, and `苜蓿芽`; the existing `SX0` illustration is retained only for
`其他芽菜`. On 2026-07-27, the project owner first approved the exact SHA-256
hashes for these five `SX0`-`SX4` sprout assets and then explicitly approved the
remaining catalog images. All 121 exact release hashes are approved.

Exact hashes, dimensions, alpha checks, and byte sizes are recorded in:

- `2026-07-27-catalog-illustration-audit.json`

These exact release hashes are approved for publication. Re-encoding or replacing
any image requires a new hash review.

## Source references

- https://data.moa.gov.tw/open_detail.aspx?id=037
- https://data.gov.tw/dataset/8066
- https://www.tapmc.com.tw/Pages/Market/Flowers
- https://kmweb.moa.gov.tw/subject/subject.php?id=21171
- https://kmweb.moa.gov.tw/theme_data.php?id=86098&theme=fengnian
- https://kmweb.moa.gov.tw/theme_data.php?id=433&theme=plant_illustration
