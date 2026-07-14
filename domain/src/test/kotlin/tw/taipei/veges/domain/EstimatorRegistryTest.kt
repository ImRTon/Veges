package tw.taipei.veges.domain

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EstimatorRegistryTest {
    private val features = EstimatorFeatureSet(
        wholesaleAverageNtdPerKg = BigDecimal("40"),
        wholesaleLowerNtdPerKg = BigDecimal("30"),
        wholesaleUpperNtdPerKg = BigDecimal("50"),
        transactionVolumeKg = BigDecimal("100"),
        monthOfYear = 7,
        marketBasis = MarketBasis.TAIPEI_COMBINED,
    )

    @Test
    fun rejectsArtifactBelowThirtyPairedPeriods() {
        val result = EstimatorRegistry().estimate(artifact(periods = 18), conceptId(), MarketBasis.TAIPEI_COMBINED, date(), listOf(date()), features)

        assertEquals(UnavailableReason.MODEL_INELIGIBLE, (result as RuntimeEstimateResult.Unavailable).reason)
    }

    @Test
    fun rejectsChecksumMismatchBeforeEvaluation() {
        val result = EstimatorRegistry().estimate(artifact(checksum = "bad"), conceptId(), MarketBasis.TAIPEI_COMBINED, date(), listOf(date()), features)

        assertEquals(UnavailableReason.ARTIFACT_INVALID, (result as RuntimeEstimateResult.Unavailable).reason)
    }

    @Test
    fun emitsPointEstimateWithMandatoryDisclosure() {
        val result = EstimatorRegistry().estimate(artifact(), conceptId(), MarketBasis.TAIPEI_COMBINED, date(), listOf(date()), features)

        val estimate = (result as RuntimeEstimateResult.Available).estimate
        assertEquals(BigDecimal("52.00"), estimate.point?.amount)
        assertEquals(EstimateDisclosure.SHORT_TAG, estimate.disclosure.shortTag)
    }

    @Test
    fun doesNotSilentlySubstituteUnsupportedBasis() {
        val result = EstimatorRegistry().estimate(artifact(), conceptId(), MarketBasis.TAIPEI_FIRST, date(), listOf(date()), features)

        assertTrue(result is RuntimeEstimateResult.Unavailable)
        assertEquals(UnavailableReason.MODEL_INELIGIBLE, (result as RuntimeEstimateResult.Unavailable).reason)
    }

    private fun artifact(
        periods: Int = 30,
        checksum: String = "checksum",
    ) = KnownEstimatorArtifact(
        modelVersion = "seasonal-median-v1",
        family = EstimatorFamily.SEASONAL_BASELINE,
        supportedBases = setOf(MarketBasis.TAIPEI_COMBINED),
        calibrationCutoff = date(),
        parameters = mapOf("medianRetailNtdPerTaiJin" to BigDecimal("52")),
        eligibility = EstimatorEligibility(periods, pointEligible = true, intervalEligible = false, confidenceEligible = false, artifactIntegrityValid = true),
        checksum = checksum,
        computedChecksum = "checksum",
    )

    private fun conceptId() = ProduceConceptId("vegetable.cabbage")

    private fun date() = LocalDate.parse("2026-07-14")
}
