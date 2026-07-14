package tw.taipei.veges.domain

import java.math.BigDecimal
import java.time.LocalDate

enum class EstimatorFamily {
    LINEAR_CALIBRATION,
    LOG_LINEAR_CALIBRATION,
    SEASONAL_BASELINE,
}

data class EstimatorFeatureSet(
    val wholesaleAverageNtdPerKg: BigDecimal,
    val wholesaleLowerNtdPerKg: BigDecimal,
    val wholesaleUpperNtdPerKg: BigDecimal,
    val transactionVolumeKg: BigDecimal,
    val monthOfYear: Int,
    val marketBasis: MarketBasis,
)

data class CalibrationTarget(
    val observedOn: LocalDate,
    val retailNtdPerTaiJin: BigDecimal,
)

data class BacktestPeriodMetric(
    val predictionDate: LocalDate,
    val actual: BigDecimal,
    val predicted: BigDecimal,
    val absoluteError: BigDecimal,
    val intervalCovered: Boolean?,
)

data class BacktestMetrics(
    val periods: List<BacktestPeriodMetric>,
    val meanAbsoluteError: BigDecimal?,
    val rootMeanSquaredError: BigDecimal?,
    val intervalCoverage: BigDecimal?,
    val meanIntervalWidth: BigDecimal?,
)

data class EstimatorOutputGates(
    val pointEligible: Boolean,
    val intervalEligible: Boolean,
    val confidenceEligible: Boolean,
)

data class CalibrationArtifact(
    val schemaVersion: Int,
    val modelVersion: String,
    val family: EstimatorFamily,
    val supportedBases: List<MarketBasis>,
    val dataCutoff: LocalDate,
    val calibrationCutoff: LocalDate,
    val parameters: Map<String, BigDecimal>,
    val metrics: BacktestMetrics,
    val gates: EstimatorOutputGates,
    val checksum: String,
)

// Threshold values are intentionally not embedded here; they are release-audit inputs.
