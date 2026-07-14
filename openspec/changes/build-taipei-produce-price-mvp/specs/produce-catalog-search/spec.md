## ADDED Requirements

### Requirement: Versioned reviewed taxonomy
The system SHALL use a versioned, reviewed taxonomy containing stable concept identifiers, Traditional Chinese household names, fruit or vegetable category, aliases, illustration metadata, and exact official commodity mappings.

#### Scenario: Taxonomy artifact is imported
- **WHEN** the bundled taxonomy schema, checksum, mappings, and review metadata are valid
- **THEN** the system imports it idempotently while preserving stable concept identifiers used by tracking records

#### Scenario: Taxonomy artifact is invalid
- **WHEN** an artifact has an unsupported schema, failed checksum, duplicate identifier, dangling mapping, or missing required review data
- **THEN** the system rejects the artifact and does not expose partially imported concepts

### Requirement: Household-name search
The system SHALL search reviewed household names and aliases without using an LLM or guessing an unmapped official commodity at runtime.

#### Scenario: User searches a reviewed alias
- **WHEN** a normalized query matches a concept alias such as `高麗菜`
- **THEN** the system returns the matching household concept with its daily name shown before official names and codes

#### Scenario: Query has no reviewed mapping
- **WHEN** a query does not match any reviewed name or alias
- **THEN** the system reports no reviewed result and does not fabricate a mapping

### Requirement: Explicit ambiguity resolution
The system SHALL represent aliases that match multiple concepts as ambiguous and require the user to select a concept.

#### Scenario: User searches an ambiguous term
- **WHEN** a query such as `白菜` is mapped to more than one reviewed concept
- **THEN** the system presents all mapped choices with distinguishing names and illustrations and does not silently open one result

### Requirement: Visual category browsing
The system SHALL allow users to browse separately reviewed fruit and vegetable concepts using representative catalog illustrations.

#### Scenario: User selects a category
- **WHEN** the user browses the fruit or vegetable category
- **THEN** the system lists only published concepts in that category with household names and representative illustrations

### Requirement: Exact observation mapping
The system SHALL retain every price observation's official commodity code, official name, market, and date even when observations are aggregated under a household concept.

#### Scenario: Concept contains multiple official variants
- **WHEN** a household concept maps to multiple reviewed official commodity variants
- **THEN** concept-level calculations use only those explicit mappings and methodology details can identify the contributing variants

### Requirement: Audited catalog publication
The system SHALL publish a launch concept only when its identity, category, aliases, illustration review, and official mappings pass the versioned catalog audit.

#### Scenario: Candidate has an unreliable mapping
- **WHEN** a candidate produce concept lacks a reviewed unambiguous relationship to official commodities
- **THEN** the release audit excludes it from the published launch catalog and records the exclusion reason

### Requirement: Honest illustration labeling
The system SHALL label every generated catalog image `AI 生成示意圖，非實物照片。` and SHALL NOT describe it as evidence of actual origin, grade, certification, packaging, farm, store, or variety.

#### Scenario: Generated image is displayed
- **WHEN** a catalog illustration appears on any screen
- **THEN** the AI-generated illustrative-image disclosure is available with the image and to accessibility services
