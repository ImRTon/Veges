package tw.taipei.veges.domain

import java.math.BigDecimal
import java.math.RoundingMode

object EstimationMath {
    val TAI_JIN_PER_KILOGRAM = BigDecimal("1.6666666666666667")
    val KILOGRAMS_PER_TAI_JIN = BigDecimal("0.6")

    fun kilogramsToTaiJin(value: BigDecimal): BigDecimal =
        value.multiply(TAI_JIN_PER_KILOGRAM).setScale(2, RoundingMode.HALF_UP)

    fun ntdPerKilogramToNtdPerTaiJin(value: BigDecimal): BigDecimal =
        value.multiply(KILOGRAMS_PER_TAI_JIN).setScale(8, RoundingMode.HALF_UP)

    fun weightedAverage(
        values: List<Pair<BigDecimal, BigDecimal>>,
    ): BigDecimal? {
        if (values.isEmpty() || values.any { it.second <= BigDecimal.ZERO }) return null
        val totalWeight = values.fold(BigDecimal.ZERO) { total, (_, weight) -> total + weight }
        if (totalWeight <= BigDecimal.ZERO) return null
        return values.fold(BigDecimal.ZERO) { total, (value, weight) -> total + value * weight }
            .divide(totalWeight, 8, RoundingMode.HALF_UP)
    }
}
