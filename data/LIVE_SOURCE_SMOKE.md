# Live Source Smoke Diagnostics

Live-source diagnostics are non-blocking. CI must never depend on upstream availability.

## Manual Operator Run

1. Run the release-tool source contract tests against committed fixtures.
2. Run a bounded live request for MOA crop metadata and one daily wholesale snapshot.
3. Verify HTTPS, expected content type, required field names, date parse, market identifiers `104`
   and `109`, positive numeric fields, and attribution metadata.
4. Download one Taipei retail CSV resource and verify the five-column header and dash-as-missing
   behavior.
5. Compare the observed schema and source dates with `data/SOURCE_CONTRACT_REVIEW.md`.
6. Record endpoint changes, closure-signal changes, resource IDs, and checksums in the release audit.

## Failure Handling

- A failed diagnostic creates an incident record and does not publish a new app/model artifact.
- Unknown fields or changed required fields fail closed until fixtures and adapters are reviewed.
- Missing rows are not converted to market closure without an official closure signal.
