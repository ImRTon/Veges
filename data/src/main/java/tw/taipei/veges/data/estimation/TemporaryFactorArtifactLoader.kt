package tw.taipei.veges.data.estimation

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tw.taipei.veges.domain.EstimatorEligibility
import tw.taipei.veges.domain.EstimatorFamily
import tw.taipei.veges.domain.KnownEstimatorArtifact
import tw.taipei.veges.domain.MarketBasis

@Serializable
internal data class TemporaryFactorArtifactDto(
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

class TemporaryFactorArtifactLoader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun load(): KnownEstimatorArtifact {
        val raw = context.assets.open(DEFAULT_ASSET).bufferedReader().use { it.readText() }
        val dto = strictJson.decodeFromString<TemporaryFactorArtifactDto>(raw)
        val computedChecksum = checksum(dto)
        validate(dto, computedChecksum)
        val approvedOn = LocalDate.parse(dto.approvedAt)
        return KnownEstimatorArtifact(
            modelVersion = dto.artifactVersion,
            family = EstimatorFamily.valueOf(dto.family),
            supportedBases = dto.supportedBases.map(MarketBasis::valueOf).toSet(),
            calibrationCutoff = approvedOn,
            approvedOn = approvedOn,
            formula = dto.formula,
            parameters = mapOf(
                "wholesaleToMarketFactor" to BigDecimal(dto.wholesaleToMarketFactor),
            ),
            eligibility = EstimatorEligibility(
                pairedCalibrationPeriods = 0,
                minimumPairedCalibrationPeriods = 0,
                pointEligible = true,
                intervalEligible = dto.intervalEnabled,
                confidenceEligible = dto.confidenceEnabled,
                artifactIntegrityValid = dto.artifactChecksum == computedChecksum,
                wholesaleFreshnessHours = dto.wholesaleFreshnessHours.toLong(),
            ),
            checksum = dto.artifactChecksum,
            computedChecksum = computedChecksum,
        )
    }

    private fun validate(dto: TemporaryFactorArtifactDto, computedChecksum: String) {
        require(dto.schemaVersion == 1)
        require(dto.family == EstimatorFamily.WHOLESALE_MULTIPLIER_REFERENCE.name)
        require(BigDecimal(dto.wholesaleToMarketFactor).compareTo(BigDecimal("2.0")) == 0)
        require(BigDecimal(dto.kilogramsPerTaiJin).compareTo(BigDecimal("0.6")) == 0)
        require(dto.sourceUnit == "NTD_PER_KILOGRAM")
        require(dto.outputUnit == "NTD_PER_TAI_JIN")
        require(dto.formula == "(wholesale NTD/kg × 0.6 kg/台斤) × 2.0")
        require(dto.wholesaleFreshnessHours == 36)
        require(dto.supportedBases.distinct().sorted() == MarketBasis.entries.map { it.name }.sorted())
        require(!dto.intervalEnabled && !dto.confidenceEnabled)
        require(dto.artifactChecksum == computedChecksum)
    }

    private fun checksum(dto: TemporaryFactorArtifactDto): String {
        val canonical = dto.copy(
            supportedBases = dto.supportedBases.distinct().sorted(),
            artifactChecksum = "",
        )
        val bytes = checksumJson.encodeToString(canonical).toByteArray(StandardCharsets.UTF_8)
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    companion object {
        const val DEFAULT_ASSET = "model/mvp-temporary-factor-artifact.json"

        private val strictJson = Json {
            encodeDefaults = true
            explicitNulls = false
            ignoreUnknownKeys = false
            isLenient = false
        }
        private val checksumJson = Json {
            encodeDefaults = true
            explicitNulls = false
            ignoreUnknownKeys = false
            isLenient = false
            prettyPrint = false
        }
    }
}
