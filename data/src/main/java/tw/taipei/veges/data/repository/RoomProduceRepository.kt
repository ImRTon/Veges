package tw.taipei.veges.data.repository

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import tw.taipei.veges.data.local.VegesDatabase
import tw.taipei.veges.data.local.EstimateEntity
import tw.taipei.veges.domain.MarketItem
import tw.taipei.veges.domain.MarketHistoryPoint
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.PriceUnit
import tw.taipei.veges.domain.ProduceCategory
import tw.taipei.veges.domain.ProduceConcept
import tw.taipei.veges.domain.ProduceRepository
import tw.taipei.veges.domain.ScaledPrice

class RoomProduceRepository @Inject constructor(
    private val database: VegesDatabase,
) : ProduceRepository {
    override fun search(query: String): Flow<List<ProduceConcept>> {
        val normalized = normalizeSearchQuery(query)
        if (normalized.isEmpty()) return flowOf(emptyList())

        return database.taxonomyDao()
            .observeSearch("%$normalized%")
            .map { concepts -> concepts.map { it.toDomain() } }
    }

    override fun browse(category: ProduceCategory): Flow<List<ProduceConcept>> =
        database.taxonomyDao()
            .observePublished(category)
            .map { concepts -> concepts.map { it.toDomain() } }

    override fun observeMarket(category: ProduceCategory): Flow<List<MarketItem>> = combine(
        database.taxonomyDao().observePublished(category),
        database.estimateDao().observePublishedHistory(category),
        database.sourceDao().observeConceptMarketHistory(category),
    ) { concepts, estimates, observations ->
        val historyByConcept = estimates.groupBy(EstimateEntity::conceptId)
        val wholesaleByConcept = observations.groupBy { it.conceptId }
        concepts.map { details ->
            val concept = details.toDomain()
            val ordered = historyByConcept[concept.id.value].orEmpty()
                .distinctBy { it.basis to it.sourceDate }
            val latestEntity = ordered.maxWithOrNull(
                compareBy<EstimateEntity>(
                    EstimateEntity::sourceDate,
                    { it.basis.marketPriority() },
                    EstimateEntity::calculatedAt,
                ),
            )
            val latest = latestEntity?.toDomain()
            val comparable = latestEntity?.let { current ->
                ordered.filter {
                    it.basis == current.basis &&
                        it.sourceDate < current.sourceDate &&
                        it.pointValue != null &&
                        it.unavailableReason == null
                }
            }.orEmpty()
            val previous = comparable.firstOrNull()?.toDomain()
            MarketItem(
                concept = concept,
                latestEstimate = latest,
                previousEstimate = previous,
                previousTradingDayPrices = comparable
                    .mapNotNull(EstimateEntity::pointValue)
                    .take(30)
                    .map { ScaledPrice(it, PriceUnit.NTD_PER_TAI_JIN) },
                wholesaleHistory = wholesaleByConcept[concept.id.value]
                    .orEmpty()
                    .groupBy { it.observedOn }
                    .mapNotNull { (date, rows) ->
                        val totalVolume = rows.sumOf { it.volume }
                        if (totalVolume <= java.math.BigDecimal.ZERO) {
                            null
                        } else {
                            MarketHistoryPoint(
                                observedOn = date,
                                averageNtdPerKg = rows
                                    .sumOf { it.averagePrice.multiply(it.volume) }
                                    .divide(
                                        totalVolume,
                                        6,
                                        java.math.RoundingMode.HALF_UP,
                                    ),
                                volumeKg = totalVolume,
                            )
                        }
                    }
                    .sortedByDescending(MarketHistoryPoint::observedOn)
                    .take(31),
            )
        }
    }
}

private fun MarketBasis.marketPriority(): Int = when (this) {
    MarketBasis.TAIPEI_COMBINED -> 2
    MarketBasis.TAIPEI_FIRST -> 1
    MarketBasis.TAIPEI_SECOND -> 0
}
