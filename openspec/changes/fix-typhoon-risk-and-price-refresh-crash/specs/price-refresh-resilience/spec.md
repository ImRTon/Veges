## ADDED Requirements

### Requirement: Coalesced refresh scheduling
The system SHALL serialize price refresh work, coalesce repeated manual requests while work is active, and bound how frequently catalog-history backfill is requested.

#### Scenario: User requests refresh repeatedly
- **WHEN** the user requests another manual refresh while one-time refresh work is already active
- **THEN** WorkManager keeps the active serialized chain without appending duplicate manual refresh work

#### Scenario: Catalog history was requested recently
- **WHEN** another latest-price refresh requests catalog-history backfill less than 24 hours after the previous request
- **THEN** no additional catalog-history worker is enqueued

#### Scenario: Catalog history interval has elapsed
- **WHEN** at least 24 hours have elapsed since the previous catalog-history request
- **THEN** one catalog-history worker is appended to the serialized refresh queue

### Requirement: Change-aware downstream processing
The system SHALL recalculate estimates and evaluate alerts only when a published sync accepted source records or discovered source days.

#### Scenario: Source publishes no changes
- **WHEN** a refresh publishes zero accepted records and zero discovered source days
- **THEN** the worker skips estimate and alert recomputation, preserves cached prices, and still attempts delivery of previously pending notifications

#### Scenario: Source publishes changes
- **WHEN** a refresh publishes accepted records or discovers source days
- **THEN** the worker recalculates estimates and evaluates alerts before delivering pending notifications

### Requirement: Refresh failure containment
The system SHALL contain recoverable refresh failures within WorkManager and preserve the last valid local price state.

#### Scenario: Official source request fails
- **WHEN** a price refresh encounters a recoverable network or source error
- **THEN** the worker reports retry or failure according to its bounded policy without terminating the app process or deleting cached valid prices

#### Scenario: Market-history projection contains an incomplete row
- **WHEN** Room emits an incomplete market-history projection while refreshed data invalidates the observable query
- **THEN** the incomplete row is discarded, complete cached observations remain available, and the app process continues running
