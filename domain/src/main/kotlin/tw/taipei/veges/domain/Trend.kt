package tw.taipei.veges.domain

import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate

enum class TrendPeriod(val days: Int) {
    SEVEN_DAYS(7),
    THIRTY_DAYS(30),
    NINETY_DAYS(90),
    ONE_YEAR(365),
}

enum class CandleInterval {
    DAY,
    WEEK,
}

val TrendPeriod.candleInterval: CandleInterval
    get() = when (this) {
        TrendPeriod.SEVEN_DAYS, TrendPeriod.THIRTY_DAYS -> CandleInterval.DAY
        TrendPeriod.NINETY_DAYS, TrendPeriod.ONE_YEAR -> CandleInterval.WEEK
    }

data class TrendPoint(
    val date: LocalDate,
    val basis: MarketBasis,
    val lowerNtdPerKg: BigDecimal?,
    val averageNtdPerKg: BigDecimal?,
    val upperNtdPerKg: BigDecimal?,
    val volumeKg: BigDecimal?,
)

data class VariantMarketPrice(
    val commodityCode: OfficialCommodityCode,
    val officialName: String,
    val basis: MarketBasis,
    val observedOn: LocalDate?,
    val wholesaleAverage: ScaledPrice?,
    val volumeKg: BigDecimal?,
)

enum class CandleDirection {
    RISING,
    FALLING,
    UNCHANGED,
}

data class ProduceMarketCandle(
    val date: LocalDate,
    val basis: MarketBasis,
    val previousAverageNtdPerKg: BigDecimal,
    val currentAverageNtdPerKg: BigDecimal,
    val officialLowNtdPerKg: BigDecimal?,
    val officialHighNtdPerKg: BigDecimal?,
    val volumeKg: BigDecimal?,
    val endDate: LocalDate = date,
) {
    val direction: CandleDirection
        get() = when {
            currentAverageNtdPerKg > previousAverageNtdPerKg -> CandleDirection.RISING
            currentAverageNtdPerKg < previousAverageNtdPerKg -> CandleDirection.FALLING
            else -> CandleDirection.UNCHANGED
        }
}

fun List<TrendPoint>.toProduceMarketCandles(): List<ProduceMarketCandle> {
    val ordered = filter { it.averageNtdPerKg != null }.sortedBy { it.date }
    return ordered.mapIndexed { index, point ->
        ProduceMarketCandle(
            date = point.date,
            basis = point.basis,
            previousAverageNtdPerKg = ordered
                .getOrNull(index - 1)
                ?.averageNtdPerKg
                ?: requireNotNull(point.averageNtdPerKg),
            currentAverageNtdPerKg = requireNotNull(point.averageNtdPerKg),
            officialLowNtdPerKg = point.lowerNtdPerKg,
            officialHighNtdPerKg = point.upperNtdPerKg,
            volumeKg = point.volumeKg,
        )
    }
}

fun List<TrendPoint>.toProduceMarketCandles(interval: CandleInterval): List<ProduceMarketCandle> =
    when (interval) {
        CandleInterval.DAY -> toProduceMarketCandles()
        CandleInterval.WEEK -> toWeeklyProduceMarketCandles()
    }

private fun List<TrendPoint>.toWeeklyProduceMarketCandles(): List<ProduceMarketCandle> {
    val weeks = filter { it.averageNtdPerKg != null }
        .sortedBy { it.date }
        .groupBy { it.date.with(DayOfWeek.MONDAY) }
        .values
        .toList()
    return weeks.mapIndexed { index, week ->
        val first = week.first()
        val last = week.last()
        val lows = week.mapNotNull { it.lowerNtdPerKg }
        val highs = week.mapNotNull { it.upperNtdPerKg }
        val volumes = week.mapNotNull { it.volumeKg }
        ProduceMarketCandle(
            date = first.date,
            endDate = last.date,
            basis = last.basis,
            previousAverageNtdPerKg = weeks.getOrNull(index - 1)
                ?.last()
                ?.averageNtdPerKg
                ?: requireNotNull(first.averageNtdPerKg),
            currentAverageNtdPerKg = requireNotNull(last.averageNtdPerKg),
            officialLowNtdPerKg = lows.minOrNull(),
            officialHighNtdPerKg = highs.maxOrNull(),
            volumeKg = volumes.takeIf { it.isNotEmpty() }?.fold(BigDecimal.ZERO, BigDecimal::add),
        )
    }
}

data class TrendSummary(
    val period: TrendPeriod,
    val latest: TrendPoint?,
    val minimumAverage: BigDecimal?,
    val maximumAverage: BigDecimal?,
    val directionText: String,
)

data class DetailSnapshot(
    val concept: ProduceConcept?,
    val estimate: Estimate?,
    val estimateHistory: List<Estimate>,
    val trendPoints: List<TrendPoint>,
    val variantPrices: List<VariantMarketPrice>,
    val isTracked: Boolean,
    val priceDirectionHistory: List<MarketHistoryPoint> = emptyList(),
)

interface DetailRepository {
    fun observeDetail(
        conceptId: ProduceConceptId,
        basis: MarketBasis,
        period: TrendPeriod,
    ): kotlinx.coroutines.flow.Flow<DetailSnapshot>
}

fun interface HistoryRefreshRequester {
    fun requestOneYearHistory(conceptId: ProduceConceptId)
}

fun TrendSummary.accessibilityText(): String {
    val latestText = latest?.averageNtdPerKg
        ?.let(EstimationMath::ntdPerKilogramToNtdPerTaiJin)
        ?.toPlainString()
        ?: "無最新平均價"
    val bodyText = when (period.candleInterval) {
        CandleInterval.DAY -> "每根代表一個交易日，線體連接前一交易日與當日平均價"
        CandleInterval.WEEK -> "每根代表一週，線體連接前一週與當週最後交易日平均價"
    }
    return "${period.days}日蔬果市場 K 線，${directionText}，最新平均價 $latestText 元/台斤。" +
        "$bodyText，影線代表官方低價至高價，成交量另列。"
}
