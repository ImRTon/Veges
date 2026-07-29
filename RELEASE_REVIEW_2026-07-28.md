# MVP release review — 2026-07-28

This is the final unsigned-candidate review record for `tw.taipei.veges` version `0.1.0`
(`versionCode` 1). All code, catalog, model, device, migration, privacy, and build gates are
complete. Signing identity and Play Console ownership remain external owner-controlled release
steps.

## Accessibility and UI

Result: pass.

- Material 3 light and dark schemes use explicit high-contrast surface/on-surface pairs.
- Interactive Material components retain their platform minimum touch target.
- Estimated values expose both `估算` and `Taipei retail reference estimate`.
- AI illustrations expose `AI 生成示意圖，非實物照片。`.
- Ambiguous aliases require an explicit reviewed-concept choice.
- Source-basis controls use text labels and do not communicate state through color alone.
- The produce-market interval K-line has an equivalent Traditional Chinese semantic summary. Its
  legend and inspector define the wick as official low/high, the body as previous/current average,
  and volume as a separate observed series.
- Compose tests cover navigation restoration, disclosures, ambiguity, source switching,
  unavailable state, chart semantics, 200% font scale, 48 dp targets, body-text contrast, and
  light/dark screenshot smoke.

Device evidence:

- The Compose accessibility/navigation suite passed 8/8 on both the 2 GiB/2-core low profile and
  the 4 GiB/4-core mid profile.
- Semantic-tree checks cover disclosures, chart summaries, source controls, ambiguity choices,
  touch targets, light/dark contrast, navigation restoration, and official-code search.
- Home, market, detail, chart, offline market, and denied-notification screens were inspected at
  standard scale. Home, market, and detail were also inspected at 200% font scale.
- The detail source controls now wrap at large font size and the estimate header stacks
  vertically. All primary content remains reachable by vertical scrolling.

## Traditional Chinese copy

Static copy review result: pass.

- Uses `參考估算` for model output and reserves observation language for wholesale source data.
- Uses `官方品項` rather than language implying government identity or app affiliation.
- Units are explicit as `元 / 台斤` for estimates and `元/公斤` plus kilograms of volume for the
  wholesale market K-line.
- Background scheduling is disclosed as best-effort.
- Government data sources and the independent/non-government status are visible in methodology
  copy and documented in `PRIVACY_AND_ATTRIBUTION.md`.
- The English full estimate label is intentionally retained because it is an approved disclosure,
  not untranslated interface copy.
- Catalog cards no longer repeat official-variety counts or history-fetch implementation details.
- Home and detail surfaces no longer repeat estimate caveats already represented by the mandatory
  estimate disclosure.
- Chart surfaces no longer display persistent construction, conversion, or securities-comparison
  slogans. The factual K-line definition remains in point inspection, accessibility semantics, and
  the expandable `資料說明` section.
- Alert creation no longer repeats fixed-basis or estimate-method slogans. Its chosen basis,
  threshold unit, validation state, notification permission, and background-delay behavior remain
  visible where actionable.

## Generated images

Pass for all 121 exact release hashes recorded in
`release-tool/audits/2026-07-27-catalog-illustration-audit.json`. The files are unique,
transparent WebP assets at no more than 512 pixels and remain within both per-file and
complete-catalog package budgets. Any replacement or re-encoding invalidates that asset's approval
and requires a new review plus taxonomy checksum.

## Catalog and model evidence

- Catalog review evidence: `release-tool/audits/2026-07-14-historical-audit.md` and the checked-in
  checksummed catalog artifacts.
- The project owner approved the exhaustive catalog breadth, the five distinct `SX0`-`SX4` sprout
  identities, and all remaining exact illustration hashes by 2026-07-27.
- The published catalog contains 120 vegetable concepts plus one reviewed fruit concept. The
  bundled and release-tool taxonomy copies share checksum
  `2c17d98130a15671becd4aeb87d61f25e1ca631f46c334767ae0d7ab5867dbc0`.
- Mapping and image evidence is recorded in
  `release-tool/audits/2026-07-27-exhaustive-vegetable-review.md` and
  `release-tool/audits/2026-07-27-catalog-illustration-audit.json`.
- Model review evidence: `release-tool/audits/2026-07-26-model-audit.md`,
  `historical-audit-2026-07-26.json`, `mvp-model-artifact.json`, and
  `mvp-temporary-factor-artifact.json`.
- The model artifact validates and regenerates byte-for-byte deterministically.
- Calibrated model families remain disabled: zero eligible entries and 15 explicit exclusions
  because the 2025-12-01 calibration cutoff is too old on 2026-07-26.
- The owner-approved temporary reference is independently checksummed and uses exactly
  `(wholesale NTD/kg × 0.6 kg/台斤) × 2.0`, a 36-hour wholesale freshness gate, same-date
  volume weighting for the combined basis, and explicit rough-estimate disclosure.
- Interval and confidence output are disabled.

## Room migration

- Exported Room schemas exist for versions 1, 2, and 3.
- `MIGRATION_1_2` has a migration-and-validation instrumentation test.
- `MIGRATION_2_3` preserves prior estimates while adding estimator approval date and formula
  provenance; it also has a migration-and-validation instrumentation test.
- Destructive fallback is prohibited by `data/MIGRATION_POLICY.md`.
- The complete 14-test data instrumentation suite passed on both representative AVD profiles.
- A real in-place `adb install -r` update preserved the tracked `vegetable.moa.lp2` row. A startup
  taxonomy-import defect that previously cascaded away tracked rows was fixed by stable-ID upsert
  plus child-table replacement, and is covered by an idempotent re-import regression test.

