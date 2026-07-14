# Release Logging Policy

Release builds may log only operational metadata:

- source identifier, request outcome, HTTP status class, retry count, and elapsed duration;
- sync run identifier, source date range, accepted/invalid counts, and freshness state;
- bundled taxonomy/model version and checksum validation result;
- notification scheduling outcome without user content.

Release builds must not log:

- full source payloads, CSV rows, response bodies, or authorization headers;
- household names tied to local tracking preferences;
- alert thresholds, notification text, or local preference values;
- raw model inputs or generated image payloads.
