package tw.taipei.veges.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import kotlin.math.exp
import kotlin.math.ln

data class EstimatorEligibility(
    val pairedCalibrationPeriods: Int,
    val minimumPairedCalibrationPeriods: Int = 18,
    val calibrationArtifactLifetimeDays: Long = 62,
    val pointEligible: Boolean,
    val intervalEligible: Boolean,
    val confidenceEligible: Boolean,
    val artifactIntegrityValid: Boolean,
    val wholesaleFreshnessHours: Long = 36,
)

data class KnownEstimatorArtifact(
    val modelVersion: String,
    val family: EstimatorFamily,
    val supportedBases: Set<MarketBasis>,
    val calibrationCutoff: LocalDate,
    val approvedOn: LocalDate = calibrationCutoff,
    val formula: String = "CALIBRATED_MODEL",
    val parameters: Map<String, BigDecimal>,
    val eligibility: EstimatorEligibility,
    val checksum: String,
    val computedChecksum: String,
)

interface KnownEstimatorEvaluator {
    val family: EstimatorFamily

    fun evaluate(
        parameters: Map<String, BigDecimal>,
        features: EstimatorFeatureSet,
    ): BigDecimal
}

class MedianBaselineEvaluator : KnownEstimatorEvaluator {
    override val family = EstimatorFamily.SEASONAL_BASELINE

    override fun evaluate(
        parameters: Map<String, BigDecimal>,
        features: EstimatorFeatureSet,
    ): BigDecimal = parameters["month%02dMedianRetailNtdPerTaiJin".format(features.monthOfYear)]
        ?: parameters["medianRetailNtdPerTaiJin"]
        ?: error("Missing seasonal median parameters")
}

class LinearCalibrationEvaluator : KnownEstimatorEvaluator {
    override val family = EstimatorFamily.LINEAR_CALIBRATION

    override fun evaluate(
        parameters: Map<String, BigDecimal>,
        features: EstimatorFeatureSet,
    ): BigDecimal =
        parameters.required("intercept") +
            parameters.required("wholesaleAverageSlope") * features.wholesaleAverageNtdPerKg
}

class LogLinearCalibrationEvaluator : KnownEstimatorEvaluator {
    override val family = EstimatorFamily.LOG_LINEAR_CALIBRATION

    override fun evaluate(
        parameters: Map<String, BigDecimal>,
        features: EstimatorFeatureSet,
    ): BigDecimal {
        val logPrediction = parameters.required("logIntercept").toDouble() +
            parameters.required("logWholesaleAverageSlope").toDouble() *
            ln(features.wholesaleAverageNtdPerKg.toDouble())
        return BigDecimal.valueOf(exp(logPrediction))
    }
}

class WholesaleMultiplierReferenceEvaluator : KnownEstimatorEvaluator {
    override val family = EstimatorFamily.WHOLESALE_MULTIPLIER_REFERENCE

    override fun evaluate(
        parameters: Map<String, BigDecimal>,
        features: EstimatorFeatureSet,
    ): BigDecimal {
        val factor = parameters.required("wholesaleToMarketFactor")
        require(factor.compareTo(BigDecimal("2.0")) == 0) {
            "Temporary wholesale-to-market factor must be exactly 2.0"
        }
        return EstimationMath.ntdPerKilogramToNtdPerTaiJin(features.wholesaleAverageNtdPerKg)
            .multiply(factor)
    }
}

sealed interface RuntimeEstimateResult {
    data class Available(val estimate: Estimate) : RuntimeEstimateResult
    data class Unavailable(val reason: UnavailableReason) : RuntimeEstimateResult
}

