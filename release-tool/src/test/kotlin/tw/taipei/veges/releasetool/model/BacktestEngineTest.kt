package tw.taipei.veges.releasetool.model

import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.taipei.veges.domain.CalibrationTarget
import tw.taipei.veges.domain.EstimatorFamily
import tw.taipei.veges.domain.EstimatorFeatureSet
import tw.taipei.veges.domain.MarketBasis

class BacktestEngineTest {
    @Test
    fun expandingWindowNeverUsesFutureRows() {
        val rows = (1..4).map { day ->
            val date = LocalDate.of(2026, 1, day)
            HistoricalCalibrationRow(
                observedOn = date,
                features = EstimatorFeatureSet(
                    wholesaleAverageNtdPerKg = BigDecimal("30"),
                    wholesaleLowerNtdPerKg = BigDecimal("20"),
                    wholesaleUpperNtdPerKg = BigDecimal("40"),
                    transactionVolumeKg = BigDecimal("100"),
                    monthOfYear = 1,
                    marketBasis = MarketBasis.TAIPEI_COMBINED,
                ),
                target = CalibrationTarget(date, BigDecimal((day * 10).toString())),
            )
        }

        val result = ExpandingWindowBacktest().run(
            rows = rows,
            family = EstimatorFamily.SEASONAL_BASELINE,
            basis = MarketBasis.TAIPEI_COMBINED,
            config = BacktestConfig(minimumTrainingRows = 1),
        )

        assertEquals(3, result.metrics.periods.size)
        assertEquals(LocalDate.of(2026, 1, 2), result.metrics.periods.first().predictionDate)
        assertTrue(result.metrics.periods.first().predicted < BigDecimal("30"))
        assertEquals("2026-01-01:INSUFFICIENT_PRIOR_TRAINING_ROWS", result.exclusions.single())
    }

    @Test
    fun insufficientPriorRowsAreExcludedWithReason() {
        val date = LocalDate.of(2026, 1, 1)
        val row = HistoricalCalibrationRow(
            observedOn = date,
            features = EstimatorFeatureSet(
                wholesaleAverageNtdPerKg = BigDecimal("30"),
                wholesaleLowerNtdPerKg = BigDecimal("20"),
                wholesaleUpperNtdPerKg = BigDecimal("40"),
                transactionVolumeKg = BigDecimal("100"),
                monthOfYear = 1,
                marketBasis = MarketBasis.TAIPEI_FIRST,
            ),
            target = CalibrationTarget(date, BigDecimal("50")),
        )

        val result = ExpandingWindowBacktest().run(
            rows = listOf(row),
            family = EstimatorFamily.LINEAR_CALIBRATION,
            basis = MarketBasis.TAIPEI_FIRST,
            config = BacktestConfig(minimumTrainingRows = 2),
        )

        assertTrue(result.metrics.periods.isEmpty())
        assertEquals("2026-01-01:INSUFFICIENT_PRIOR_TRAINING_ROWS", result.exclusions.single())
    }
}
