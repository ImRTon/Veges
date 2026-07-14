## ADDED Requirements

### Requirement: Tracked-produce home
The system SHALL provide a Material 3 home screen centered on tracked produce, showing the selected estimate basis, latest qualified value, source date, change indicator, freshness, and alert summary where available.

#### Scenario: Tracked estimates are available
- **WHEN** the user opens home with tracked concepts and current qualified estimates
- **THEN** the screen presents a compact scannable list with NTD-per-`台斤` values and text or icons in addition to color for movement

#### Scenario: Tracked estimate is unavailable
- **WHEN** a tracked concept has no qualified current estimate
- **THEN** its row shows a specific unavailable or stale state rather than zero, a fabricated value, or an observed-retail claim

### Requirement: Search and browse experience
The system SHALL expose household-name search and separate visual fruit and vegetable browsing using Material 3 search, filtering, list, and bottom-sheet patterns.

#### Scenario: Search result is unambiguous
- **WHEN** one reviewed concept matches the submitted name or alias
- **THEN** the user can open its detail while seeing the household name first and official identity as context

#### Scenario: Search result is ambiguous
- **WHEN** multiple reviewed concepts match an ambiguous alias
- **THEN** a choice sheet presents the alternatives and requires an explicit selection

### Requirement: Produce detail experience
The system SHALL provide a detail screen with household and official identity, illustrative image disclosure, qualified estimate state, source controls, provenance dates, tracking, alert actions, and 7D/30D/90D/1Y trend ranges.

#### Scenario: User changes source basis
- **WHEN** the user selects an eligible Taipei First or Taipei Second basis
- **THEN** the detail updates all value, trend, date, and methodology labels consistently for that basis

#### Scenario: Selected basis is ineligible
- **WHEN** a basis has no qualified model for the concept
- **THEN** the source control identifies it as unavailable and the screen does not reuse another basis's value

### Requirement: Appropriate trend visualization
The system SHALL visualize wholesale upper, middle, lower, average, and volume data using range-and-average trends and SHALL NOT render conventional stock candlesticks or label wholesale series as retail observations.

#### Scenario: User inspects a chart point
- **WHEN** the user focuses or taps a trend point
- **THEN** the screen exposes its date, series type, unit, source basis, value or range, and volume where applicable

#### Scenario: User cannot perceive chart graphics
- **WHEN** accessibility services focus the trend or color differentiation is unavailable
- **THEN** an equivalent textual summary and semantic values communicate direction, period, extrema, and latest value

### Requirement: Estimate and source disclosure
The system SHALL label model output with the visible `估算` tag and full label `Taipei retail reference estimate`, distinguish wholesale input from historical retail calibration, and expose methodology and freshness without describing the estimate as an observed, lowest, store, or guaranteed price.

#### Scenario: Estimate appears in any feature
- **WHEN** a point estimate is shown on home, search, detail, alert editing, or a notification
- **THEN** the `估算` tag and full estimated-retail context identify it as an estimated retail reference and provide access to basis and source date

#### Scenario: User opens methodology
- **WHEN** the user requests data-basis details
- **THEN** the system explains the selected market basis, contributing official codes, combination method, model version, wholesale source date, retail calibration cutoff, and limitations

#### Scenario: Estimate is rendered for accessibility services
- **WHEN** a screen reader or notification reader encounters a model-derived price
- **THEN** its accessible label includes `Taipei retail reference estimate`, the `估算` meaning, basis, and source date

### Requirement: Explicit asynchronous states
The system SHALL render distinguishable initial-loading, refreshing, current, stale, offline, unavailable, and failed-refresh states while preserving usable cached content.

#### Scenario: Refresh runs with cached content
- **WHEN** a synchronization is in progress and prior valid content exists
- **THEN** the screen keeps the content visible, indicates refreshing, and does not replace it with a full-screen loading state

#### Scenario: Initial load has no usable data
- **WHEN** no local data exists and synchronization fails
- **THEN** the screen explains the failure and offers a retry without showing zero prices or empty success content

### Requirement: Accessible adaptive UI
The system SHALL support phone screen sizes, portrait and landscape constraints, system font scaling, screen readers, minimum touch targets, and contrast-compliant light and dark Material 3 themes.

#### Scenario: Large font scale is enabled
- **WHEN** the app runs at the supported maximum accessibility font scale
- **THEN** essential names, prices, dates, controls, and disclosures remain readable and operable without clipping critical content

#### Scenario: Movement color cannot be distinguished
- **WHEN** a price increase or decrease is displayed
- **THEN** direction is also communicated by signed text, wording, iconography, or accessible semantics

### Requirement: Taiwan localization and units
The system SHALL present user-facing MVP content in Traditional Chinese appropriate for Taiwan and format prices in NTD per `台斤` with explicit dates.

#### Scenario: Price and source date are rendered
- **WHEN** a qualified estimate is available
- **THEN** the value uses locale-appropriate NTD formatting, the unit is `台斤`, and the source date is unambiguous
