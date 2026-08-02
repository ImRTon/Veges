package tw.taipei.veges.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

data class HomeItem(
    val concept: ProduceConcept,
    val latestEstimate: Estimate?,
    val previousEstimate: Estimate? = null,
    val activeAlertCount: Int = 0,
    val unavailableReason: UnavailableReason?,
)

data class MarketItem(
    val concept: ProduceConcept,
    val latestEstimate: Estimate?,
    val previousEstimate: Estimate? = null,
    val previousTradingDayPrices: List<ScaledPrice> = emptyList(),
    val wholesaleHistory: List<MarketHistoryPoint> = emptyList(),
)

data class MarketHistoryPoint(
    val observedOn: LocalDate,
    val averageNtdPerKg: BigDecimal,
    val volumeKg: BigDecimal,
)

fun MarketItem.previousChangePercent(): BigDecimal? =
    percentageChange(
        current = latestEstimate?.point?.amount,
        reference = previousEstimate?.point?.amount,
    )

fun HomeItem.previousChangePercent(): BigDecimal? =
    percentageChange(
        current = latestEstimate?.point?.amount,
        reference = previousEstimate?.point?.amount,
    )

fun MarketItem.sevenDayAverageChangePercent(): BigDecimal? =
    averageChangePercent(7)

fun MarketItem.averageChangePercent(tradingDays: Int): BigDecimal? {
    require(tradingDays > 0) { "Trading-day window must be positive" }
    val values = previousTradingDayPrices.take(tradingDays)
    if (values.size < tradingDays) return null
    val average = values
        .map(ScaledPrice::amount)
        .reduce(BigDecimal::add)
        .divide(BigDecimal(tradingDays), 6, RoundingMode.HALF_UP)
    return percentageChange(
        current = latestEstimate?.point?.amount,
        reference = average,
    )
}

private fun percentageChange(
    current: BigDecimal?,
    reference: BigDecimal?,
): BigDecimal? {
    if (current == null || reference == null || reference.signum() <= 0) return null
    return current
        .subtract(reference)
        .divide(reference, 6, RoundingMode.HALF_UP)
        .multiply(BigDecimal("100"))
}

interface HomeRepository {
    fun observeHome(): Flow<List<HomeItem>>

    fun observePriceRefresh(): Flow<PriceRefresh>

    fun requestRefresh()
}

enum class PriceRefreshStage {
    PREPARING,
    LATEST_PRICES,
    HISTORY,
    SAVING,
}

data class PriceRefresh(
    val isRunning: Boolean = false,
    val stage: PriceRefreshStage = PriceRefreshStage.PREPARING,
    val fraction: Float = 0f,
)

sealed interface CatalogSearchResult {
    data object NoReviewedResult : CatalogSearchResult
    data class Single(val concept: ProduceConcept) : CatalogSearchResult
    data class Ambiguous(val query: String, val concepts: List<ProduceConcept>) : CatalogSearchResult
}
