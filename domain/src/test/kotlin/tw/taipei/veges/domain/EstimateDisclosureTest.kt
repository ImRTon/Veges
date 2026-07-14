package tw.taipei.veges.domain

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class EstimateDisclosureTest {
    @Test
    fun everyEstimateCarriesTheMandatoryDisclosure() {
        val estimate = Estimate(
            conceptId = ProduceConceptId("vegetable.cabbage"),
            basis = MarketBasis.TAIPEI_COMBINED,
            modelVersion = "candidate-v1",
            wholesaleSourceDates = listOf(LocalDate.parse("2026-07-14")),
            calibrationCutoff = LocalDate.parse("2026-07-14"),
            calculatedAt = Instant.parse("2026-07-14T03:00:00Z"),
            point = ScaledPrice(BigDecimal("52"), PriceUnit.NTD_PER_TAI_JIN),
            intervalLower = null,
            intervalUpper = null,
            confidence = null,
            unavailableReason = null,
        )

        assertEquals(EstimateDisclosure.SHORT_TAG, estimate.disclosure.shortTag)
        assertEquals(EstimateDisclosure.FULL_LABEL, estimate.disclosure.fullLabel)
    }
}
