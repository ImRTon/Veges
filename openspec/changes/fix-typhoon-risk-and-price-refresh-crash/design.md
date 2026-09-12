## Context

The home screen currently feeds active CWA warnings directly into price predictors. Both predictors deliberately require independent wholesale price or volume evidence, so a newly issued weather warning cannot produce any visible state by itself. The typhoon CAP warning also commonly names a sea warning area, while typhoon-driven heavy-rain CAP records carry the actionable township and county areas in their description and area fields.

The app has no reviewed per-item production-origin mapping. It can therefore make a defensible market-wide statement about affected agricultural counties, but it cannot claim that a specific produce item comes from a particular warned county.

Price refresh uses a long-running `CoroutineWorker` and already supplies the matching manifest and runtime `dataSync` foreground-service type. An Android 14 emulator run completed without an `AndroidRuntime` exception, but two consecutive workers ran for one user action: the latest-price refresh followed by catalog-history backfill. During the run the process approached a 65 MB Java heap, about 177 MB total PSS, and about 76 MB swap PSS, with frequent garbage collection and skipped frames. The queue currently appends repeated manual work, catalog-history backfill has no request interval, and every published sync recalculates estimates and alerts even when it accepted no records and discovered no source days.

## Goals / Non-Goals

**Goals:**

- Show an early market-wide supply-risk state when active official warnings affect configured agricultural counties.
- Identify heavy rain as typhoon-driven only when the official warning description explicitly mentions a typhoon.
- Keep the early weather state separate from price forecasts that require observed market evidence.
- Reduce peak and repeated work during price refresh without changing its serialized execution model.
- Cover classification, refresh scheduling, and change-aware downstream-processing contracts with automated tests.
- Remove internal animation-preview controls from the user-facing home screen.
- Ensure every displayed price-rise reason and numeric projection is backed by the same qualifying evidence.

**Non-Goals:**

- Claim that an individual item originates in a warned area without reviewed origin data.
- Guarantee a future price increase or attach a forecast percentage to weather-only risk.
- Replace the existing item-direction or market price-surge models.
- Persist historical weather warnings in Room in this change.

## Decisions

1. Add an optional `cause` to `MarketShockSignal` using the existing weather-kind vocabulary. The CWA parser will flatten textual description content and set the cause to `TYPHOON` only for non-typhoon warnings whose official description explicitly contains `颱風`. This preserves the observed hazard (`HEAVY_RAIN`) while allowing accurate user-facing attribution.

2. Add a pure domain evaluator that filters active signals to named agricultural counties, normalizes township-level area descriptions to county names, and returns one prioritized market-wide risk. Typhoon-caused rain ranks ahead of other heavy rain, then extreme heat. Empty or sea-only areas do not qualify for a production-area warning.

3. Present the production-area signal inside the existing typhoon price-surge radar, using its existing animation rather than adding a separate colored banner. Copy is limited to the cause, a bounded affected-area summary, and the possible price consequence. It will not add disclaimer-style explanation or a weather-only percentage. Existing price-surge predictions remain governed by market-evidence thresholds.

4. Keep the initial risk market-wide. Adding reviewed produce-to-origin mappings is a separate data-governance change because the current taxonomy intentionally contains identity and official market mappings, not sourcing claims.

5. Enqueue manual refresh with `ExistingWorkPolicy.KEEP` on the existing one-time sync queue. Repeated taps while refresh work is active therefore coalesce instead of appending another worker, while automatic and historical requests continue to use the same serialized queue. Catalog-history backfill requests will be persisted and limited to once per 24 hours, with clock rollback treated as eligible so a bad future timestamp cannot suppress updates indefinitely.

6. Treat a published sync as source-changing only when it accepted at least one record or discovered at least one source day. Only source-changing runs recalculate estimates and evaluate alerts. Pending notification delivery still runs so an earlier valid event is not stranded, and the latest-price refresh may still request the rate-limited catalog backfill.

7. Remove the animation-test button, synthetic six-reason gallery, and production UI state used only to preview animations. Production warning animations remain unchanged and are covered through real radar states.

8. Centralize weather applicability and attribution. A warning qualifies only when it names a recognized agricultural county; empty or unknown areas fail closed. The effective user-facing weather kind is `cause ?: kind`, so typhoon-driven heavy rain is attributed to the typhoon while preserving the observed CAP warning kind in the source signal.

9. Weather remains a market-wide context signal because there is no reviewed produce-to-origin mapping. It must not add score, reasons, or projected percentage to an individual produce outlook. Market-wide affected-item qualification and percentages are calculated from observed price and volume evidence; an independently qualified production-area warning may describe the concurrent market context but cannot create or enlarge the affected item set.

10. Gate each numeric projection term with the same threshold that creates its visible reason. Sub-threshold price momentum, volume movement, or baseline anomaly contributes neither a reason nor a hidden percentage uplift. The existing momentum and longer-baseline anomaly weights remain unchanged pending separate backtesting; regression tests guard their boundary behavior and prevent weather from masking duplicated or insufficient market evidence.

11. Treat the Room market-history projection as an untrusted database boundary. Device crash reports show that the SQLite statement adapter can transiently surface a null projected `conceptId` while the observable query is invalidated during refresh, even though an offline integrity check reports a valid database and no stored null concept ids. Projection fields are therefore nullable at the DAO boundary, incomplete rows are discarded before grouping, and SQL retains explicit non-null predicates. Cached complete observations remain available and one incomplete projection row cannot terminate the main coroutine.

## Risks / Trade-offs

- [CWA wording changes and no longer includes `颱風`] → The signal remains a heavy-rain production risk instead of being incorrectly attributed to a typhoon.
- [Township names vary] → Normalize by matching known county-name prefixes and ignore unrecognized or sea-only areas.
- [A county is agricultural but the tracked item is not produced there] → Keep the warning market-wide and avoid item-specific claims.
- [Weather fetch fails after a successful fetch] → Continue retaining only unexpired in-memory signals as today; do not replace them with an empty result caused by transport failure.
- [`KEEP` ignores a tap while any one-time refresh is active] → Keep refresh status visible; the active serialized run already supplies the newest obtainable data and avoids duplicate memory-heavy work.
- [A throttled catalog backfill misses a newly added item for part of a day] → Latest-price refresh remains immediate; history is retried after the bounded 24-hour interval.
- [A zero-change run has an older pending notification] → Continue delivering pending notifications even while estimate and alert recomputation is skipped.
- [A warning omits or changes its area structure] → Fail closed for price reasoning instead of treating an area-less warning as nationwide evidence.
- [A county warning affects only some produce] → Keep it market-wide and do not alter any single-item score until reviewed origin mappings exist.
- [Removing the preview control reduces manual animation inspection] → Retain automated semantics coverage for real radar states and inspect animation changes through previews or development tooling outside production UI.

## Migration Plan

No database migration is required. Domain constructors receive defaults where needed so existing call sites remain source-compatible. Refresh throttling adds one preference timestamp that can be safely removed during rollback. Rollback consists of removing the new home risk state and restoring the previous queue and downstream-processing policies; cached price data is unaffected.

## Open Questions

None for this change. Item-specific production-origin warnings remain intentionally deferred until reviewed origin data exists.
