package tw.taipei.veges.releasetool.model

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.math.exp
import kotlin.math.ln
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

data class FittedEstimator(
    val family: EstimatorFamily,
    val parameters: Map<String, BigDecimal>,
) {
    fun predict(features: EstimatorFeatureSet): BigDecimal = when (family) {
        EstimatorFamily.LINEAR_CALIBRATION -> {
            parameters.required(INTERCEPT) +
                parameters.required(WHOLESALE_AVERAGE_SLOPE) * features.wholesaleAverageNtdPerKg
        }
        EstimatorFamily.LOG_LINEAR_CALIBRATION -> {
            val logPrediction = parameters.required(LOG_INTERCEPT).toDouble() +
                parameters.required(LOG_WHOLESALE_AVERAGE_SLOPE).toDouble() *
                ln(features.wholesaleAverageNtdPerKg.toDouble())
            BigDecimal.valueOf(exp(logPrediction)).normalized()
        }
        EstimatorFamily.SEASONAL_BASELINE -> {
            parameters[monthMedianKey(features.monthOfYear)]
                ?: parameters.required(MEDIAN_RETAIL)
        }
        EstimatorFamily.WHOLESALE_MULTIPLIER_REFERENCE ->
            error("Temporary wholesale multiplier is not a calibrated backtest family")
    }.normalized()

    private fun Map<String, BigDecimal>.required(name: String): BigDecimal =
        requireNotNull(this[name]) { "Missing $name parameter for $family" }

    companion object {
        const val INTERCEPT = "intercept"
        const val WHOLESALE_AVERAGE_SLOPE = "wholesaleAverageSlope"
        const val LOG_INTERCEPT = "logIntercept"
        const val LOG_WHOLESALE_AVERAGE_SLOPE = "logWholesaleAverageSlope"
        const val MEDIAN_RETAIL = "medianRetailNtdPerTaiJin"

        fun monthMedianKey(month: Int): String = "month%02dMedianRetailNtdPerTaiJin".format(month)
    }
}

object CandidateEstimatorFitter {
    private val mathContext = MathContext(16, RoundingMode.HALF_UP)

    fun fit(rows: List<HistoricalCalibrationRow>, family: EstimatorFamily): FittedEstimator {
        require(rows.isNotEmpty()) { "At least one calibration row is required" }
        return when (family) {
            EstimatorFamily.LINEAR_CALIBRATION -> fitLinear(rows, logScale = false)
            EstimatorFamily.LOG_LINEAR_CALIBRATION -> fitLinear(rows, logScale = true)
            EstimatorFamily.SEASONAL_BASELINE -> fitSeasonal(rows)
            EstimatorFamily.WHOLESALE_MULTIPLIER_REFERENCE ->
                error("Temporary wholesale multiplier is not fitted from calibration rows")
        }
    }

    private fun fitLinear(
        rows: List<HistoricalCalibrationRow>,
        logScale: Boolean,
    ): FittedEstimator {
        val points = rows.map { row ->
            val x = row.features.wholesaleAverageNtdPerKg
            val y = row.target.retailNtdPerTaiJin
            require(x > BigDecimal.ZERO && y > BigDecimal.ZERO) {
                "Linear calibration requires positive wholesale and retail values"
            }
            if (logScale) {
                BigDecimal.valueOf(ln(x.toDouble())) to BigDecimal.valueOf(ln(y.toDouble()))
            } else {
                x to y
            }
        }
        val count = BigDecimal(points.size)
        val meanX = points.sumOf { it.first }.divide(count, mathContext)
        val meanY = points.sumOf { it.second }.divide(count, mathContext)
        val numerator = points.fold(BigDecimal.ZERO) { total, (x, y) ->
            total + (x - meanX).multiply(y - meanY, mathContext)
        }
        val denominator = points.fold(BigDecimal.ZERO) { total, (x, _) ->
            total + (x - meanX).pow(2, mathContext)
        }
        val slope = if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal.ZERO
        } else {
            numerator.divide(denominator, mathContext)
        }
        val intercept = meanY - slope.multiply(meanX, mathContext)
        return if (logScale) {
            FittedEstimator(
                family = EstimatorFamily.LOG_LINEAR_CALIBRATION,
                parameters = linkedMapOf(
                    FittedEstimator.LOG_INTERCEPT to intercept.normalized(),
                    FittedEstimator.LOG_WHOLESALE_AVERAGE_SLOPE to slope.normalized(),
                ),
            )
        } else {
            FittedEstimator(
                family = EstimatorFamily.LINEAR_CALIBRATION,
                parameters = linkedMapOf(
                    FittedEstimator.INTERCEPT to intercept.normalized(),
                    FittedEstimator.WHOLESALE_AVERAGE_SLOPE to slope.normalized(),
                ),
            )
        }
    }

    private fun fitSeasonal(rows: List<HistoricalCalibrationRow>): FittedEstimator {
        val parameters = linkedMapOf(
            FittedEstimator.MEDIAN_RETAIL to rows.map { it.target.retailNtdPerTaiJin }.median(),
        )
        rows.groupBy { it.features.monthOfYear }
            .toSortedMap()
            .forEach { (month, monthRows) ->
                parameters[FittedEstimator.monthMedianKey(month)] =
                    monthRows.map { it.target.retailNtdPerTaiJin }.median()
            }
        return FittedEstimator(EstimatorFamily.SEASONAL_BASELINE, parameters)
    }

    private fun List<BigDecimal>.median(): BigDecimal {
        val ordered = sorted()
        val midpoint = ordered.size / 2
        return if (ordered.size % 2 == 1) {
            ordered[midpoint]
        } else {
            (ordered[midpoint - 1] + ordered[midpoint])
                .divide(BigDecimal("2"), mathContext)
        }.normalized()
    }
}

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

            val model = CandidateEstimatorFitter.fit(training, family)
            val predicted = model.predict(validation.features)
            if (predicted <= BigDecimal.ZERO) {
                exclusions += "${validation.observedOn}:NON_POSITIVE_PREDICTION"
                return@forEach
            }
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

    private fun List<BigDecimal>.averageDecimal(): BigDecimal =
        fold(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal(size), 8, RoundingMode.HALF_UP)
}

private fun BigDecimal.normalized(): BigDecimal =
    setScale(8, RoundingMode.HALF_UP).stripTrailingZeros()
