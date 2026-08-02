package tw.taipei.veges.domain

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemPriceDirectionPredictorTest {
    private val today = LocalDate.parse("2026-07-29")
    private val now = Instant.parse("2026-07-29T14:00:00Z")
    private val predictor = ItemPriceDirectionPredictor()

    @Test
    fun reportsExplainableRisingSignal() {
        val result = predictor.evaluate(
            history = history(
                recentPrice = "130",
                baselinePrice = "100",
                recentVolume = "50",
                baselineVolume = "100",
            ),
            shockSignals = listOf(activeShock(MarketShockKind.TYPHOON)),
            today = today,
            now = now,
        )
        val outlook = requireNotNull(result.outlook)

        assertEquals(ItemPriceDirectionStatus.SIGNAL, result.status)
        assertEquals(ItemPriceDirection.RISING, outlook.direction)
        assertTrue(outlook.projectedChangePercent > BigDecimal.ZERO)
        assertTrue(outlook.reasons.any { it.kind == ItemPriceDirectionReasonKind.TYPHOON })
        assertTrue(outlook.reasons.any { it.kind == ItemPriceDirectionReasonKind.PRICE_MOMENTUM_UP })
    }

    @Test
    fun reportsExplainableFallingSignal() {
        val result = predictor.evaluate(
            history = history(
                recentPrice = "70",
                baselinePrice = "100",
                recentVolume = "160",
                baselineVolume = "100",
            ),
            shockSignals = emptyList(),
            today = today,
            now = now,
        )
        val outlook = requireNotNull(result.outlook)

        assertEquals(ItemPriceDirectionStatus.SIGNAL, result.status)
        assertEquals(ItemPriceDirection.FALLING, outlook.direction)
        assertTrue(outlook.projectedChangePercent < BigDecimal.ZERO)
        assertTrue(outlook.reasons.any { it.kind == ItemPriceDirectionReasonKind.VOLUME_EXPANSION })
        assertTrue(outlook.reasons.any { it.kind == ItemPriceDirectionReasonKind.PRICE_MOMENTUM_DOWN })
    }

    @Test
    fun stableHistoryReportsNoClearSignal() {
        val result = predictor.evaluate(
            history = history(),
            shockSignals = emptyList(),
            today = today,
            now = now,
        )

        assertEquals(ItemPriceDirectionStatus.NO_CLEAR_SIGNAL, result.status)
        assertNull(result.outlook)
    }

    @Test
    fun fewerThanTenValidDaysReportsInsufficientHistory() {
        val result = predictor.evaluate(
            history = history().take(9),
            shockSignals = emptyList(),
            today = today,
            now = now,
        )

        assertEquals(ItemPriceDirectionStatus.INSUFFICIENT_HISTORY, result.status)
        assertEquals(9, result.validTradingDayCount)
        assertNull(result.outlook)
    }

    @Test
    fun oldLatestObservationReportsStaleData() {
        val result = predictor.evaluate(
            history = history().map { it.copy(observedOn = it.observedOn.minusDays(5)) },
            shockSignals = emptyList(),
            today = today,
            now = now,
        )

        assertEquals(ItemPriceDirectionStatus.STALE_DATA, result.status)
        assertNull(result.outlook)
    }

    @Test
    fun activeWeatherWithoutMarketMovementDoesNotCreateSignal() {
        val result = predictor.evaluate(
            history = history(),
            shockSignals = listOf(activeShock(MarketShockKind.HEAVY_RAIN)),
            today = today,
            now = now,
        )

        assertEquals(ItemPriceDirectionStatus.NO_CLEAR_SIGNAL, result.status)
        assertNull(result.outlook)
    }

    @Test
    fun expiredWeatherDoesNotContributeToRisingSignal() {
        val expired = activeShock(MarketShockKind.TYPHOON).copy(
            effectiveAt = now.minusSeconds(7_200),
            expiresAt = now.minusSeconds(3_600),
        )
        val result = predictor.evaluate(
            history = history(
                recentPrice = "130",
                baselinePrice = "100",
                recentVolume = "50",
                baselineVolume = "100",
            ),
            shockSignals = listOf(expired),
            today = today,
            now = now,
        )
        val outlook = requireNotNull(result.outlook)

        assertEquals(ItemPriceDirection.RISING, outlook.direction)
        assertTrue(outlook.reasons.none { it.kind == ItemPriceDirectionReasonKind.TYPHOON })
    }

    private fun history(
        recentPrice: String = "100",
        baselinePrice: String = "100",
        recentVolume: String = "100",
        baselineVolume: String = "100",
    ): List<MarketHistoryPoint> = List(23) { index ->
        MarketHistoryPoint(
            observedOn = today.minusDays(index.toLong()),
            averageNtdPerKg = BigDecimal(if (index < 3) recentPrice else baselinePrice),
            volumeKg = BigDecimal(if (index < 3) recentVolume else baselineVolume),
        )
    }

    private fun activeShock(kind: MarketShockKind) = MarketShockSignal(
        kind = kind,
        severity = BigDecimal.ONE,
        headline = "官方警報",
        affectedAreas = setOf("雲林縣"),
        effectiveAt = now.minusSeconds(3_600),
        expiresAt = now.plusSeconds(3_600),
    )
}
