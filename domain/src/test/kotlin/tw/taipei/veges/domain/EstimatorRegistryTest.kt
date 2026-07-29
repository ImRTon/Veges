package tw.taipei.veges.domain

import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EstimatorRegistryTest {
    private val clock = Clock.fixed(Instant.parse("2026-07-26T00:00:00Z"), ZoneOffset.UTC)
    private val registry = EstimatorRegistry(clock = clock)
    private val features = EstimatorFeatureSet(
        wholesaleAverageNtdPerKg = BigDecimal("40"),
        wholesaleLowerNtdPerKg = BigDecimal("30"),
        wholesaleUpperNtdPerKg = BigDecimal("50"),
        transactionVolumeKg = BigDecimal("100"),
        monthOfYear = 7,
        marketBasis = MarketBasis.TAIPEI_COMBINED,
    )

    @Test
    fun rejectsArtifactBelowEighteenPairedPeriods() {
        val result = registry.estimate(artifact(periods = 17), conceptId(), MarketBasis.TAIPEI_COMBINED, date(), listOf(date()), features)

        assertEquals(UnavailableReason.MODEL_INELIGIBLE, (result as RuntimeEstimateResult.Unavailable).reason)
    }

    @Test
    fun rejectsChecksumMismatchBeforeEvaluation() {
        val result = registry.estimate(artifact(checksum = "bad"), conceptId(), MarketBasis.TAIPEI_COMBINED, date(), listOf(date()), features)

        assertEquals(UnavailableReason.ARTIFACT_INVALID, (result as RuntimeEstimateResult.Unavailable).reason)
    }

    @Test
    fun emitsPointEstimateWithMandatoryDisclosure() {
        val result = registry.estimate(artifact(), conceptId(), MarketBasis.TAIPEI_COMBINED, date(), listOf(date()), features)

        val estimate = (result as RuntimeEstimateResult.Available).estimate
        assertEquals(BigDecimal("52.00"), estimate.point?.amount)
        assertEquals(EstimateDisclosure.SHORT_TAG, estimate.disclosure.shortTag)
    }

    @Test
    fun doesNotSilentlySubstituteUnsupportedBasis() {
        val result = registry.estimate(artifact(), conceptId(), MarketBasis.TAIPEI_FIRST, date(), listOf(date()), features)

        assertTrue(result is RuntimeEstimateResult.Unavailable)
        assertEquals(UnavailableReason.MODEL_INELIGIBLE, (result as RuntimeEstimateResult.Unavailable).reason)
    }

    @Test
    fun rejectsCalibrationArtifactAfterApprovedLifetime() {
        val stale = artifact(calibrationCutoff = LocalDate.parse("2026-05-24"))

        val result = registry.estimate(stale, conceptId(), MarketBasis.TAIPEI_COMBINED, date(), listOf(date()), features)

        assertEquals(UnavailableReason.STALE_INPUT, (result as RuntimeEstimateResult.Unavailable).reason)
    }

    @Test
    fun rejectsUnsupportedIntervalOrConfidenceGates() {
        val artifact = artifact(
            intervalEligible = true,
            confidenceEligible = true,
        )

        val result = registry.estimate(artifact, conceptId(), MarketBasis.TAIPEI_COMBINED, date(), listOf(date()), features)

        assertEquals(UnavailableReason.ARTIFACT_INVALID, (result as RuntimeEstimateResult.Unavailable).reason)
    }

    @Test
    fun temporaryMultiplierConvertsKilogramPriceThenDoublesIt() {
        val temporary = artifact(
            family = EstimatorFamily.WHOLESALE_MULTIPLIER_REFERENCE,
            parameters = mapOf("wholesaleToMarketFactor" to BigDecimal("2.0")),
            formula = "(wholesale NTD/kg × 0.6 kg/台斤) × 2.0",
        )

        val result = registry.estimate(
            artifact = temporary,
            conceptId = conceptId(),
            basis = MarketBasis.TAIPEI_COMBINED,
            sourceDate = date(),
            sourceDates = listOf(date()),
            features = features,
            sourceRetrievedAt = Instant.parse("2026-07-25T00:00:00Z"),
        )

        val estimate = (result as RuntimeEstimateResult.Available).estimate
        assertEquals(BigDecimal("48.00"), estimate.point?.amount)
        assertEquals(date(), estimate.estimatorApprovedOn)
        assertEquals("(wholesale NTD/kg × 0.6 kg/台斤) × 2.0", estimate.formula)
    }

    @Test
    fun temporaryMultiplierRejectsSourceOlderThanThirtySixHours() {
        val temporary = artifact(
            family = EstimatorFamily.WHOLESALE_MULTIPLIER_REFERENCE,
            parameters = mapOf("wholesaleToMarketFactor" to BigDecimal("2.0")),
            formula = "(wholesale NTD/kg × 0.6 kg/台斤) × 2.0",
        )

        val result = registry.estimate(
            temporary,
            conceptId(),
            MarketBasis.TAIPEI_COMBINED,
            date(),
            listOf(date()),
            features,
            Instant.parse("2026-07-24T11:59:59Z"),
        )

        assertEquals(UnavailableReason.STALE_INPUT, (result as RuntimeEstimateResult.Unavailable).reason)
    }

    private fun artifact(
        periods: Int = 18,
        checksum: String = "checksum",
        calibrationCutoff: LocalDate = date(),
        intervalEligible: Boolean = false,
        confidenceEligible: Boolean = false,
        family: EstimatorFamily = EstimatorFamily.SEASONAL_BASELINE,
        parameters: Map<String, BigDecimal> = mapOf("medianRetailNtdPerTaiJin" to BigDecimal("52")),
        formula: String = "CALIBRATED_MODEL",
    ) = KnownEstimatorArtifact(
        modelVersion = "seasonal-median-v1",
        family = family,
        supportedBases = setOf(MarketBasis.TAIPEI_COMBINED),
        calibrationCutoff = calibrationCutoff,
        formula = formula,
        parameters = parameters,
        eligibility = EstimatorEligibility(
            pairedCalibrationPeriods = periods,
            pointEligible = true,
            intervalEligible = intervalEligible,
            confidenceEligible = confidenceEligible,
            artifactIntegrityValid = true,
        ),
        checksum = checksum,
        computedChecksum = "checksum",
    )

    private fun conceptId() = ProduceConceptId("vegetable.cabbage")

    private fun date() = LocalDate.parse("2026-07-14")
}
