## ADDED Requirements

### Requirement: Versioned reviewed taxonomy
The system SHALL use a versioned, reviewed taxonomy containing stable concept identifiers, Traditional Chinese household or official produce names, fruit or vegetable category, aliases, illustration metadata, and exact official commodity mappings. The vegetable taxonomy SHALL include every official `N04` variety code observed in Taipei First or Taipei Second during the declared release-audit window. Variants MAY share a concept only when their official names have the same exact base text before the first hyphen and reviewed semantics confirm the suffix is a non-identity modifier rather than a distinct edible household produce.

#### Scenario: Taxonomy artifact is imported
- **WHEN** the bundled taxonomy schema, checksum, mappings, and review metadata are valid
- **THEN** the system imports it idempotently while preserving stable concept identifiers used by tracking records

#### Scenario: Taxonomy artifact is invalid
- **WHEN** an artifact has an unsupported schema, failed checksum, duplicate identifier, dangling mapping, or missing required review data
- **THEN** the system rejects the artifact and does not expose partially imported concepts

#### Scenario: Official vegetable variety has sparse price history
- **WHEN** an `N04` variety is present in the audited Taipei market catalog but has fewer than the estimation or trend history minimum
- **THEN** the system still publishes it for browse and search with its exact official identity and reports `行情累積中` instead of excluding the variety

#### Scenario: Shared official base names distinct edible produce
- **WHEN** official codes `SX0` through `SX4` share the `芽菜類` base but identify other sprouts, mung bean sprouts, soybean sprouts, pea shoots, and alfalfa sprouts
- **THEN** the reviewed taxonomy publishes five separate stable concepts with independent search, tracking, history, and illustration identities

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
The system SHALL allow users to browse the complete release-audited Taipei vegetable produce catalog and the reviewed fruit catalog using one distinct transparent-background catalog illustration per produce concept, visibly rendered from verified bundled assets rather than replaced by text initials. Concepts SHALL NOT share illustration files or use an atlas. The installed app SHALL bundle only optimized transparent WebP assets whose longest edge is at most 512 pixels, whose individual encoded size is at most 128 KiB, and whose complete catalog illustration set is at most 6 MiB; high-resolution generation sources SHALL remain outside packaged app assets.

#### Scenario: User selects a category
- **WHEN** the user browses the fruit or vegetable category
- **THEN** the system lists only published concepts in that category with household names and representative illustrations

#### Scenario: User browses the complete vegetable catalog
- **WHEN** the user selects vegetables without entering a query
- **THEN** every audited `N04` Taipei variety is reachable through its produce concept in a performant grouped or lazy-rendered list, with concise household-first cards and complete official variety names and codes available in detail without persistent variant-count or history-fetch slogans

#### Scenario: Approved illustration asset is available
- **WHEN** a published concept card or detail header is rendered
- **THEN** the system displays the approved bundled illustration with the AI-image disclosure and accessible household-name description

#### Scenario: Illustration identity is audited
- **WHEN** the release audit checks a published catalog
- **THEN** every produce concept references a unique transparent-background image hash and no concept references an atlas, another concept's image, a logo, packaging, or a text-initial fallback

#### Scenario: Illustration package budget is audited
- **WHEN** the release audit validates bundled Android catalog assets
- **THEN** every referenced image is an at-most-512-pixel transparent WebP no larger than 128 KiB, the complete referenced set is no larger than 6 MiB, and no high-resolution or chroma-key source is packaged

### Requirement: Exact observation mapping
The system SHALL retain every price observation's official commodity code, official name, market, and date even when observations are aggregated under a household concept.

#### Scenario: Concept contains multiple official variants
- **WHEN** a household concept maps to multiple reviewed official commodity variants
- **THEN** concept-level calculations use only those explicit mappings and methodology details can identify the contributing variants

### Requirement: Audited catalog publication
The system SHALL publish a launch concept only when its identity, category, aliases, illustration review, and official mappings pass the versioned catalog audit. A 30-valid-day threshold SHALL govern price-history or estimate eligibility, not whether an official vegetable variety is browseable.

#### Scenario: Candidate has an unreliable mapping
- **WHEN** a candidate produce concept lacks a reviewed unambiguous relationship to official commodities
- **THEN** the release audit excludes it from the published launch catalog and records the exclusion reason

### Requirement: Honest illustration labeling
The system SHALL label every generated catalog image `AI 生成示意圖，非實物照片。` and SHALL NOT describe it as evidence of actual origin, grade, certification, packaging, farm, store, or variety.

#### Scenario: Generated image is displayed
- **WHEN** a catalog illustration appears on any screen
- **THEN** the AI-generated illustrative-image disclosure is available with the image and to accessibility services
