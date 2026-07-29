package tw.taipei.veges.domain

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TrendMarketCandleTest {
    @Test
    fun `candle body connects previous and current average while wick keeps official range`() {
        val points = listOf(
            point("2026-07-25", average = "20", low = "15", high = "24", volume = "1000"),
            point("2026-07-26", average = "23", low = "18", high = "27", volume = "1200"),
        )

        val candle = points.toProduceMarketCandles().last()

        assertEquals(BigDecimal("20"), candle.previousAverageNtdPerKg)
        assertEquals(BigDecimal("23"), candle.currentAverageNtdPerKg)
        assertEquals(BigDecimal("18"), candle.officialLowNtdPerKg)
        assertEquals(BigDecimal("27"), candle.officialHighNtdPerKg)
        assertEquals(BigDecimal("1200"), candle.volumeKg)
        assertEquals(CandleDirection.RISING, candle.direction)
    }

    @Test
    fun `first candle is unchanged because no previous trading day is in range`() {
        val candle = listOf(
            point("2026-07-26", average = "23", low = "18", high = "27", volume = "1200"),
        ).toProduceMarketCandles().single()

        assertEquals(candle.currentAverageNtdPerKg, candle.previousAverageNtdPerKg)
        assertEquals(CandleDirection.UNCHANGED, candle.direction)
    }

    private fun point(
        date: String,
        average: String,
        low: String,
        high: String,
        volume: String,
    ) = TrendPoint(
        date = LocalDate.parse(date),
        basis = MarketBasis.TAIPEI_FIRST,
        lowerNtdPerKg = BigDecimal(low),
        averageNtdPerKg = BigDecimal(average),
        upperNtdPerKg = BigDecimal(high),
        volumeKg = BigDecimal(volume),
    )
}
