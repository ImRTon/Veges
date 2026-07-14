package tw.taipei.veges.domain

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@JvmInline
value class ProduceConceptId(val value: String)

@JvmInline
value class OfficialCommodityCode(val value: String)

enum class ProduceCategory {
    FRUIT,
    VEGETABLE,
}

enum class MarketBasis {
    TAIPEI_COMBINED,
    TAIPEI_FIRST,
    TAIPEI_SECOND,
}

enum class PriceUnit {
    NTD_PER_TAI_JIN,
    NTD_PER_KILOGRAM,
    NTD_PER_PIECE,
}

enum class SourceKind {
    MOA_WHOLESALE,
    TAIPEI_RETAIL_HISTORY,
    BUNDLED_TAXONOMY,
    BUNDLED_CALIBRATION,
}

enum class Freshness {
    CURRENT,
    STALE,
    OFFLINE,
    UNKNOWN,
}

enum class SourceDayState {
    VALID,
    CLOSED,
    MISSING,
    INVALID,
    FAILED,
}

enum class UnavailableReason {
    NO_REVIEWED_MAPPING,
    MISSING_SOURCE_DATA,
    SOURCE_CLOSED,
    INVALID_SOURCE_DATA,
    FAILED_REFRESH,
    STALE_INPUT,
    UNSUPPORTED_MODEL,
    MODEL_INELIGIBLE,
    ARTIFACT_INVALID,
}

data class ScaledPrice(
    val amount: BigDecimal,
    val unit: PriceUnit,
)

data class Provenance(
    val source: SourceKind,
    val attribution: String,
    val sourceUrl: String?,
    val retrievedAt: Instant,
)

data class OfficialVariant(
    val code: OfficialCommodityCode,
    val officialName: String,
    val market: MarketBasis,
)

data class ProduceConcept(
    val id: ProduceConceptId,
    val householdName: String,
    val aliases: List<String>,
    val category: ProduceCategory,
    val published: Boolean,
    val illustrationAsset: String?,
    val officialVariants: List<OfficialVariant>,
)

data class SourceObservation(
    val source: SourceKind,
    val market: MarketBasis,
    val commodityCode: OfficialCommodityCode,
    val officialName: String,
    val observedOn: LocalDate,
    val lowerPrice: ScaledPrice?,
    val averagePrice: ScaledPrice?,
    val upperPrice: ScaledPrice?,
    val volume: BigDecimal?,
    val state: SourceDayState,
    val provenance: Provenance,
)

data class Estimate(
    val conceptId: ProduceConceptId,
    val basis: MarketBasis,
    val modelVersion: String,
    val wholesaleSourceDates: List<LocalDate>,
    val calibrationCutoff: LocalDate,
    val calculatedAt: Instant,
    val point: ScaledPrice?,
    val intervalLower: ScaledPrice?,
    val intervalUpper: ScaledPrice?,
    val confidence: BigDecimal?,
    val unavailableReason: UnavailableReason?,
    val disclosure: EstimateDisclosure = EstimateDisclosure(),
)

data class RefreshMetadata(
    val wholesaleFreshness: Freshness,
    val calibrationCutoff: LocalDate?,
    val lastSuccessfulRefresh: Instant?,
    val lastAttemptedRefresh: Instant?,
    val failureReason: UnavailableReason?,
)

interface RefreshRepository {
    val state: Flow<RefreshState>

    suspend fun refresh()
}

sealed interface RefreshState {
    data object Idle : RefreshState
    data object Refreshing : RefreshState
    data class Failed(val retryable: Boolean) : RefreshState
}

interface ProduceRepository {
    fun search(query: String): Flow<List<ProduceConcept>>

    fun browse(category: ProduceCategory): Flow<List<ProduceConcept>>
}

// TODO(3.1): Add reviewed source contracts after endpoint terms and fixtures are verified.
// TODO(5.6): Add estimator registry and eligibility contracts.
// TODO(6.1): Add tracking and alert use-case contracts.
