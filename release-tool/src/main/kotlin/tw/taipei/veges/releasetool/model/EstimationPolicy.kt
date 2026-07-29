package tw.taipei.veges.releasetool.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.serialization.Serializable
import tw.taipei.veges.domain.BacktestMetrics

@Serializable
data class EstimationPublicationPolicy(
    val schemaVersion: Int,
    val policyVersion: String,
    val approvedAt: String,
    val approvedBy: String,
    val minimumValidObservationDays: Int,
    val minimumPairedCalibrationPeriods: Int,
    val minimumCoverageRatio: String,
    val maximumRecencyDays: Int,
    val maximumMeanAbsoluteErrorNtdPerTaiJin: String,
    val maximumRootMeanSquaredErrorNtdPerTaiJin: String,
    val wholesaleFreshnessHours: Int,
    val calibrationArtifactLifetimeDays: Int,
    val intervalEnabled: Boolean,
    val confidenceEnabled: Boolean,
    val publishCandidates: Boolean,
    val requireExplicitAmbiguityChoice: Boolean,
    val estimateDisclosureShortTag: String,
    val estimateDisclosureFullLabel: String,
    val notes: String,
) {
    val coverageThreshold: BigDecimal get() = BigDecimal(minimumCoverageRatio)
    val maeThreshold: BigDecimal get() = BigDecimal(maximumMeanAbsoluteErrorNtdPerTaiJin)
    val rmseThreshold: BigDecimal get() = BigDecimal(maximumRootMeanSquaredErrorNtdPerTaiJin)

    fun validate() {
        require(schemaVersion == 1) { "Unsupported publication policy schema" }
        require(policyVersion.isNotBlank() && approvedAt.isNotBlank() && approvedBy.isNotBlank()) {
            "Approved publication policy metadata is required"
        }
        require(minimumValidObservationDays > 0) { "Catalog observation minimum must be positive" }
        require(minimumPairedCalibrationPeriods > 0) { "Calibration period minimum must be positive" }
        require(coverageThreshold > BigDecimal.ZERO && coverageThreshold <= BigDecimal.ONE) {
            "Coverage threshold must be within (0, 1]"
        }
        require(maximumRecencyDays >= 0) { "Maximum recency must not be negative" }
        require(maeThreshold > BigDecimal.ZERO && rmseThreshold > BigDecimal.ZERO) {
            "Point-error thresholds must be positive"
        }
        require(wholesaleFreshnessHours > 0 && calibrationArtifactLifetimeDays > 0) {
            "Freshness thresholds must be positive"
        }
        require(!intervalEnabled && !confidenceEnabled) {
            "MVP policy must keep interval and confidence outputs disabled"
        }
        require(estimateDisclosureShortTag == "估算") { "Estimate short disclosure must remain 估算" }
        require(estimateDisclosureFullLabel == "Taipei retail reference estimate") {
            "Estimate full disclosure is invalid"
        }
    }
}

@Serializable
data class EligibilityGateResult(
    val pairedCalibrationPeriods: Int,
    val retailTargetPeriods: Int,
    val coverageRatio: String,
    val recencyDays: Long,
    val pointEligible: Boolean,
    val intervalEligible: Boolean,
    val confidenceEligible: Boolean,
    val exclusionReasons: List<String>,
)

object EstimationGateEvaluator {
    fun evaluate(
        policy: EstimationPublicationPolicy,
        pairedCalibrationPeriods: Int,
        retailTargetPeriods: Int,
        calibrationCutoff: LocalDate,
        generatedOn: LocalDate,
        metrics: BacktestMetrics,
    ): EligibilityGateResult {
        policy.validate()
        val coverage = if (retailTargetPeriods <= 0) {
            BigDecimal.ZERO
        } else {
            BigDecimal(pairedCalibrationPeriods)
                .divide(BigDecimal(retailTargetPeriods), 8, RoundingMode.HALF_UP)
        }
        val recencyDays = ChronoUnit.DAYS.between(calibrationCutoff, generatedOn)
        val reasons = buildList {
            if (pairedCalibrationPeriods < policy.minimumPairedCalibrationPeriods) {
                add("INSUFFICIENT_PAIRED_CALIBRATION_PERIODS")
            }
            if (coverage < policy.coverageThreshold) {
                add("PAIRED_COVERAGE_BELOW_MINIMUM")
            }
            if (recencyDays < 0) {
                add("CALIBRATION_CUTOFF_AFTER_GENERATION")
            } else if (recencyDays > policy.maximumRecencyDays) {
                add("CALIBRATION_TOO_OLD_AT_GENERATION")
            }
            val mae = metrics.meanAbsoluteError
            if (mae == null) {
                add("MISSING_MAE")
            } else if (mae > policy.maeThreshold) {
                add("MAE_ABOVE_MAXIMUM")
            }
            val rmse = metrics.rootMeanSquaredError
            if (rmse == null) {
                add("MISSING_RMSE")
            } else if (rmse > policy.rmseThreshold) {
                add("RMSE_ABOVE_MAXIMUM")
            }
        }.distinct().sorted()
        val pointEligible = reasons.isEmpty()
        return EligibilityGateResult(
            pairedCalibrationPeriods = pairedCalibrationPeriods,
            retailTargetPeriods = retailTargetPeriods,
            coverageRatio = coverage.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString(),
            recencyDays = recencyDays,
            pointEligible = pointEligible,
            intervalEligible = pointEligible && policy.intervalEnabled && metrics.intervalCoverage != null,
            confidenceEligible = false,
            exclusionReasons = reasons,
        )
    }
}
