package tw.taipei.veges.data.local

import androidx.room.Embedded
import androidx.room.Relation
import java.math.BigDecimal
import java.time.LocalDate
import tw.taipei.veges.domain.MarketBasis

data class TaxonomyConceptWithDetails(
    @Embedded val concept: TaxonomyConceptEntity,
    @Relation(
        parentColumn = "stableId",
        entityColumn = "conceptId",
    )
    val aliases: List<TaxonomyAliasEntity>,
    @Relation(
        parentColumn = "stableId",
        entityColumn = "conceptId",
    )
    val variants: List<OfficialVariantEntity>,
)

data class PendingNotificationDelivery(
    val eventId: String,
    val conceptId: String,
    val householdName: String,
    val basis: MarketBasis,
    val estimateNtdPerTaiJin: BigDecimal,
    val thresholdNtdPerTaiJin: BigDecimal,
    val sourceDate: LocalDate,
)

data class ConceptMarketObservation(
    val conceptId: String,
    val observedOn: LocalDate,
    val averagePrice: BigDecimal,
    val volume: BigDecimal,
)
