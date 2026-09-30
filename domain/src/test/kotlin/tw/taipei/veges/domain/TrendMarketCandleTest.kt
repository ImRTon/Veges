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

    @Test
    fun `weekly candles combine each calendar week and connect to previous week close`() {
        val points = listOf(
            point("2026-07-14", average = "20", low = "15", high = "24", volume = "1000"),
            point("2026-07-17", average = "22", low = "16", high = "26", volume = "800"),
            point("2026-07-21", average = "25", low = "19", high = "28", volume = "600"),
            point("2026-07-23", average = "21", low = "14", high = "30", volume = "400"),
            point("2026-07-26", average = "24", low = "18", high = "27", volume = "500"),
        )

        val candles = points.toProduceMarketCandles(CandleInterval.WEEK)

        assertEquals(2, candles.size)
        val first = candles.first()
        assertEquals(LocalDate.parse("2026-07-14"), first.date)
        assertEquals(LocalDate.parse("2026-07-17"), first.endDate)
        assertEquals(BigDecimal("20"), first.previousAverageNtdPerKg)
        assertEquals(BigDecimal("22"), first.currentAverageNtdPerKg)
        val second = candles.last()
        assertEquals(LocalDate.parse("2026-07-21"), second.date)
        assertEquals(LocalDate.parse("2026-07-26"), second.endDate)
        assertEquals(BigDecimal("22"), second.previousAverageNtdPerKg)
        assertEquals(BigDecimal("24"), second.currentAverageNtdPerKg)
        assertEquals(BigDecimal("14"), second.officialLowNtdPerKg)
        assertEquals(BigDecimal("30"), second.officialHighNtdPerKg)
        assertEquals(BigDecimal("1500"), second.volumeKg)
        assertEquals(CandleDirection.RISING, second.direction)
    }

    @Test
    fun `long periods use weekly candles while short periods stay daily`() {
        assertEquals(CandleInterval.DAY, TrendPeriod.SEVEN_DAYS.candleInterval)
        assertEquals(CandleInterval.DAY, TrendPeriod.THIRTY_DAYS.candleInterval)
        assertEquals(CandleInterval.WEEK, TrendPeriod.NINETY_DAYS.candleInterval)
        assertEquals(CandleInterval.WEEK, TrendPeriod.ONE_YEAR.candleInterval)
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
