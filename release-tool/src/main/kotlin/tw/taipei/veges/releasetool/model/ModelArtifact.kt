package tw.taipei.veges.releasetool.model

import java.math.BigDecimal
import kotlinx.serialization.Serializable
import tw.taipei.veges.domain.EstimatorFamily
import tw.taipei.veges.domain.CALIBRATED_ESTIMATOR_FAMILIES
import tw.taipei.veges.releasetool.artifact.ArtifactCodec

@Serializable
data class ModelArtifactBundle(
    val schemaVersion: Int,
    val artifactVersion: String,
    val generatedAt: String,
    val policyVersion: String,
    val dataCutoff: String?,
    val sourceFiles: List<SourceFileProvenance>,
    val thresholds: ModelThresholdArtifact,
    val entries: List<ModelArtifactEntry>,
    val exclusions: List<ModelArtifactExclusion>,
    val artifactChecksum: String,
)

@Serializable
data class ModelThresholdArtifact(
    val minimumPairedCalibrationPeriods: Int,
    val minimumCoverageRatio: String,
    val maximumRecencyDays: Int,
    val maximumMeanAbsoluteErrorNtdPerTaiJin: String,
    val maximumRootMeanSquaredErrorNtdPerTaiJin: String,
    val wholesaleFreshnessHours: Int,
    val calibrationArtifactLifetimeDays: Int,
    val intervalEnabled: Boolean,
    val confidenceEnabled: Boolean,
)

@Serializable
data class ModelArtifactEntry(
    val conceptId: String,
    val basis: String,
    val modelVersion: String,
    val family: String,
    val calibrationCutoff: String,
    val pairedCalibrationPeriods: Int,
    val coverageRatio: String,
    val backtestPeriods: Int,
    val meanAbsoluteError: String,
    val rootMeanSquaredError: String,
    val parameters: Map<String, String>,
    val pointEligible: Boolean,
    val intervalEligible: Boolean,
    val confidenceEligible: Boolean,
)

@Serializable
data class ModelArtifactExclusion(
    val conceptId: String,
    val basis: String,
    val reasonCodes: List<String>,
)

object ModelArtifactGenerator {
    fun generate(
        report: HistoricalAuditReport,
        policy: EstimationPublicationPolicy,
        artifactVersion: String,
    ): ModelArtifactBundle {
        policy.validate()
        require(report.schemaVersion == 1) { "Unsupported historical audit schema" }
        require(report.thresholdStatus == "APPROVED_POLICY_ENFORCED") {
            "Historical audit did not enforce an approved policy"
        }
        require(artifactVersion.isNotBlank()) { "Artifact version is required" }

        val entries = mutableListOf<ModelArtifactEntry>()
        val exclusions = mutableListOf<ModelArtifactExclusion>()
        report.result.groupBy { it.conceptId to it.basis }
            .toSortedMap(compareBy<Pair<String, String>>({ it.first }, { it.second }))
            .forEach { (identity, candidates) ->
                val actualFamilies = candidates.map { it.family }.toSet()
                val expectedFamilies = CALIBRATED_ESTIMATOR_FAMILIES.map { it.name }.toSet()
                if (actualFamilies != expectedFamilies) {
                    exclusions += ModelArtifactExclusion(
                        conceptId = identity.first,
                        basis = identity.second,
                        reasonCodes = listOf("MISSING_FAMILY_SPECIFIC_BACKTEST"),
                    )
                    return@forEach
                }
                val eligible = candidates.filter { candidate ->
                    candidate.gate.pointEligible &&
                        candidate.meanAbsoluteError != null &&
                        candidate.rootMeanSquaredError != null &&
                        candidate.fittedParameters.isNotEmpty()
                }
                val selected = eligible.minWithOrNull(
                    compareBy<HistoricalConceptAudit>(
                        { BigDecimal(requireNotNull(it.rootMeanSquaredError)) },
                        { BigDecimal(requireNotNull(it.meanAbsoluteError)) },
                        { it.family },
                    ),
                )
                if (selected == null) {
                    exclusions += ModelArtifactExclusion(
                        conceptId = identity.first,
                        basis = identity.second,
                        reasonCodes = candidates.flatMap { candidate ->
                            candidate.exclusions.map { "${candidate.family}:$it" }
                        }.distinct().sorted(),
                    )
                    return@forEach
                }
                entries += ModelArtifactEntry(
                    conceptId = selected.conceptId,
                    basis = selected.basis,
                    modelVersion = "$artifactVersion:${selected.conceptId}:${selected.basis}:${selected.family}",
                    family = selected.family,
                    calibrationCutoff = requireNotNull(selected.calibrationCutoff),
                    pairedCalibrationPeriods = selected.calibrationRows,
                    coverageRatio = selected.gate.coverageRatio,
                    backtestPeriods = selected.backtestPeriods,
                    meanAbsoluteError = requireNotNull(selected.meanAbsoluteError),
                    rootMeanSquaredError = requireNotNull(selected.rootMeanSquaredError),
                    parameters = selected.fittedParameters.toSortedMap(),
                    pointEligible = true,
                    intervalEligible = false,
                    confidenceEligible = false,
                )
            }

        val unsigned = ModelArtifactBundle(
            schemaVersion = 1,
            artifactVersion = artifactVersion,
            generatedAt = report.generatedAt,
            policyVersion = policy.policyVersion,
            dataCutoff = report.result.mapNotNull { it.calibrationCutoff }.maxOrNull(),
            sourceFiles = (report.wholesaleFiles + report.retailFiles)
                .sortedWith(compareBy({ it.source }, { it.fileName }, { it.sha256 })),
            thresholds = ModelThresholdArtifact(
                minimumPairedCalibrationPeriods = policy.minimumPairedCalibrationPeriods,
                minimumCoverageRatio = policy.minimumCoverageRatio,
                maximumRecencyDays = policy.maximumRecencyDays,
                maximumMeanAbsoluteErrorNtdPerTaiJin = policy.maximumMeanAbsoluteErrorNtdPerTaiJin,
                maximumRootMeanSquaredErrorNtdPerTaiJin = policy.maximumRootMeanSquaredErrorNtdPerTaiJin,
                wholesaleFreshnessHours = policy.wholesaleFreshnessHours,
                calibrationArtifactLifetimeDays = policy.calibrationArtifactLifetimeDays,
                intervalEnabled = false,
                confidenceEnabled = false,
            ),
            entries = entries,
            exclusions = exclusions,
            artifactChecksum = "",
        )
        return unsigned.copy(artifactChecksum = ArtifactCodec.modelArtifactChecksum(unsigned))
    }

    fun validate(artifact: ModelArtifactBundle) {
        require(artifact.schemaVersion == 1) { "Unsupported model artifact schema" }
        require(artifact.artifactVersion.isNotBlank() && artifact.policyVersion.isNotBlank()) {
            "Model artifact identity is required"
        }
        require(!artifact.thresholds.intervalEnabled && !artifact.thresholds.confidenceEnabled) {
            "MVP model artifact must disable interval and confidence outputs"
        }
        require(artifact.entries.all { it.pointEligible && !it.intervalEligible && !it.confidenceEligible }) {
            "Model artifact contains unsupported output gates"
        }
        require(artifact.entries.map { it.conceptId to it.basis }.distinct().size == artifact.entries.size) {
            "Model artifact contains duplicate concept/basis entries"
        }
        require(artifact.artifactChecksum == ArtifactCodec.modelArtifactChecksum(artifact)) {
            "Model artifact checksum mismatch"
        }
    }
}
