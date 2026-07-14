## ADDED Requirements

### Requirement: Persistent tracked produce
The system SHALL let users track and untrack published produce concepts and SHALL preserve those choices locally across restarts and catalog updates that retain the same stable concept identifier.

#### Scenario: User tracks a concept
- **WHEN** the user selects Track on a published concept
- **THEN** the concept appears in the tracked-produce home list after app restart

#### Scenario: User untracks a concept
- **WHEN** the user removes a tracked concept
- **THEN** the concept no longer appears in the tracked list and the system requests confirmation before removing its active alert rules

### Requirement: Below-threshold alert rules
The system SHALL let users create, edit, enable, disable, and delete local rules that compare a concept's estimated retail reference price against a positive NTD-per-`台斤` below-threshold for a fixed source basis.

#### Scenario: User creates a valid rule
- **WHEN** the user selects a concept, an eligible source basis, and a positive below-threshold value
- **THEN** the system persists an enabled rule that explicitly names the estimated-retail metric and source basis

#### Scenario: Global source preference changes
- **WHEN** the user changes the source used by detail screens after an alert is created
- **THEN** the existing alert retains its original basis until the user edits that alert

### Requirement: Valid-new-estimate evaluation
The system SHALL evaluate alerts only after a newly sourced, valid, qualified estimate for the rule's exact concept and basis is persisted.

#### Scenario: New estimate is below threshold
- **WHEN** a valid estimate with a source date newer than the rule's last evaluation is below the configured threshold
- **THEN** the rule enters the met state and is eligible for one local notification

#### Scenario: Source data is unusable
- **WHEN** synchronization yields missing, invalid, zero-value, officially closed, stale-only, unqualified, or unchanged source data
- **THEN** the system does not transition or notify the rule from that data

### Requirement: Deduplication and rearming
The system SHALL notify once when a below-threshold rule transitions into the met state and SHALL rearm it only after a later valid estimate is no longer below the threshold.

#### Scenario: Condition remains met on another valid day
- **WHEN** a notified rule receives another newer valid estimate that remains below threshold
- **THEN** the system updates evaluation state without sending a duplicate transition notification

#### Scenario: Condition clears and is met again
- **WHEN** a later valid estimate clears the condition and a subsequent valid estimate falls below threshold
- **THEN** the system sends a new transition notification

### Requirement: Local best-effort notifications
The system SHALL evaluate and deliver alerts on device after foreground or background synchronization and SHALL disclose that Android may delay background updates.

#### Scenario: Background work runs successfully
- **WHEN** WorkManager synchronizes a new valid estimate and notification permission is granted
- **THEN** met-state transitions produce a local notification identifying the concept, estimate, threshold, basis, and source date

#### Scenario: Notification permission is denied
- **WHEN** a rule transitions to met without notification permission
- **THEN** tracking and evaluation continue, no notification is attempted, and the app exposes the permission state and remediation action

### Requirement: Transactional alert processing
The system SHALL persist estimate evaluation, transition state, and notification event identity transactionally so retries cannot create duplicate events.

#### Scenario: Worker retries after interruption
- **WHEN** alert processing resumes after interruption for an already recorded estimate and event identity
- **THEN** the system does not create a second notification event for the same rule transition
