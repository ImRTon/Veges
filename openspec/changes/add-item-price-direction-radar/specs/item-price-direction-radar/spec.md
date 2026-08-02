## ADDED Requirements

### Requirement: Basis-specific item direction evaluation
The system SHALL evaluate the selected produce concept using valid wholesale price and volume observations for the selected Taipei market basis, independently of the chart period selected by the user.

#### Scenario: User changes chart period
- **WHEN** the user changes the detail chart between 7, 30, 90, or 365 days without changing market basis or source data
- **THEN** the item direction evaluation remains based on its fixed recent trading-day window

#### Scenario: User changes market basis
- **WHEN** the user selects a different Taipei market basis
- **THEN** the item direction evaluation is recalculated from observations aggregated for that basis

### Requirement: Explainable rising and falling signals
The system SHALL evaluate rising and falling evidence separately and SHALL return a directional outlook only when market evidence meets the configured magnitude, strength, freshness, and history thresholds.

#### Scenario: Rising market evidence
- **WHEN** recent prices strengthen and supporting evidence such as volume contraction, an above-baseline anomaly, or an active agricultural weather warning produces a qualifying rising score
- **THEN** the evaluation reports a rising signal with signed projected change, strength score, and plain-language reasons

#### Scenario: Falling market evidence
- **WHEN** recent prices weaken and supporting evidence such as volume expansion or a below-baseline anomaly produces a qualifying falling score
- **THEN** the evaluation reports a falling signal with signed projected change, strength score, and plain-language reasons

#### Scenario: Weak or conflicting evidence
- **WHEN** neither rising nor falling evidence clearly meets the configured thresholds
- **THEN** the evaluation reports no material direction instead of forcing an up or down forecast

### Requirement: Qualified weather contribution
The system SHALL treat active official weather warnings as supporting evidence for rising supply pressure and SHALL require independent market evidence before producing a directional signal.

#### Scenario: Weather warning without market evidence
- **WHEN** an active weather warning exists but the selected item's price and volume history provides no qualifying directional evidence
- **THEN** the evaluation does not produce a rising signal solely from the warning

#### Scenario: Expired warning
- **WHEN** a weather warning is outside its effective interval
- **THEN** it does not contribute to the item evaluation

### Requirement: Explicit data availability states
The system SHALL distinguish insufficient history, stale source data, and eligible data with no material signal.

#### Scenario: Insufficient history
- **WHEN** fewer than ten valid trading days are available
- **THEN** the detail radar reports that more history is required and includes the current valid-day count

#### Scenario: Stale history
- **WHEN** the newest valid observation is older than four days
- **THEN** the detail radar reports that market data needs updating and does not show a directional outlook

#### Scenario: Eligible neutral history
- **WHEN** at least ten fresh valid trading days are available but no direction qualifies
- **THEN** the detail radar reports that no clear 7–14 day signal is present

### Requirement: Accessible item radar presentation
The detail screen SHALL present the selected item's 7–14 day evaluation in a Material 3 card using text, iconography, and color, and SHALL not rely on color alone.

#### Scenario: Directional signal presentation
- **WHEN** a rising or falling signal is available
- **THEN** the card shows direction text, signed projected magnitude, strength score, source date, and analyzed-day count, and only shows secondary evidence labels that do not duplicate the primary reason

#### Scenario: Non-directional presentation
- **WHEN** the evaluation is neutral, stale, or insufficient
- **THEN** the card shows a specific plain-language state and next-step explanation without presenting a forecast percentage

#### Scenario: Accessibility service reads the card
- **WHEN** an accessibility service focuses the item radar
- **THEN** the semantic description states the item name, horizon, direction or availability state, and evidence summary
