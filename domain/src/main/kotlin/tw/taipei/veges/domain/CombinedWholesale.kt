package tw.taipei.veges.domain

import java.math.BigDecimal
import java.time.LocalDate

data class MarketWholesaleInput(
    val market: MarketBasis,
    val observedOn: LocalDate,
    val averageNtdPerKg: BigDecimal,
    val lowerNtdPerKg: BigDecimal,
    val upperNtdPerKg: BigDecimal,
    val volumeKg: BigDecimal,
)

sealed interface CombinedWholesaleResult {
    data class Success(
        val observedOn: LocalDate,
        val averageNtdPerKg: BigDecimal,
        val lowerNtdPerKg: BigDecimal,
        val upperNtdPerKg: BigDecimal,
        val volumeKg: BigDecimal,
    ) : CombinedWholesaleResult

    data class Unavailable(val reason: UnavailableReason) : CombinedWholesaleResult
}

fun combineTaipeiWholesale(
    first: MarketWholesaleInput?,
    second: MarketWholesaleInput?,
): CombinedWholesaleResult {
    if (first == null || second == null) {
        return CombinedWholesaleResult.Unavailable(UnavailableReason.MISSING_SOURCE_DATA)
    }
    if (first.market != MarketBasis.TAIPEI_FIRST || second.market != MarketBasis.TAIPEI_SECOND) {
        return CombinedWholesaleResult.Unavailable(UnavailableReason.INVALID_SOURCE_DATA)
    }
    if (first.observedOn != second.observedOn) {
        return CombinedWholesaleResult.Unavailable(UnavailableReason.MISSING_SOURCE_DATA)
    }
    if (listOf(first.volumeKg, second.volumeKg).any { it <= BigDecimal.ZERO }) {
        return CombinedWholesaleResult.Unavailable(UnavailableReason.INVALID_SOURCE_DATA)
    }

    fun weighted(selector: (MarketWholesaleInput) -> BigDecimal) = EstimationMath.weightedAverage(
        listOf(selector(first) to first.volumeKg, selector(second) to second.volumeKg),
    )

    val average = weighted { it.averageNtdPerKg }
    val lower = weighted { it.lowerNtdPerKg }
    val upper = weighted { it.upperNtdPerKg }
    if (average == null || lower == null || upper == null) {
        return CombinedWholesaleResult.Unavailable(UnavailableReason.INVALID_SOURCE_DATA)
    }
    return CombinedWholesaleResult.Success(
        observedOn = first.observedOn,
        averageNtdPerKg = average,
        lowerNtdPerKg = lower,
        upperNtdPerKg = upper,
        volumeKg = first.volumeKg + second.volumeKg,
    )
}
