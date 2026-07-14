package tw.taipei.veges.domain

import java.math.BigDecimal
import java.time.LocalDate

enum class TrendPeriod(val days: Int) {
    SEVEN_DAYS(7),
    THIRTY_DAYS(30),
    NINETY_DAYS(90),
    ONE_YEAR(365),
}

data class TrendPoint(
    val date: LocalDate,
    val basis: MarketBasis,
    val lowerNtdPerKg: BigDecimal?,
    val averageNtdPerKg: BigDecimal?,
    val upperNtdPerKg: BigDecimal?,
    val volumeKg: BigDecimal?,
)

data class TrendSummary(
    val period: TrendPeriod,
    val latest: TrendPoint?,
    val minimumAverage: BigDecimal?,
    val maximumAverage: BigDecimal?,
    val directionText: String,
)

fun TrendSummary.accessibilityText(): String {
    val latestText = latest?.averageNtdPerKg?.toPlainString() ?: "無最新平均價"
    return "${period.days}日批發趨勢，${directionText}，最新平均價 $latestText 元/公斤"
}
