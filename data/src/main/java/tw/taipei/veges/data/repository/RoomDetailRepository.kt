package tw.taipei.veges.data.repository

import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import tw.taipei.veges.data.local.SourceObservationEntity
import tw.taipei.veges.data.local.VegesDatabase
import tw.taipei.veges.domain.DetailRepository
import tw.taipei.veges.domain.DetailSnapshot
import tw.taipei.veges.domain.EstimationMath
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.PriceUnit
import tw.taipei.veges.domain.ProduceConceptId
import tw.taipei.veges.domain.TrendPeriod
import tw.taipei.veges.domain.TrendPoint

@OptIn(ExperimentalCoroutinesApi::class)
class RoomDetailRepository @Inject constructor(
    private val database: VegesDatabase,
    private val clock: Clock,
) : DetailRepository {
    override fun observeDetail(
        conceptId: ProduceConceptId,
        basis: MarketBasis,
        period: TrendPeriod,
    ): Flow<DetailSnapshot> {
        val conceptFlow = database.taxonomyDao().observePublishedConcept(conceptId.value)
        val sourceFlow = conceptFlow.flatMapLatest { concept ->
            val codes = concept?.variants?.map { it.commodityCode }?.distinct().orEmpty()
            if (codes.isEmpty()) {
                flowOf(emptyList())
            } else {
                database.sourceDao().observeValidWholesaleObservations(
                    commodityCodes = codes,
                    from = LocalDate.now(clock).minusDays(period.days.toLong() - 1L),
                )
            }
        }
        val from = LocalDate.now(clock).minusDays(period.days.toLong() - 1L)
        return combine(
            conceptFlow,
            database.estimateDao().observeHistory(conceptId.value, basis, from),
            sourceFlow,
            database.trackingDao().observeIsTracked(conceptId.value),
        ) { concept, estimates, sourceRows, isTracked ->
            val domainEstimates = estimates.map { it.toDomain() }
            DetailSnapshot(
                concept = concept?.toDomain(),
                estimate = domainEstimates.maxWithOrNull(
                    compareBy({ it.wholesaleSourceDates.maxOrNull() }, { it.calculatedAt }),
                ),
                estimateHistory = domainEstimates,
                trendPoints = aggregateTrend(sourceRows, basis),
                isTracked = isTracked,
            )
        }
    }

    private fun aggregateTrend(
        rows: List<SourceObservationEntity>,
        basis: MarketBasis,
    ): List<TrendPoint> = rows
        .groupBy { it.observedOn }
        .toSortedMap()
        .mapNotNull { (date, dayRows) ->
            val selectedRows = when (basis) {
                MarketBasis.TAIPEI_FIRST,
                MarketBasis.TAIPEI_SECOND,
                -> dayRows.filter { it.market == basis }

                MarketBasis.TAIPEI_COMBINED -> {
                    val markets = dayRows.map { it.market }.toSet()
                    if (MarketBasis.TAIPEI_FIRST !in markets || MarketBasis.TAIPEI_SECOND !in markets) {
                        return@mapNotNull null
                    }
                    dayRows
                }
            }
            selectedRows.toTrendPoint(date, basis)
        }

    private fun List<SourceObservationEntity>.toTrendPoint(
        date: LocalDate,
        basis: MarketBasis,
    ): TrendPoint? {
        if (isEmpty() || any {
                it.priceUnit != PriceUnit.NTD_PER_KILOGRAM ||
                    it.volume == null ||
                    it.volume <= BigDecimal.ZERO ||
                    it.averagePrice == null
            }
        ) {
            return null
        }
        val average = EstimationMath.weightedAverage(
            map { requireNotNull(it.averagePrice) to requireNotNull(it.volume) },
        ) ?: return null
        val lower = takeIf { all { row -> row.lowerPrice != null } }?.let {
            EstimationMath.weightedAverage(map { requireNotNull(it.lowerPrice) to requireNotNull(it.volume) })
        }
        val upper = takeIf { all { row -> row.upperPrice != null } }?.let {
            EstimationMath.weightedAverage(map { requireNotNull(it.upperPrice) to requireNotNull(it.volume) })
        }
        return TrendPoint(
            date = date,
            basis = basis,
            lowerNtdPerKg = lower,
            averageNtdPerKg = average,
            upperNtdPerKg = upper,
            volumeKg = sumOf { requireNotNull(it.volume) },
        )
    }
}