## Dependency and security review

Static result: pass for the shipped Android runtime graph; build-tool findings require routine
toolchain upgrade tracking.

- Dependency versions are centralized and every configuration is locked.
- CI uses Gradle strict dependency verification with checked-in SHA-256 metadata.
- Repositories are restricted to Google Maven, Maven Central, and the Gradle Plugin Portal.
- No advertising, analytics, account, payment, WebView, location, contacts, media, SMS, call-log,
  or device-identifier SDK is present.
- Manifest permissions are limited to `INTERNET` and runtime `POST_NOTIFICATIONS`.
- Cleartext traffic and Android backup are disabled.
- The only exported component is the launcher/deep-link activity; the custom `veges` URI is scoped
  to host `produce`.
- Notification pending intents are immutable.
- Network requests use HTTPS, bounded timeouts, capped retries, validation, and atomic publication.
- Release logging rules prohibit payloads and local preferences.

OSV evidence:

- Owner-authorized official OSV batch query completed at `2026-07-26T12:28:58Z`.
- 421 unique locked Maven coordinates were queried; OSV returned 77 advisory/component matches
  across 18 exact coordinates.
- Matches are limited to Protobuf 3.24.4, Netty 4.1.93/4.1.110, OpenTelemetry API 1.41.0, and
  Bouncy Castle 1.80/1.80.2 coordinates used by the build/test toolchain.
- Explicit dependency-graph checks across `app`, all Android feature/library modules, `domain`,
  and `release-tool` found none of those coordinates on any product runtime classpath. They are
  not packaged into the APK. Re-run OSV after Gradle/AGP/Kotlin toolchain upgrades.

## Google Play policy preflight

Code/package result: pass. Play Console declarations remain pending.

- Data safety: expected declaration is no user data collected or shared. Government data is fetched
  directly; tracking, alerts, estimates, and notification history remain on-device. Reconfirm this
  against the final dependency graph.
- Privacy policy: content exists in `PRIVACY_AND_ATTRIBUTION.md`; publishing requires a stable,
  public HTTPS URL in both Play Console and the store listing.
- Government information: sources are named and linked, and the app explicitly states it is
  independent and does not represent a government entity. The Government apps declaration must be
  completed as unaffiliated in Play Console.
- Notifications: denial is supported without disabling tracking; pending events remain local.
- AI-generated content: the app cannot generate content. It only bundles 121 reviewed
  illustrations and visibly discloses them, so the generative-AI-app reporting requirement is not
  applicable to current functionality.
- No ads, billing, UGC, accounts, health claims, financial products, gambling, or child-directed
  features are present.
- Store title, description, icon, screenshots, content rating, app access, target-audience,
  government-app, and data-safety declarations require owner submission and must match the reviewed
  behavior.

## Closed-device and performance matrix

Result: pass.

- Low profile: API 34, Pixel 2, 2 cores, 2,048 MiB, 1080 × 1920, software GPU.
- Mid profile: API 34, Pixel 5, 4 cores, 4,096 MiB, 1080 × 2400, software GPU.
- App instrumentation: 8/8 on each profile. Data instrumentation: 14/14 on each profile.
- Online initial catalog, manual incremental refresh, cached offline market browsing, closure
  fixtures, denied notification permission, 200% font scale, and in-place update preservation all
  passed.
- The market visibly contains all 120 approved vegetables with individual transparent images;
  the reviewed fruit remains separately browsable.
- Low profile: 1.857-second median cold launch, 2.670-second end-to-end `LP2` search, 19.74-second
  incremental sync, approximately 9.6 MiB retained database, 111,752 KiB PSS.
- Mid profile: 0.832–0.969-second cold launches, 1.175-second end-to-end `LP2` search,
  9,192 KiB database, 122,086 KiB PSS.
- Mid chart scroll: 91 frames, 4.40% deadline-janky, p50 9 ms, p90 24 ms. The constrained low
  software-GPU profile recorded 36.71% deadline-janky and p90 53 ms, within its documented bound.
- Sync, network, storage, memory, chart, and physical-device battery bounds are accepted in
  `data/SYNC_BENCHMARK.md`.

## Release decision

The implementation and unsigned-candidate gates pass.

- On 2026-07-28, the final strict matrix ran
  `verifyFormatting staticAnalysis build :app:compileDebugAndroidTestKotlin
  :data:compileDebugAndroidTestKotlin :app:validateReleaseArtifact :app:bundleRelease` with strict
  dependency verification. All 779 Gradle tasks completed successfully, including all module unit
  tests, lint, Android test compilation, R8/resource shrinking, APK/AAB packaging, and the custom
  release artifact validator.
- The immediately preceding cache-independent clean matrix also passed 780 tasks.
- Unsigned release APK: 6,627,196 bytes; SHA-256
  `596554590ABDF81A7B12926D7DF305A580A9B19E42696B4574F3330E624A0051`.
- Release AAB: 9,774,605 bytes; SHA-256
  `B8B5A371CE5E5B97A92BC751C8AE9B3046EE8349A632279EFF2183475B18A98A`.

The remaining actions are intentionally owner-controlled rather than implementation defects:

1. Supply or select the release signing identity / Play App Signing workflow.
2. Publish `PRIVACY_AND_ATTRIBUTION.md` at a stable public HTTPS URL.
3. Complete the Play Console store listing, content rating, target audience, unaffiliated
   government-app declaration, and Data safety declaration.
4. Choose closed-track rollout ownership and staged-rollout percentage.

Do not upload the unsigned artifacts. Produce and record the signed MVP candidate only after these
owner-controlled values are supplied.
