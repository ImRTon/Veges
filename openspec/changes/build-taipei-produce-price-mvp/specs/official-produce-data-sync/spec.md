## ADDED Requirements

### Requirement: Official source ingestion
The system SHALL retrieve paginated wholesale observations for Taipei First and Taipei Second and Taipei historical retail observations directly from configured official HTTPS data sources.

#### Scenario: All source pages are available
- **WHEN** a synchronization begins and the official source returns multiple valid pages
- **THEN** the system retrieves every page and records the source, request time, observation dates, market, commodity code, original units, and source attribution

#### Scenario: A source is temporarily unavailable
- **WHEN** a source request times out or returns a retryable failure
- **THEN** the system retains previously validated data, records the failed attempt, and schedules only bounded retry work

### Requirement: Validated atomic persistence
The system SHALL validate external records before atomically making them available to repositories and SHALL make repeated ingestion idempotent.

#### Scenario: Repeated observation is synchronized
- **WHEN** the same source, market, commodity, and observation date is ingested more than once
- **THEN** the system stores one current observation without duplicating history

#### Scenario: A page contains an invalid record
- **WHEN** a fetched batch fails schema, date, market, unit, numeric-range, or referential-integrity validation
- **THEN** the system does not expose a partial replacement as current data and records a diagnostic failure state

### Requirement: Non-price data states
The system SHALL distinguish valid, officially closed, missing, invalid, and synchronization-failed source states and SHALL NOT interpret a zero-value or absent record as a zero price.

#### Scenario: Market closure is confirmed
- **WHEN** an official signal identifies a market/date as closed
- **THEN** the system records a closed state and preserves the latest prior valid observation date

#### Scenario: No official closure signal exists
- **WHEN** expected data is absent and no official source confirms closure
- **THEN** the system records the data as missing rather than closed or zero-priced

#### Scenario: Price or volume fields are unusable
- **WHEN** an observation contains zero or invalid values required by downstream estimation
- **THEN** the system marks the observation invalid and excludes it from estimation and alerts

### Requirement: Incremental on-device refresh
The system SHALL coordinate unique best-effort periodic refresh and foreground catch-up work, fetch only the required incremental range where supported, and preserve enough valid history for all offered trend periods.

#### Scenario: Periodic work is enqueued repeatedly
- **WHEN** app startup or configuration attempts to register background synchronization more than once
- **THEN** only one uniquely identified periodic synchronization schedule remains active

#### Scenario: Device reconnects after missed work
- **WHEN** the app returns to foreground with network access and source data is stale
- **THEN** the system requests a catch-up synchronization without waiting for the next periodic execution

### Requirement: Independent freshness and provenance
The system SHALL expose wholesale freshness, retail-calibration cutoff, last successful refresh, and last attempted refresh independently.

#### Scenario: Wholesale data is current but calibration is old
- **WHEN** the latest wholesale synchronization succeeds and the calibration artifact has an older cutoff date
- **THEN** consumers receive both dates and do not represent the calibration as equally current

#### Scenario: Latest refresh fails
- **WHEN** valid cached data exists and a later refresh attempt fails
- **THEN** consumers receive the cached source date together with stale and failed-refresh metadata

### Requirement: Offline-readable data
The system SHALL provide previously validated catalog and price data without network access after successful initialization.

#### Scenario: App starts offline after prior synchronization
- **WHEN** the device has no network connection and validated local data exists
- **THEN** repositories return the local data with offline and freshness state instead of replacing it with an empty result
