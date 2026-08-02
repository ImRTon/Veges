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
import tw.taipei.veges.data.local.OfficialVariantEntity
import tw.taipei.veges.data.local.VegesDatabase
import tw.taipei.veges.domain.DetailRepository
import tw.taipei.veges.domain.DetailSnapshot
import tw.taipei.veges.domain.EstimationMath
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.MarketHistoryPoint
import tw.taipei.veges.domain.OfficialCommodityCode
import tw.taipei.veges.domain.PriceUnit
import tw.taipei.veges.domain.ProduceConceptId
import tw.taipei.veges.domain.ScaledPrice
import tw.taipei.veges.domain.TrendPeriod
import tw.taipei.veges.domain.TrendPoint
import tw.taipei.veges.domain.VariantMarketPrice

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
                    from = LocalDate.now(clock).minusDays(
                        maxOf(period.days, VARIANT_PRICE_LOOKBACK_DAYS).toLong() - 1L,
                    ),
                )
            }
        }
        val from = LocalDate.now(clock).minusDays(period.days.toLong() - 1L)
        val analysisFrom = LocalDate.now(clock).minusDays(ANALYSIS_LOOKBACK_DAYS - 1L)
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
                trendPoints = aggregateTrend(
                    rows = sourceRows.filterNot { it.observedOn.isBefore(from) },
                    basis = basis,
                ),
                variantPrices = variantMarketPrices(
                    variants = concept?.variants.orEmpty(),
                    rows = sourceRows,
                    basis = basis,
                ),
                isTracked = isTracked,
                priceDirectionHistory = aggregateTrend(
                    rows = sourceRows.filterNot { it.observedOn.isBefore(analysisFrom) },
                    basis = basis,
                ).mapNotNull { point ->
                    val average = point.averageNtdPerKg ?: return@mapNotNull null
                    val volume = point.volumeKg ?: return@mapNotNull null
                    MarketHistoryPoint(
                        observedOn = point.date,
                        averageNtdPerKg = average,
                        volumeKg = volume,
                    )
                },
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

    private companion object {
        const val VARIANT_PRICE_LOOKBACK_DAYS = 31
        const val ANALYSIS_LOOKBACK_DAYS = 31L
    }
}

internal fun variantMarketPrices(
    variants: List<OfficialVariantEntity>,
    rows: List<SourceObservationEntity>,
    basis: MarketBasis,
): List<VariantMarketPrice> {
    val rowsByCode = rows.groupBy(SourceObservationEntity::commodityCode)
    return variants
        .distinctBy(OfficialVariantEntity::commodityCode)
        .sortedWith(
            compareBy(
                { it.officialName.substringAfter('-', it.officialName) },
                OfficialVariantEntity::commodityCode,
            ),
        )
        .map { variant ->
            val latest = latestVariantAggregation(
                rows = rowsByCode[variant.commodityCode].orEmpty(),
                basis = basis,
            )
            VariantMarketPrice(
                commodityCode = OfficialCommodityCode(variant.commodityCode),
                officialName = variant.officialName,
                basis = basis,
                observedOn = latest?.date,
                wholesaleAverage = latest?.averageNtdPerKg?.let {
                    ScaledPrice(
                        amount = EstimationMath.ntdPerKilogramToNtdPerTaiJin(it),
                        unit = PriceUnit.NTD_PER_TAI_JIN,
                    )
                },
                volumeKg = latest?.volumeKg,
            )
        }
}

private fun latestVariantAggregation(
    rows: List<SourceObservationEntity>,
    basis: MarketBasis,
): VariantAggregation? = rows
    .groupBy(SourceObservationEntity::observedOn)
    .toSortedMap(reverseOrder())
    .asSequence()
    .mapNotNull { (date, dayRows) ->
        val selectedRows = when (basis) {
            MarketBasis.TAIPEI_FIRST,
            MarketBasis.TAIPEI_SECOND,
            -> dayRows.filter { it.market == basis }

            MarketBasis.TAIPEI_COMBINED -> {
                val markets = dayRows.map(SourceObservationEntity::market).toSet()
                if (MarketBasis.TAIPEI_FIRST !in markets || MarketBasis.TAIPEI_SECOND !in markets) {
                    return@mapNotNull null
                }
                dayRows.filter {
                    it.market == MarketBasis.TAIPEI_FIRST ||
                        it.market == MarketBasis.TAIPEI_SECOND
                }
            }
        }
        selectedRows.toVariantAggregation(date)
    }
    .firstOrNull()

private fun List<SourceObservationEntity>.toVariantAggregation(
    date: LocalDate,
): VariantAggregation? {
    if (isEmpty() || any {
            it.priceUnit != PriceUnit.NTD_PER_KILOGRAM ||
                it.averagePrice == null ||
                it.averagePrice <= BigDecimal.ZERO ||
                it.volume == null ||
                it.volume <= BigDecimal.ZERO
        }
    ) {
        return null
    }
    return VariantAggregation(
        date = date,
        averageNtdPerKg = EstimationMath.weightedAverage(
            map { requireNotNull(it.averagePrice) to requireNotNull(it.volume) },
        ) ?: return null,
        volumeKg = sumOf { requireNotNull(it.volume) },
    )
}

private data class VariantAggregation(
    val date: LocalDate,
    val averageNtdPerKg: BigDecimal,
    val volumeKg: BigDecimal,
)