class EstimatorRegistry(
    private val evaluators: Set<KnownEstimatorEvaluator> = setOf(
        MedianBaselineEvaluator(),
        LinearCalibrationEvaluator(),
        LogLinearCalibrationEvaluator(),
        WholesaleMultiplierReferenceEvaluator(),
    ),
    private val clock: Clock = Clock.systemUTC(),
) {
    fun estimate(
        artifact: KnownEstimatorArtifact,
        conceptId: ProduceConceptId,
        basis: MarketBasis,
        sourceDate: LocalDate,
        sourceDates: List<LocalDate>,
        features: EstimatorFeatureSet,
        sourceRetrievedAt: Instant? = null,
    ): RuntimeEstimateResult {
        if (!artifact.eligibility.artifactIntegrityValid || artifact.checksum != artifact.computedChecksum) {
            return RuntimeEstimateResult.Unavailable(UnavailableReason.ARTIFACT_INVALID)
        }
        if (!artifact.eligibility.pointEligible) {
            return RuntimeEstimateResult.Unavailable(UnavailableReason.MODEL_INELIGIBLE)
        }
        if (artifact.eligibility.intervalEligible || artifact.eligibility.confidenceEligible) {
            return RuntimeEstimateResult.Unavailable(UnavailableReason.ARTIFACT_INVALID)
        }
        if (artifact.family == EstimatorFamily.WHOLESALE_MULTIPLIER_REFERENCE) {
            val retrievedAt = sourceRetrievedAt
                ?: return RuntimeEstimateResult.Unavailable(UnavailableReason.STALE_INPUT)
            val age = Duration.between(retrievedAt, clock.instant())
            if (age.isNegative || age > Duration.ofHours(artifact.eligibility.wholesaleFreshnessHours)) {
                return RuntimeEstimateResult.Unavailable(UnavailableReason.STALE_INPUT)
            }
            if (artifact.approvedOn != artifact.calibrationCutoff ||
                artifact.formula != "(wholesale NTD/kg × 0.6 kg/台斤) × 2.0"
            ) {
                return RuntimeEstimateResult.Unavailable(UnavailableReason.ARTIFACT_INVALID)
            }
        } else {
            if (artifact.eligibility.pairedCalibrationPeriods < artifact.eligibility.minimumPairedCalibrationPeriods) {
                return RuntimeEstimateResult.Unavailable(UnavailableReason.MODEL_INELIGIBLE)
            }
            if (LocalDate.now(clock).isAfter(
                    artifact.calibrationCutoff.plusDays(artifact.eligibility.calibrationArtifactLifetimeDays),
                )
            ) {
                return RuntimeEstimateResult.Unavailable(UnavailableReason.STALE_INPUT)
            }
        }
        if (basis !in artifact.supportedBases) {
            return RuntimeEstimateResult.Unavailable(UnavailableReason.MODEL_INELIGIBLE)
        }
        val evaluator = evaluators.firstOrNull { it.family == artifact.family }
            ?: return RuntimeEstimateResult.Unavailable(UnavailableReason.UNSUPPORTED_MODEL)
        val point = runCatching { evaluator.evaluate(artifact.parameters, features) }
            .getOrNull()
            ?.takeIf { it > BigDecimal.ZERO }
            ?: return RuntimeEstimateResult.Unavailable(UnavailableReason.INVALID_SOURCE_DATA)

        return RuntimeEstimateResult.Available(
            Estimate(
                conceptId = conceptId,
                basis = basis,
                modelVersion = artifact.modelVersion,
                wholesaleSourceDates = sourceDates,
                calibrationCutoff = artifact.calibrationCutoff,
                estimatorApprovedOn = artifact.approvedOn,
                formula = artifact.formula,
                calculatedAt = clock.instant(),
                point = ScaledPrice(point.setScale(2, RoundingMode.HALF_UP), PriceUnit.NTD_PER_TAI_JIN),
                intervalLower = null,
                intervalUpper = null,
                confidence = null,
                unavailableReason = null,
            ),
        )
    }
}

private fun Map<String, BigDecimal>.required(name: String): BigDecimal =
    requireNotNull(this[name]) { "Missing estimator parameter $name" }
