package tw.taipei.veges.domain

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemPriceDirectionPredictorTest {
    private val today = LocalDate.parse("2026-07-29")
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
            today = today,
        )
        val outlook = requireNotNull(result.outlook)

        assertEquals(ItemPriceDirectionStatus.SIGNAL, result.status)
        assertEquals(ItemPriceDirection.RISING, outlook.direction)
        assertTrue(outlook.projectedChangePercent > BigDecimal.ZERO)
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
            today = today,
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
            today = today,
        )

        assertEquals(ItemPriceDirectionStatus.NO_CLEAR_SIGNAL, result.status)
        assertNull(result.outlook)
    }

    @Test
    fun fewerThanTenValidDaysReportsInsufficientHistory() {
        val result = predictor.evaluate(
            history = history().take(9),
            today = today,
        )

        assertEquals(ItemPriceDirectionStatus.INSUFFICIENT_HISTORY, result.status)
        assertEquals(9, result.validTradingDayCount)
        assertNull(result.outlook)
    }

    @Test
    fun oldLatestObservationReportsStaleData() {
        val result = predictor.evaluate(
            history = history().map { it.copy(observedOn = it.observedOn.minusDays(5)) },
            today = today,
        )

        assertEquals(ItemPriceDirectionStatus.STALE_DATA, result.status)
        assertNull(result.outlook)
    }

    @Test
    fun neutralMarketHistoryDoesNotCreateSignal() {
        val result = predictor.evaluate(
            history = history(),
            today = today,
        )

        assertEquals(ItemPriceDirectionStatus.NO_CLEAR_SIGNAL, result.status)
        assertNull(result.outlook)
    }

    @Test
    fun includesEachRisingReasonAtItsConfiguredBoundary() {
        val exactVolume = requireNotNull(
            predictor.evaluate(
                history = history(
                    recentPrice = "150",
                    baselinePrice = "100",
                    recentVolume = "85",
                    baselineVolume = "100",
                ),
                today = today,
            ).outlook,
        )
        val exactMomentum = requireNotNull(
            predictor.evaluate(
                history = history(
                    recentPrice = "105",
                    baselinePrice = "100",
                    recentVolume = "50",
                    baselineVolume = "100",
                    olderPrice = "60",
                ),
                today = today,
            ).outlook,
        )
        val exactAnomaly = requireNotNull(
            predictor.evaluate(
                history = history(
                    recentPrice = "110",
                    baselinePrice = "100",
                    recentVolume = "50",
                    baselineVolume = "100",
                    olderPrice = "100",
                ),
                today = today,
            ).outlook,
        )

        assertTrue(exactVolume.reasons.any { it.kind == ItemPriceDirectionReasonKind.VOLUME_CONTRACTION })
        assertTrue(exactMomentum.reasons.any { it.kind == ItemPriceDirectionReasonKind.PRICE_MOMENTUM_UP })
        assertTrue(exactAnomaly.reasons.any { it.kind == ItemPriceDirectionReasonKind.ABOVE_RECENT_NORMAL })
    }

    @Test
    fun subthresholdVolumeDoesNotChangeProjection() {
        val neutralVolume = requireNotNull(
            predictor.evaluate(
                history = history(
                    recentPrice = "130",
                    baselinePrice = "100",
                    recentVolume = "100",
                    baselineVolume = "100",
                ),
                today = today,
            ).outlook,
        )
        val belowThresholdVolume = requireNotNull(
            predictor.evaluate(
                history = history(
                    recentPrice = "130",
                    baselinePrice = "100",
                    recentVolume = "86",
                    baselineVolume = "100",
                ),
                today = today,
            ).outlook,
        )

        assertEquals(neutralVolume.projectedChangePercent, belowThresholdVolume.projectedChangePercent)
        assertTrue(
            belowThresholdVolume.reasons.none {
                it.kind == ItemPriceDirectionReasonKind.VOLUME_CONTRACTION
            },
        )
    }

    @Test
    fun subthresholdMomentumDoesNotChangeProjection() {
        val neutralMomentum = requireNotNull(
            predictor.evaluate(
                history = history(
                    recentPrice = "100",
                    baselinePrice = "100",
                    recentVolume = "50",
                    baselineVolume = "100",
                    olderPrice = "60",
                ),
                today = today,
            ).outlook,
        )
        val belowThresholdMomentum = requireNotNull(
            predictor.evaluate(
                history = history(
                    recentPrice = "100",
                    baselinePrice = "96",
                    recentVolume = "50",
                    baselineVolume = "100",
                    olderPrice = "60",
                ),
                today = today,
            ).outlook,
        )

        assertEquals(neutralMomentum.projectedChangePercent, belowThresholdMomentum.projectedChangePercent)
        assertTrue(
            belowThresholdMomentum.reasons.none {
                it.kind == ItemPriceDirectionReasonKind.PRICE_MOMENTUM_UP
            },
        )
    }

    @Test
    fun subthresholdAnomalyDoesNotChangeProjection() {
        val neutralAnomaly = requireNotNull(
            predictor.evaluate(
                history = history(
                    recentPrice = "120",
                    baselinePrice = "100",
                    recentVolume = "50",
                    baselineVolume = "100",
                    olderPrice = "120",
                ),
                today = today,
            ).outlook,
        )
        val belowThresholdAnomaly = requireNotNull(
            predictor.evaluate(
                history = history(
                    recentPrice = "120",
                    baselinePrice = "100",
                    recentVolume = "50",
                    baselineVolume = "100",
                    olderPrice = "110",
                ),
                today = today,
            ).outlook,
        )

        assertEquals(neutralAnomaly.projectedChangePercent, belowThresholdAnomaly.projectedChangePercent)
        assertTrue(
            belowThresholdAnomaly.reasons.none {
                it.kind == ItemPriceDirectionReasonKind.ABOVE_RECENT_NORMAL
            },
        )
    }

    private fun history(
        recentPrice: String = "100",
        baselinePrice: String = "100",
        recentVolume: String = "100",
        baselineVolume: String = "100",
        olderPrice: String = baselinePrice,
    ): List<MarketHistoryPoint> = List(23) { index ->
        MarketHistoryPoint(
            observedOn = today.minusDays(index.toLong()),
            averageNtdPerKg = BigDecimal(
                when {
                    index < 3 -> recentPrice
                    index < 10 -> baselinePrice
                    else -> olderPrice
                },
            ),
            volumeKg = BigDecimal(if (index < 3) recentVolume else baselineVolume),
        )
    }

}
