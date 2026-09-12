## ADDED Requirements

### Requirement: Active production-area weather risk
The system SHALL derive an early market-wide supply risk when an active official weather warning names at least one configured agricultural county, without requiring wholesale prices or volumes to have already moved.

#### Scenario: Typhoon-driven rain affects agricultural areas
- **WHEN** an active heavy-rain warning names agricultural counties and its official description identifies a typhoon as the cause
- **THEN** the home screen shows a typhoon-related production-area risk naming the affected counties

#### Scenario: Agricultural weather risk precedes market movement
- **WHEN** a qualifying production-area warning is active but current price and volume history remains neutral
- **THEN** the early production-area risk is shown while the price-surge predictor continues to report no market-evidence surge

#### Scenario: Sea-only typhoon warning
- **WHEN** a typhoon warning names only sea areas and no related land warning affects a configured agricultural county
- **THEN** the system does not claim that agricultural production areas are affected

#### Scenario: Expired production-area warning
- **WHEN** a warning is outside its effective interval
- **THEN** it does not produce a current production-area risk

### Requirement: Evidence-preserving warning classification
The system SHALL preserve the observed warning kind and SHALL attribute it to a typhoon only when the official warning content explicitly identifies a typhoon as its cause.

#### Scenario: Heavy rain explicitly caused by typhoon
- **WHEN** a heavy-rain CAP description contains an explicit typhoon reference
- **THEN** the parsed signal remains a heavy-rain warning and records typhoon as its cause

#### Scenario: Heavy rain without typhoon attribution
- **WHEN** a heavy-rain CAP description does not identify a typhoon
- **THEN** the parsed signal is presented as heavy-rain risk without typhoon attribution

### Requirement: Concise radar presentation
The home screen SHALL present production-area weather risk as concise natural Traditional Chinese inside the existing typhoon price-surge radar and SHALL use the radar's existing animation instead of a separate colored banner.

#### Scenario: Several counties are affected
- **WHEN** more affected agricultural counties exist than fit in the concise summary
- **THEN** the radar text names a bounded set of counties and states that additional production areas are also affected

#### Scenario: Production-area signal is active
- **WHEN** a qualifying production-area signal is shown
- **THEN** the radar includes the cause, affected-area summary, and possible price consequence without a disclaimer-style explanation

#### Scenario: Accessibility service reads the risk
- **WHEN** an accessibility service focuses the production-area risk card
- **THEN** the radar semantics identify the cause, affected production areas, and possible consequence without relying on color alone

### Requirement: Weather attribution and applicability
The system SHALL use the recorded cause for user-facing weather attribution and SHALL only use warnings that identify a recognized agricultural county.

#### Scenario: Typhoon-driven heavy rain supports market context
- **WHEN** a heavy-rain warning identifies a typhoon as its cause and names a recognized agricultural county
- **THEN** the market context attributes the warning to the typhoon while retaining heavy rain as the observed source hazard

#### Scenario: Warning has no usable affected area
- **WHEN** a warning has an empty area set or names no recognized agricultural county
- **THEN** it does not contribute a weather reason or price projection

### Requirement: No unsupported item-level weather attribution
The system SHALL NOT add a weather reason, score, or projected percentage to a single produce item until a reviewed mapping connects that item to an affected production area.

#### Scenario: Agricultural county warning exists without item origin mapping
- **WHEN** an official warning affects a configured agricultural county and a selected item has qualifying market history but no reviewed origin mapping
- **THEN** the item outlook is calculated only from its observed price and volume evidence

#### Scenario: Market-wide surge and production-area risk coexist
- **WHEN** observed price and volume evidence qualifies a market-wide surge while a production-area weather risk is active
- **THEN** weather may describe the concurrent market context but does not create affected items or increase item projection percentages

### Requirement: Explainable reason thresholds
The system SHALL include an input in a projected price change only when that input meets the same threshold used to display its reason.

#### Scenario: Input remains below its reason threshold
- **WHEN** price momentum, volume movement, or recent-normal deviation remains below its configured reason threshold
- **THEN** that input contributes neither a visible reason nor a hidden projected-percentage uplift

### Requirement: Production UI excludes internal previews
The production home screen SHALL show only current user-relevant radar states and SHALL not expose internal animation-preview controls or synthetic reason galleries.

#### Scenario: User views the price-surge radar
- **WHEN** the home screen renders any radar state
- **THEN** no animation-test button, test mode, or synthetic reason gallery is available
