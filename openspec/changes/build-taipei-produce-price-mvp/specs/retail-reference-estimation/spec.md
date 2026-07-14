## ADDED Requirements

### Requirement: Reproducible eligibility audit
The system SHALL determine estimate eligibility per concept and source basis using a reproducible release audit with reviewed coverage, recency, sample-size, mapping-quality, and time-aware backtest thresholds.

#### Scenario: Concept and source pass all gates
- **WHEN** the audit metrics for a concept/source pair satisfy every committed threshold
- **THEN** the generated artifact marks that pair eligible and records inputs, data cutoff, thresholds, metrics, model version, and checksum

#### Scenario: Any required gate fails
- **WHEN** a concept/source pair has insufficient calibration, unreliable mappings, stale data, or unacceptable backtest performance
- **THEN** the artifact marks it ineligible with reasons and the app does not calculate a retail estimate for that pair

### Requirement: No future leakage in validation
The release audit SHALL evaluate candidate estimators with time-ordered training and validation splits that prevent observations after a prediction date from influencing that prediction.

#### Scenario: Backtest is executed
- **WHEN** candidate models are compared for release
- **THEN** every validation prediction uses only inputs available as of that prediction date and the report includes per-period and aggregate metrics

### Requirement: Supported source bases
The system SHALL support `Taipei combined` as the default basis and Taipei First and Taipei Second as advanced bases only where each basis has an eligible model.

#### Scenario: Combined estimate is requested
- **WHEN** valid same-date observations from both Taipei markets and an eligible combined model exist
- **THEN** the system volume-weights the market inputs using the documented method and evaluates the combined model

#### Scenario: One combined input is unavailable
- **WHEN** either required Taipei market lacks a valid same-date input
- **THEN** the system reports the combined estimate unavailable and does not silently substitute a single market

#### Scenario: Advanced basis is selected
- **WHEN** the user selects Taipei First or Taipei Second and that concept/source pair is eligible
- **THEN** the system evaluates and labels the estimate using that selected market basis

### Requirement: Calibrated retail estimate
The system SHALL derive retail reference estimates from valid official wholesale inputs and a known, versioned calibration model and SHALL NOT present a fixed unit conversion or markup as a retail estimate.

#### Scenario: Eligible point estimate is calculated
- **WHEN** valid mapped inputs and a supported eligible model version are available
- **THEN** the system produces a point estimate in NTD per `台斤` with concept, basis, model version, source date, calibration cutoff, and calculation time

#### Scenario: Model version is unsupported
- **WHEN** an artifact references a model evaluator version unknown to the installed app
- **THEN** the system fails closed with an unavailable reason instead of approximating the estimate

### Requirement: Mandatory estimate disclosure tag
The system SHALL attach the visible `估算` tag and the full label `Taipei retail reference estimate` to every model-derived price, including values shown in history, alerts, and notifications.

#### Scenario: Model-derived price is displayed
- **WHEN** a retail reference estimate is shown in any feature or notification
- **THEN** the estimate retains the `估算` tag and is not presented as an observed wholesale or store price

#### Scenario: Valid wholesale price is displayed without an estimate
- **WHEN** a valid wholesale observation is available but the estimate eligibility gate has not passed
- **THEN** the system may show the wholesale observation as wholesale data without applying the retail-estimate tag or claiming a retail value

### Requirement: Separately gated uncertainty output
The system SHALL show an uncertainty range or confidence label only when that specific output passes its committed validation and calibration thresholds.

#### Scenario: Point estimate passes but interval does not
- **WHEN** point-estimate eligibility passes and interval validation fails
- **THEN** the system shows the qualified point estimate without an estimated range or confidence label

#### Scenario: All output gates pass
- **WHEN** point, interval, and confidence validation gates pass
- **THEN** the system returns each output with the model metadata required to explain it

### Requirement: Invalid and stale input handling
The system SHALL exclude invalid, missing, officially closed, zero-value, and unmapped observations from new estimates and SHALL identify when a displayed latest valid estimate is stale.

#### Scenario: Current source day is closed
- **WHEN** no new valid estimate can be produced because the market is officially closed
- **THEN** the system retains the latest valid estimate, shows its actual source date and closure/freshness state, and does not create an estimate dated as the closed day

#### Scenario: Reliable commodity mapping is absent
- **WHEN** a concept lacks an eligible reviewed mapping for a source observation
- **THEN** no estimate is calculated from that observation

### Requirement: Minimum calibration history
The system SHALL require at least 30 paired periods with both a valid mapped wholesale input and a valid historical retail calibration target for a concept/source pair before calculating a retail reference estimate; coverage, recency, and model-quality thresholds remain independently configurable release gates.

#### Scenario: Paired calibration history is shorter than 30 periods
- **WHEN** a concept/source pair has fewer than 30 periods containing both valid wholesale and historical retail calibration data
- **THEN** the system keeps valid wholesale data available but marks the retail reference estimate unavailable for insufficient calibration history

### Requirement: Auditable historical trends
The system SHALL retain estimate history and its provenance for the supported 7-day, 30-day, 90-day, and 1-year trend periods.

#### Scenario: Historical estimate is recalculated by a newer model
- **WHEN** the app displays model-derived history generated by a model newer than the observation date
- **THEN** the methodology identifies the model version and calibration cutoff so the trend is not represented as a contemporaneously observed retail series
