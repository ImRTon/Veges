package tw.taipei.veges.releasetool.model

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import tw.taipei.veges.domain.BacktestMetrics
import tw.taipei.veges.domain.BacktestPeriodMetric
import tw.taipei.veges.domain.CalibrationTarget
import tw.taipei.veges.domain.EstimatorFamily
import tw.taipei.veges.domain.EstimatorFeatureSet
import tw.taipei.veges.domain.MarketBasis

data class HistoricalCalibrationRow(
    val observedOn: LocalDate,
    val features: EstimatorFeatureSet,
    val target: CalibrationTarget,
)

data class BacktestConfig(
    val minimumTrainingRows: Int,
    val validationStart: LocalDate? = null,
    val validationEnd: LocalDate? = null,
)

data class BacktestResult(
    val family: EstimatorFamily,
    val basis: MarketBasis,
    val dataCutoff: LocalDate,
    val calibrationCutoff: LocalDate,
    val metrics: BacktestMetrics,
    val exclusions: List<String>,
)

class ExpandingWindowBacktest {
    fun run(
        rows: List<HistoricalCalibrationRow>,
        family: EstimatorFamily,
        basis: MarketBasis,
        config: BacktestConfig,
    ): BacktestResult {
        val ordered = rows
            .filter { it.features.marketBasis == basis && it.observedOn == it.target.observedOn }
            .sortedBy { it.observedOn }
        val dataCutoff = ordered.maxOfOrNull { it.observedOn }
            ?: error("No rows available for $basis")
        val validationRows = ordered.filter { row ->
            (config.validationStart == null || !row.observedOn.isBefore(config.validationStart)) &&
                (config.validationEnd == null || !row.observedOn.isAfter(config.validationEnd))
        }
        val periods = mutableListOf<BacktestPeriodMetric>()
        val exclusions = mutableListOf<String>()

        validationRows.forEach { validation ->
            val training = ordered.filter { it.observedOn.isBefore(validation.observedOn) }
            if (training.size < config.minimumTrainingRows) {
                exclusions += "${validation.observedOn}:INSUFFICIENT_PRIOR_TRAINING_ROWS"
                return@forEach
            }

            val model = fit(training, family)
            val predicted = model.predict(validation.features)
            val absoluteError = predicted.subtract(validation.target.retailNtdPerTaiJin).abs()
            periods += BacktestPeriodMetric(
                predictionDate = validation.observedOn,
                actual = validation.target.retailNtdPerTaiJin,
                predicted = predicted,
                absoluteError = absoluteError,
                intervalCovered = null,
            )
        }

        val metrics = if (periods.isEmpty()) {
            BacktestMetrics(emptyList(), null, null, null, null)
        } else {
            val mae = periods.map { it.absoluteError }.averageDecimal()
            val rmse = periods.map { it.absoluteError.pow(2) }
                .averageDecimal()
                .sqrt(MathContext(8, RoundingMode.HALF_UP))
            BacktestMetrics(periods, mae, rmse, null, null)
        }
        val calibrationCutoff = periods.maxOfOrNull { it.predictionDate } ?: dataCutoff
        return BacktestResult(family, basis, dataCutoff, calibrationCutoff, metrics, exclusions)
    }

    private fun fit(rows: List<HistoricalCalibrationRow>, family: EstimatorFamily): FittedModel {
        // The first implementation is deliberately transparent: a robust historical median.
        // Candidate families remain explicit so later fitting cannot silently change the artifact.
        require(family in EstimatorFamily.entries) { "Unknown estimator family" }
        val median = rows.map { it.target.retailNtdPerTaiJin }.sorted().let { values ->
            values[values.size / 2]
        }
        return FittedModel { median }
    }

    private fun List<BigDecimal>.averageDecimal(): BigDecimal =
        fold(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal(size), 8, RoundingMode.HALF_UP)

    private fun interface FittedModel {
        fun predict(features: EstimatorFeatureSet): BigDecimal
    }
}
