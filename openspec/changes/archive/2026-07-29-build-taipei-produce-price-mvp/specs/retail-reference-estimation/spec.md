## ADDED Requirements

### Requirement: Reproducible temporary-factor approval
The system SHALL calculate the temporary MVP market reference only from a checksummed, versioned artifact that records the explicitly approved factor `2.0`, unit conversion, supported source bases, generation time, and approval metadata.

#### Scenario: Concept and source pass all gates
- **WHEN** a published concept has an exact reviewed mapping, the required positive wholesale input is valid and fresh, and the factor artifact passes integrity/version checks
- **THEN** the app converts NTD/kg to NTD/`台斤`, multiplies by exactly `2.0`, and records source dates, basis, artifact version, formula, and checksum

#### Scenario: Any required gate fails
- **WHEN** a concept/source pair has unreliable mappings, stale/invalid input, missing required combined-market input, or an invalid factor artifact
- **THEN** the artifact marks it ineligible with reasons and the app does not calculate a retail estimate for that pair

#### Scenario: Wholesale input exceeds freshness
- **WHEN** more than 36 hours have elapsed since the successful wholesale retrieval
- **THEN** the app keeps prior provenance visible but does not calculate a new temporary reference

### Requirement: No future leakage in validation
The release audit SHALL evaluate candidate estimators with time-ordered training and validation splits that prevent observations after a prediction date from influencing that prediction.

#### Scenario: Backtest is executed
- **WHEN** candidate models are compared for release
- **THEN** every validation prediction uses only inputs available as of that prediction date and the report includes per-period and aggregate metrics

#### Scenario: Named model family lacks family-specific evidence
- **WHEN** a model family is declared but the audit did not fit and backtest that family's actual estimator
- **THEN** the family remains ineligible and cannot inherit another family's validation metrics

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

### Requirement: Temporary multiplier reference
The system SHALL derive the temporary market-reference estimate as `(valid wholesale NTD/kg × 0.6 kg/台斤) × 2.0` and SHALL disclose that this is a rough fixed-factor estimate rather than an observed or calibrated retail price.

#### Scenario: Eligible point estimate is calculated
- **WHEN** valid mapped inputs and the approved factor artifact are available
- **THEN** the system produces a point estimate in NTD per `台斤` with concept, basis, estimator version, source date, factor-artifact approval date, formula, and calculation time

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
The system SHALL keep uncertainty ranges and confidence labels disabled for the MVP because the approved audit contains no interval or confidence calibration metrics. A future artifact SHALL show either output only after that specific output passes newly reviewed validation and calibration thresholds.

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

### Requirement: Future calibrated replacement remains separate
The system SHALL retain the historical calibration audit as non-runtime evidence and SHALL NOT enable any calibrated family, interval, or confidence output until a later change supplies current retail targets and approves its gates.

#### Scenario: Paired calibration history is shorter than 18 periods
- **WHEN** a calibrated family lacks current approved evidence
- **THEN** the temporary factor may remain available, but the calibrated family stays disabled and cannot inherit the factor artifact's approval

### Requirement: Auditable historical trends
The system SHALL retain estimate history and its provenance for the supported 7-day, 30-day, 90-day, and 1-year trend periods.

#### Scenario: Historical estimate is recalculated by a newer model
- **WHEN** the app displays model-derived history generated by a model newer than the observation date
- **THEN** the methodology identifies the estimator version, factor `2.0`, and factor-artifact approval date so the trend is not represented as a contemporaneously observed retail series
