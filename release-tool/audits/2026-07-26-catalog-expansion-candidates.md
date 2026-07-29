# Catalog Expansion Candidate Review

> Superseded by `2026-07-27-exhaustive-vegetable-review.md`. This earlier
> 32-concept shortlist is retained only as historical evidence.

Prepared: 2026-07-26

Status: `PENDING_PROJECT_OWNER_APPROVAL`

This document proposes expanding the five-concept pilot to 32 household produce
concepts: 21 vegetables and 11 fruits. It is review evidence only. No added concept
may be marked `PUBLISHED`, bundled into the app taxonomy, or treated as an approved
retail estimate until the project owner approves the household-to-official mapping
choices and the exact generated illustration hashes.

## Source Evidence

- Source: Ministry of Agriculture, Taiwan, `FarmTransData.aspx`
- Markets: Taipei First (`台北一`, market code `109`) and Taipei Second
  (`台北二`, market code `104`)
- Query window: ROC `115.04.27` through `115.07.26`
- Query controls: explicit `StartDate`, `EndDate`, `Market`, `$top=9999`, and
  `$skip=0`
- Inclusion floor: every proposed official code below appeared on at least 34
  distinct trading dates across the review window and had observations in both
  selected markets
- Runtime retention: the app independently requests and retains a rolling one-year
  history after an approved mapping is published

Official dataset documentation:
https://data.moa.gov.tw/open_detail.aspx?id=037

## Proposed Vegetables

| # | Household concept | Proposed official mapping(s) | Valid dates | Mapping decision |
|---:|---|---|---:|---|
| 1 | 高麗菜 | `LA1 甘藍-初秋`; `LA2 甘藍-改良種` | 35 each | Existing approved aggregation |
| 2 | 小白菜 | `LB1 小白菜-土白菜`; `LB2 小白菜-蚵仔白`; `LB3 小白菜-奶油白` | 35 each | Existing approved aggregation |
| 3 | 包心白菜 | `LC1 包心白-包白` | 35 | Existing approved exact mapping |
| 4 | 青江白菜 | `LD1 青江白菜-小梗` | 35 | Existing approved exact mapping |
| 5 | 牛番茄 | `FJ3 番茄-牛番茄` | 35 | Exact household variety |
| 6 | 胡蘿蔔 | `SB1 胡蘿蔔-未洗`; `SB2 胡蘿蔔-清洗` | 34 each | Aggregate washing state |
| 7 | 大黃瓜 | `FC1 胡瓜-黑刺` | 35 | Household naming alias for 胡瓜 |
| 8 | 小黃瓜 | `FD1 花胡瓜` | 35 | Household naming alias for 花胡瓜 |
| 9 | 絲瓜 | `FF1 絲瓜` | 35 | Exact mapping; excludes 角瓜 |
| 10 | 地瓜葉 | `LO1 甘薯葉` | 34 | Household naming alias |
| 11 | 空心菜 | `LF2 蕹菜-小葉`; `LF3 蕹菜-水蕹菜` | 35 each | Aggregate common market forms |
| 12 | 青蔥 | `SE1 青蔥-日蔥`; `SE6 青蔥-粉蔥` | 34 each | Aggregate common varieties |
| 13 | 茄子 | `FI2 茄子-麻荸茄` | 35 | Common long-eggplant mapping |
| 14 | 苦瓜 | `FG1 苦瓜-白大米`; `FG4 苦瓜-翠綠` | 35 each | Aggregate white and green forms |
| 15 | 玉米 | `FY6 玉米-甜軟殼` | 35 | Sweet-corn mapping; excludes corn shoots |
| 16 | 洋蔥 | `SD1 洋蔥-本產` | 34 | Exact local-produce mapping |
| 17 | 馬鈴薯 | `SC1 馬鈴薯-本產` | 34 | Exact local-produce mapping |
| 18 | 青椒 | `FK5 甜椒-青椒` | 35 | Exact household variety |
| 19 | 杏鮑菇 | `MJ1 杏鮑菇` | 34 | Exact mapping |
| 20 | 青花菜 | `FR9 青花苔-進口` | 35 | Official imported-broccoli mapping |
| 21 | 花椰菜 | `FB1 花椰菜-青梗` | 35 | Exact household mapping |

## Proposed Fruits

| # | Household concept | Proposed official mapping(s) | Valid dates | Mapping decision |
|---:|---|---|---:|---|
| 22 | 香蕉 | `A1 香蕉`; `A2 香蕉-芭蕉紅芭蕉` | 35 each | Existing approved aggregation |
| 23 | 芭樂 | `P1 番石榴-珍珠芭` | 34 | Launch as the common 珍珠芭 variety |
| 24 | 愛文芒果 | `R1 芒果-愛文` | 34 | Exact household variety |
| 25 | 木瓜 | `I1 木瓜-網室紅肉`; `I3 木瓜-日昇種` | 35 each | Aggregate ripe-eating varieties; exclude 青木瓜 |
| 26 | 金鑽鳳梨 | `B2 鳳梨-金鑽鳳梨` | 35 | Exact household variety |
| 27 | 酪梨 | `G3 酪梨` | 35 | Exact mapping |
| 28 | 火龍果 | `811 紅龍果-白肉`; `812 紅龍果-紅肉`; `813 紅龍果-雙色紅龍果` | 35 each | Aggregate flesh colors |
| 29 | 檸檬 | `F1 雜柑-檸檬`; `F5 雜柑-無子檸檬` | 35 each | Aggregate seeded and seedless forms |
| 30 | 富士蘋果 | `X69 蘋果-富士進口` | 34 | Exact imported variety |
| 31 | 巨峰葡萄 | `S1 葡萄-巨峰` | 34 | Exact household variety |
| 32 | 百香果 | `51 百香果-改良種` | 35 | Common improved-variety mapping |

Every mapping above applies separately to both Taipei First and Taipei Second. A
combined basis remains unavailable unless both markets have valid same-date data;
the app must not silently substitute one market for the other.

## Explicit Decisions Requested

Recommended approval is the complete 32-concept set above, including these
intentional household aggregations:

1. 胡蘿蔔 combines washed and unwashed lots.
2. 空心菜 combines small-leaf and water forms.
3. 青蔥 combines 日蔥 and 粉蔥.
4. 苦瓜 combines white and green forms.
5. 木瓜 combines 網室紅肉 and 日昇種 but excludes 青木瓜.
6. 火龍果 combines white-, red-, and dual-color flesh.
7. 檸檬 combines standard and seedless lots.

Approval of this candidate list authorizes generating one claim-free representative
illustration per added concept for a separate visual/hash review. It does not approve
those future images before they are shown.
