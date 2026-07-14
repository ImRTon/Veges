package tw.taipei.veges.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

data class EstimatorEligibility(
    val pairedCalibrationPeriods: Int,
    val minimumPairedCalibrationPeriods: Int = 30,
    val pointEligible: Boolean,
    val intervalEligible: Boolean,
    val confidenceEligible: Boolean,
    val artifactIntegrityValid: Boolean,
)

data class KnownEstimatorArtifact(
    val modelVersion: String,
    val family: EstimatorFamily,
    val supportedBases: Set<MarketBasis>,
    val calibrationCutoff: LocalDate,
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
    ): BigDecimal = parameters["medianRetailNtdPerTaiJin"]
        ?: error("Missing medianRetailNtdPerTaiJin parameter")
}

sealed interface RuntimeEstimateResult {
    data class Available(val estimate: Estimate) : RuntimeEstimateResult
    data class Unavailable(val reason: UnavailableReason) : RuntimeEstimateResult
}

class EstimatorRegistry(
    private val evaluators: Set<KnownEstimatorEvaluator> = setOf(MedianBaselineEvaluator()),
    private val clock: Clock = Clock.systemUTC(),
) {
    fun estimate(
        artifact: KnownEstimatorArtifact,
        conceptId: ProduceConceptId,
        basis: MarketBasis,
        sourceDate: LocalDate,
        sourceDates: List<LocalDate>,
        features: EstimatorFeatureSet,
    ): RuntimeEstimateResult {
        if (!artifact.eligibility.artifactIntegrityValid || artifact.checksum != artifact.computedChecksum) {
            return RuntimeEstimateResult.Unavailable(UnavailableReason.ARTIFACT_INVALID)
        }
        if (artifact.eligibility.pairedCalibrationPeriods < artifact.eligibility.minimumPairedCalibrationPeriods) {
            return RuntimeEstimateResult.Unavailable(UnavailableReason.MODEL_INELIGIBLE)
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
