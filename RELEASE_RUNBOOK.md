# Release and operations runbook

This runbook applies to the offline-first Taipei produce-price MVP. The app has no remote
configuration or proprietary backend, so catalog/model corrections ship as a new app artifact.

## 1. Release procedure

1. Confirm every OpenSpec task is complete and every review in
   `RELEASE_REVIEW_2026-07-28.md` is pass. A documented blocker is not a waived gate.
2. Confirm the catalog and model artifacts are reviewed, current, checksummed, and accepted by
   their import validators. A model artifact with zero eligible entries blocks an MVP that claims
   retail estimates.
3. Run with strict dependency verification:

   ```powershell
   $env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
   .\gradlew.bat --dependency-verification=strict verifyFormatting staticAnalysis test :app:validateReleaseArtifact
   ```

4. Compile and execute the Android instrumentation suite on the representative emulator and
   physical closed-test device. Do not substitute compilation for execution.
5. Run the online/offline/closure/notification-permission/update matrix and record device,
   Android version, build hash, results, startup, sync, storage, network, memory, and battery
   measurements.
6. Review the merged release manifest, dependency inventory, R8 output, APK/AAB contents, version
   code/name, privacy policy, store listing, screenshots, content rating, government-app
   declaration, and Data safety answers.
7. Sign only through the owner-approved keystore or Play App Signing workflow. Never place a
   keystore or password in the repository, Gradle files, shell history, or CI logs.
8. Verify the signed artifact signature and checksum, upload to the closed track, and use a staged
   rollout. Record artifact checksum, signing certificate fingerprint, commit, reviewer, and Play
   release ID.

## 2. Rollback

1. Stop rollout in Play Console when a correctness, source, migration, privacy, or crash gate
   fails.
2. Promote the last known-good Play artifact if Play permits it; otherwise ship a higher-version
   rollback build containing the last known-good code and artifacts.
3. Never use destructive Room fallback. A rollback build must open every schema already released
   to users or fail the release gate before upload.
4. Because catalog/model configuration is bundled, roll it back only in a new signed app artifact.
5. Preserve incident evidence: affected versions, checksums, source windows, failure counts,
   reproduction steps, and user-visible impact. Do not capture local tracking or alert values.

## 3. Data and model refresh

1. Download source snapshots from the reviewed official HTTPS endpoints. Record retrieval time,
   endpoint, response headers needed for provenance, filename, and SHA-256.
2. Do not reinterpret new nulls, sentinels, fields, market codes, units, or closure signals until a
   fixture and contract review approve the behavior.
3. Run `audit-history` using the reviewed taxonomy, retail mappings, publication policy, wholesale
   directory, complete retail directory, fixed generation instant, and a new output filename.
4. Review all named estimator families. Confirm no future leakage, the data cutoff, 18 paired
   periods, 90% coverage, 45-day generation recency, MAE ≤ NT$15/台斤, RMSE ≤ NT$22/台斤,
   36-hour wholesale freshness, and 62-day artifact lifetime.
5. Keep interval, confidence, and families without family-specific evidence disabled.
6. Run `generate-model-artifact`, `validate-model-artifact`, and
   `checksum-model-artifact`. Generate twice and require byte-identical files.
7. Update the human-readable audit, add/adjust CI expectations, rerun the complete matrix, and
   obtain model-owner approval. Never hand-edit the checksum or promote an excluded entry.

## 4. Taxonomy amendment

1. Create or amend a stable household concept ID; IDs are not reused.
2. Review Traditional Chinese household name, normalized aliases, ambiguity behavior, category,
   exact official commodity codes/names, and supported market bases.
3. Ambiguous aliases must map to every reviewed candidate and force explicit user choice.
4. Review any illustration for recognizability, botanical plausibility, and prohibited claims.
   Record the exact image SHA-256 and keep `AI 生成示意圖，非實物照片。`.
5. Run the taxonomy audit and checksum command. Review coverage, mappings, recency, exclusions, and
   source provenance before changing publication status.
6. Add migration/import tests when the artifact schema changes. Release as a new app version.

## 5. Source incident

1. Fail closed: preserve the last valid Room snapshot and show stale/offline/unavailable state.
   Do not coerce malformed records, closures, missing days, or zeros into valid observations.
2. Capture operational metadata allowed by `RELEASE_LOGGING_POLICY.md`; never retain full payloads,
   headers containing secrets, tracking preferences, thresholds, or notification text.
3. Reproduce against a redacted fixture, compare official schema/terms/attribution, and classify
   availability failure versus schema drift versus a newly documented closure signal.
4. Patch DTO/validation only after contract review. Add success, malformed, empty, closure, and
   atomicity regression tests.
5. If published estimates may be wrong, halt rollout and use the rollback procedure. Notify users
   in release notes when the impact is material.

## 6. Database migration

1. Export the current Room schema and design an explicit forward migration.
2. Preserve taxonomy, tracking, alerts, estimates, provenance, model metadata, notification
   outbox state, and sync history unless a reviewed requirement says otherwise.
3. Add a `MigrationTestHelper` test that creates the old schema, inserts representative data,
   migrates, validates the schema, and asserts data/constraints/indexes.
4. Test a real update install from the prior closed-track build. Check online/offline startup,
   WorkManager uniqueness, notifications, and rollback implications.
5. Never enable destructive migration fallback. If preservation cannot be demonstrated, block the
   release and propose a separate migration decision.
