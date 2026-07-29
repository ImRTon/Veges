package tw.taipei.veges.releasetool.model

import java.math.BigDecimal
import kotlinx.serialization.Serializable
import tw.taipei.veges.domain.EstimatorFamily
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.releasetool.artifact.ArtifactCodec

@Serializable
data class TemporaryFactorPolicy(
    val schemaVersion: Int,
    val policyVersion: String,
    val approvedAt: String,
    val approvedBy: String,
    val wholesaleToMarketFactor: String,
    val kilogramsPerTaiJin: String,
    val wholesaleFreshnessHours: Int,
    val supportedBases: List<String>,
    val intervalEnabled: Boolean,
    val confidenceEnabled: Boolean,
    val disclosure: String,
)

@Serializable
data class TemporaryFactorArtifact(
    val schemaVersion: Int,
    val artifactVersion: String,
    val generatedAt: String,
    val policyVersion: String,
    val approvedAt: String,
    val approvedBy: String,
    val family: String,
    val wholesaleToMarketFactor: String,
    val kilogramsPerTaiJin: String,
    val sourceUnit: String,
    val outputUnit: String,
    val formula: String,
    val wholesaleFreshnessHours: Int,
    val supportedBases: List<String>,
    val intervalEnabled: Boolean,
    val confidenceEnabled: Boolean,
    val disclosure: String,
    val artifactChecksum: String,
)

object TemporaryFactorArtifactGenerator {
    private const val EXPECTED_FACTOR = "2.0"
    private const val EXPECTED_KILOGRAMS_PER_TAI_JIN = "0.6"
    private val expectedBases = MarketBasis.entries.map { it.name }.sorted()

    fun generate(
        policy: TemporaryFactorPolicy,
        artifactVersion: String,
        generatedAt: String,
    ): TemporaryFactorArtifact {
        validatePolicy(policy)
        require(artifactVersion.isNotBlank()) { "Artifact version is required" }
        require(generatedAt.isNotBlank()) { "Generated-at timestamp is required" }
        val unsigned = TemporaryFactorArtifact(
            schemaVersion = 1,
            artifactVersion = artifactVersion,
            generatedAt = generatedAt,
            policyVersion = policy.policyVersion,
            approvedAt = policy.approvedAt,
            approvedBy = policy.approvedBy,
            family = EstimatorFamily.WHOLESALE_MULTIPLIER_REFERENCE.name,
            wholesaleToMarketFactor = policy.wholesaleToMarketFactor,
            kilogramsPerTaiJin = policy.kilogramsPerTaiJin,
            sourceUnit = "NTD_PER_KILOGRAM",
            outputUnit = "NTD_PER_TAI_JIN",
            formula = "(wholesale NTD/kg × 0.6 kg/台斤) × 2.0",
            wholesaleFreshnessHours = policy.wholesaleFreshnessHours,
            supportedBases = policy.supportedBases.sorted(),
            intervalEnabled = false,
            confidenceEnabled = false,
            disclosure = policy.disclosure,
            artifactChecksum = "",
        )
        return unsigned.copy(
            artifactChecksum = ArtifactCodec.temporaryFactorArtifactChecksum(unsigned),
        )
    }

    fun validate(artifact: TemporaryFactorArtifact) {
        require(artifact.schemaVersion == 1) { "Unsupported temporary factor artifact schema" }
        require(artifact.artifactVersion.isNotBlank() && artifact.policyVersion.isNotBlank()) {
            "Temporary factor artifact identity is required"
        }
        require(artifact.family == EstimatorFamily.WHOLESALE_MULTIPLIER_REFERENCE.name) {
            "Unexpected estimator family"
        }
        require(BigDecimal(artifact.wholesaleToMarketFactor).compareTo(BigDecimal(EXPECTED_FACTOR)) == 0) {
            "Temporary wholesale-to-market factor must be exactly 2.0"
        }
        require(BigDecimal(artifact.kilogramsPerTaiJin).compareTo(BigDecimal(EXPECTED_KILOGRAMS_PER_TAI_JIN)) == 0) {
            "Kilograms per tai-jin must be exactly 0.6"
        }
        require(artifact.sourceUnit == "NTD_PER_KILOGRAM" && artifact.outputUnit == "NTD_PER_TAI_JIN") {
            "Temporary factor artifact units are invalid"
        }
        require(artifact.wholesaleFreshnessHours == 36) {
            "Temporary factor artifact freshness must be 36 hours"
        }
        require(artifact.supportedBases.distinct().sorted() == expectedBases) {
            "Temporary factor artifact must explicitly support every Taipei basis"
        }
        require(!artifact.intervalEnabled && !artifact.confidenceEnabled) {
            "Temporary factor artifact must disable interval and confidence"
        }
        require(artifact.formula == "(wholesale NTD/kg × 0.6 kg/台斤) × 2.0") {
            "Temporary factor artifact formula is invalid"
        }
        require(artifact.artifactChecksum == ArtifactCodec.temporaryFactorArtifactChecksum(artifact)) {
            "Temporary factor artifact checksum mismatch"
        }
    }

    private fun validatePolicy(policy: TemporaryFactorPolicy) {
        require(policy.schemaVersion == 1) { "Unsupported temporary factor policy schema" }
        require(policy.policyVersion.isNotBlank() && policy.approvedAt.isNotBlank() && policy.approvedBy.isNotBlank()) {
            "Temporary factor approval metadata is required"
        }
        require(BigDecimal(policy.wholesaleToMarketFactor).compareTo(BigDecimal(EXPECTED_FACTOR)) == 0) {
            "Temporary wholesale-to-market factor must be exactly 2.0"
        }
        require(BigDecimal(policy.kilogramsPerTaiJin).compareTo(BigDecimal(EXPECTED_KILOGRAMS_PER_TAI_JIN)) == 0) {
            "Kilograms per tai-jin must be exactly 0.6"
        }
        require(policy.wholesaleFreshnessHours == 36) { "Wholesale freshness must be 36 hours" }
        require(policy.supportedBases.distinct().sorted() == expectedBases) {
            "Policy must approve every Taipei basis"
        }
        require(!policy.intervalEnabled && !policy.confidenceEnabled) {
            "Temporary factor policy cannot enable interval or confidence"
        }
        require(policy.disclosure.isNotBlank()) { "Temporary factor disclosure is required" }
    }
}
