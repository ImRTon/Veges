package tw.taipei.veges.data.local

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.PriceUnit
import tw.taipei.veges.domain.SourceDayState
import tw.taipei.veges.domain.SourceKind
import tw.taipei.veges.domain.UnavailableReason

@Entity(
    tableName = "taxonomy_concepts",
    indices = [
        Index(value = ["category", "published"]),
        Index(value = ["taxonomyVersion"]),
    ],
)
data class TaxonomyConceptEntity(
    @PrimaryKey val stableId: String,
    val householdName: String,
    val normalizedHouseholdName: String,
    val category: ProduceCategory,
    val published: Boolean,
    val illustrationAsset: String?,
    val illustrationDisclosure: String,
    val taxonomyVersion: String,
    val artifactChecksum: String,
    val reviewedAt: Instant,
    val reviewer: String,
)

@Entity(
    tableName = "taxonomy_aliases",
    primaryKeys = ["conceptId", "normalizedAlias"],
    foreignKeys = [
        ForeignKey(
            entity = TaxonomyConceptEntity::class,
            parentColumns = ["stableId"],
            childColumns = ["conceptId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["normalizedAlias"])],
)
data class TaxonomyAliasEntity(
    val conceptId: String,
    val normalizedAlias: String,
    val displayAlias: String,
)

@Entity(
    tableName = "official_variants",
    primaryKeys = ["conceptId", "commodityCode", "market"],
    foreignKeys = [
        ForeignKey(
            entity = TaxonomyConceptEntity::class,
            parentColumns = ["stableId"],
            childColumns = ["conceptId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["commodityCode", "market"])],
)
data class OfficialVariantEntity(
    val conceptId: String,
    val commodityCode: String,
    val officialName: String,
    val market: MarketBasis,
)

@Entity(
    tableName = "source_observations",
    indices = [
        Index(
            value = ["sourceKind", "market", "commodityCode", "observedOn"],
            unique = true,
        ),
        Index(value = ["commodityCode", "observedOn"]),
        Index(value = ["market", "observedOn"]),
    ],
)
data class SourceObservationEntity(
    @PrimaryKey val observationId: String,
    val sourceKind: SourceKind,
    val market: MarketBasis,
    val commodityCode: String,
    val officialName: String,
    val observedOn: LocalDate,
    val lowerPrice: BigDecimal?,
    val averagePrice: BigDecimal?,
    val upperPrice: BigDecimal?,
    val priceUnit: PriceUnit?,
    val volume: BigDecimal?,
    val volumeUnit: String?,
    val state: SourceDayState,
    val sourceUrl: String?,
    val attribution: String,
    val retrievedAt: Instant,
    val syncRunId: String,
)

@Entity(
    tableName = "source_day_states",
    indices = [
        Index(value = ["sourceKind", "market", "observedOn"], unique = true),
        Index(value = ["state", "observedOn"]),
    ],
)
data class SourceDayStateEntity(
    @PrimaryKey val stateId: String,
    val sourceKind: SourceKind,
    val market: MarketBasis,
    val observedOn: LocalDate,
    val state: SourceDayState,
    val diagnostic: String?,
    val latestValidObservationOn: LocalDate?,
    val updatedAt: Instant,
    val syncRunId: String?,
)

@Entity(
    tableName = "sync_runs",
    indices = [
        Index(value = ["sourceKind", "startedAt"]),
        Index(value = ["status", "startedAt"]),
    ],
)
data class SyncRunEntity(
    @PrimaryKey val runId: String,
    val sourceKind: SourceKind,
    val startedAt: Instant,
    val completedAt: Instant?,
    val status: SourceDayState,
    val requestedFrom: LocalDate?,
    val requestedTo: LocalDate?,
    val pagesFetched: Int,
    val recordsAccepted: Int,
    val diagnostic: String?,
)

@Entity(
    tableName = "model_metadata",
    indices = [Index(value = ["conceptId", "basis", "modelVersion"], unique = true)],
)
data class ModelMetadataEntity(
    @PrimaryKey val artifactId: String,
    val conceptId: String,
    val basis: MarketBasis,
    val modelVersion: String,
    val artifactChecksum: String,
    val calibrationCutoff: LocalDate,
    val pointEligible: Boolean,
    val intervalEligible: Boolean,
    val confidenceEligible: Boolean,
    val exclusionReason: String?,
    val importedAt: Instant,
)

@Entity(
    tableName = "estimates",
    indices = [
        Index(value = ["conceptId", "basis", "sourceDate"]),
        Index(value = ["conceptId", "calculatedAt"]),
        Index(value = ["modelVersion", "calibrationCutoff"]),
    ],
)
data class EstimateEntity(
    @PrimaryKey val estimateId: String,
    val conceptId: String,
    val basis: MarketBasis,
    val modelVersion: String,
    val sourceDate: LocalDate,
    val sourceDatesJson: String,
    val calibrationCutoff: LocalDate,
    val pairedCalibrationPeriods: Int,
    val calculatedAt: Instant,
    val pointValue: BigDecimal?,
    val pointUnit: PriceUnit?,
    val intervalLower: BigDecimal?,
    val intervalUpper: BigDecimal?,
    val confidence: BigDecimal?,
    val unavailableReason: UnavailableReason?,
    val disclosureShortTag: String,
    val disclosureFullLabel: String,
)

@Entity(
    tableName = "tracked_concepts",
    primaryKeys = ["conceptId"],
    foreignKeys = [
        ForeignKey(
            entity = TaxonomyConceptEntity::class,
            parentColumns = ["stableId"],
            childColumns = ["conceptId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["updatedAt"])],
)
data class TrackedConceptEntity(
    val conceptId: String,
    val trackedAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "alert_rules",
    indices = [
        Index(value = ["conceptId", "enabled"]),
        Index(value = ["basis", "enabled"]),
    ],
)
data class AlertRuleEntity(
    @PrimaryKey val ruleId: String,
    val conceptId: String,
    val basis: MarketBasis,
    val thresholdNtdPerTaiJin: BigDecimal,
    val enabled: Boolean,
    val conditionMet: Boolean,
    val lastEvaluatedSourceDate: LocalDate?,
    val lastTransitionEstimateId: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

@Entity(
    tableName = "notification_events",
    indices = [
        Index(value = ["ruleId", "sourceDate"]),
        Index(value = ["estimateId"]),
        Index(value = ["eventIdentity"], unique = true),
    ],
)
data class NotificationEventEntity(
    @PrimaryKey val eventId: String,
    val eventIdentity: String,
    val ruleId: String,
    val estimateId: String,
    val conceptId: String,
    val basis: MarketBasis,
    val sourceDate: LocalDate,
    val createdAt: Instant,
    val deliveredAt: Instant?,
)
