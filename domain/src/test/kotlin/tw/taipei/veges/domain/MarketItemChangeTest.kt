package tw.taipei.veges.domain

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class MarketItemChangeTest {
    @Test
    fun sevenDayAverageChangeUsesRetailReferencePrice() {
        val item = MarketItem(
            concept = concept(),
            latestEstimate = estimate("42.5"),
            previousTradingDayPrices = List(7) {
                ScaledPrice(
                    amount = BigDecimal("50.0"),
                    unit = PriceUnit.NTD_PER_TAI_JIN,
                )
            },
        )

        assertEquals(
            BigDecimal("-15.000000"),
            item.sevenDayAverageChangePercent(),
        )
    }

    @Test
    fun selectedWindowUsesOnlyRequestedTradingDays() {
        val item = MarketItem(
            concept = concept(),
            latestEstimate = estimate("90"),
            previousTradingDayPrices = listOf("100", "100", "100", "82.5", "82.5", "82.5", "82.5")
                .map { ScaledPrice(BigDecimal(it), PriceUnit.NTD_PER_TAI_JIN) },
        )

        assertEquals(BigDecimal("-10.000000"), item.averageChangePercent(3))
        assertEquals(BigDecimal("0.000000"), item.averageChangePercent(7))
    }

    @Test
    fun missingComparisonDoesNotInventAChange() {
        val item = MarketItem(
            concept = concept(),
            latestEstimate = estimate("42.5"),
        )

        assertEquals(null, item.sevenDayAverageChangePercent())
    }

    private fun concept() = ProduceConcept(
        id = ProduceConceptId("vegetable.test"),
        householdName = "測試蔬菜",
        aliases = emptyList(),
        category = ProduceCategory.VEGETABLE,
        published = true,
        illustrationAsset = null,
        officialVariants = emptyList(),
    )

    private fun estimate(value: String) = Estimate(
        conceptId = ProduceConceptId("vegetable.test"),
        basis = MarketBasis.TAIPEI_COMBINED,
        modelVersion = "test",
        wholesaleSourceDates = listOf(LocalDate.parse("2026-07-29")),
        calibrationCutoff = LocalDate.parse("2026-07-01"),
        calculatedAt = Instant.parse("2026-07-29T00:00:00Z"),
        point = ScaledPrice(BigDecimal(value), PriceUnit.NTD_PER_TAI_JIN),
        intervalLower = null,
        intervalUpper = null,
        confidence = null,
        unavailableReason = null,
    )
}
