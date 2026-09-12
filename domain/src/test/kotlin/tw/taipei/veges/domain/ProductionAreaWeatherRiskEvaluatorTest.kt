package tw.taipei.veges.domain

import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProductionAreaWeatherRiskEvaluatorTest {
    private val now = Instant.parse("2026-08-09T02:00:00Z")
    private val evaluator = ProductionAreaWeatherRiskEvaluator()

    @Test
    fun mergesTyphoonDrivenRainAcrossAgriculturalCounties() {
        val risk = evaluator.evaluate(
            signals = listOf(
                signal(setOf("苗栗縣泰安鄉")),
                signal(setOf("臺中市和平區", "臺北市北投區")),
            ),
            now = now,
        )

        requireNotNull(risk)
        assertEquals(MarketShockKind.TYPHOON, risk.kind)
        assertEquals(listOf("苗栗縣", "臺中市"), risk.affectedCounties)
    }

    @Test
    fun ignoresSeaOnlyTyphoonWarning() {
        val risk = evaluator.evaluate(
            signals = listOf(
                signal(
                    areas = setOf("臺灣北部海面"),
                    kind = MarketShockKind.TYPHOON,
                    cause = null,
                ),
            ),
            now = now,
        )

        assertNull(risk)
    }

    @Test
    fun ignoresExpiredAgriculturalWarning() {
        val risk = evaluator.evaluate(
            signals = listOf(
                signal(setOf("雲林縣古坑鄉")).copy(expiresAt = now),
            ),
            now = now,
        )

        assertNull(risk)
    }

    private fun signal(
        areas: Set<String>,
        kind: MarketShockKind = MarketShockKind.HEAVY_RAIN,
        cause: MarketShockKind? = MarketShockKind.TYPHOON,
    ) = MarketShockSignal(
        kind = kind,
        severity = BigDecimal("0.9"),
        headline = "豪雨特報",
        affectedAreas = areas,
        effectiveAt = now.minusSeconds(3_600),
        expiresAt = now.plusSeconds(10_800),
        cause = cause,
    )
}
