package tw.taipei.veges.domain

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceSurgePredictorTest {
    private val today = LocalDate.parse("2026-07-29")
    private val now = Instant.parse("2026-07-29T14:00:00Z")

    @Test
    fun combinesMarketEvidenceAndActiveTyphoonIntoExplainableAlert() {
        val prediction = PriceSurgePredictor().evaluate(
            items = listOf(
                marketItem(
                    recentPrice = "130",
                    baselinePrice = "100",
                    recentVolume = "50",
                    baselineVolume = "100",
                ),
            ),
            shockSignals = listOf(
                MarketShockSignal(
                    kind = MarketShockKind.TYPHOON,
                    severity = BigDecimal.ONE,
                    headline = "海上陸上颱風警報",
                    affectedAreas = setOf("雲林縣"),
                    effectiveAt = now.minusSeconds(3_600),
                    expiresAt = now.plusSeconds(10_800),
                ),
            ),
            today = today,
            now = now,
        ).predictions.single()

        assertEquals(PriceSurgeReasonKind.TYPHOON, prediction.reasons.first().kind)
        assertTrue(prediction.projectedRisePercent >= BigDecimal("20"))
        assertTrue(prediction.riskScore >= 75)
    }

    @Test
    fun requiresTenValidTradingDays() {
        val item = marketItem(
            recentPrice = "130",
            baselinePrice = "100",
            recentVolume = "50",
            baselineVolume = "100",
        ).copy(wholesaleHistory = marketItemHistory().take(9))

        val result = PriceSurgePredictor().evaluate(
            items = listOf(item),
            shockSignals = emptyList(),
            today = today,
            now = now,
        )

        assertEquals(0, result.eligibleItemCount)
        assertTrue(result.predictions.isEmpty())
    }

    @Test
    fun weatherAloneDoesNotInventAProducePriceAlert() {
        val stable = marketItem(
            recentPrice = "100",
            baselinePrice = "100",
            recentVolume = "100",
            baselineVolume = "100",
        )
        val result = PriceSurgePredictor().evaluate(
            items = listOf(stable),
            shockSignals = listOf(
                MarketShockSignal(
                    kind = MarketShockKind.HEAVY_RAIN,
                    severity = BigDecimal.ONE,
                    headline = "豪雨特報",
                    affectedAreas = setOf("屏東縣"),
                    effectiveAt = now.minusSeconds(60),
                    expiresAt = now.plusSeconds(3_600),
                ),
            ),
            today = today,
            now = now,
        )

        assertEquals(1, result.eligibleItemCount)
        assertTrue(result.predictions.isEmpty())
    }

    @Test
    fun createsOneMarketOutlookWhenAtLeastTwentyPercentMoveTogether() {
        val items = (0 until 10).map { index ->
            val item = if (index < 3) {
                marketItem(
                    recentPrice = "130",
                    baselinePrice = "100",
                    recentVolume = "50",
                    baselineVolume = "100",
                )
            } else {
                marketItem(
                    recentPrice = "100",
                    baselinePrice = "100",
                    recentVolume = "100",
                    baselineVolume = "100",
                )
            }
            item.withIdentity(index)
        }

        val result = PriceSurgePredictor().evaluate(
            items = items,
            shockSignals = emptyList(),
            today = today,
            now = now,
        )
        val outlook = requireNotNull(result.marketOutlook)

        assertEquals(3, outlook.affectedItemCount)
        assertEquals(10, outlook.eligibleItemCount)
        assertEquals(30, outlook.marketBreadthPercent)
        assertEquals("到貨量普遍縮減，整體價格可能上揚", outlook.primaryReason.headline)
    }

    @Test
    fun doesNotLabelTwoIsolatedItemsAsAMarketWideSurge() {
        val items = (0 until 10).map { index ->
            val item = if (index < 2) {
                marketItem(
                    recentPrice = "130",
                    baselinePrice = "100",
                    recentVolume = "50",
                    baselineVolume = "100",
                )
            } else {
                marketItem(
                    recentPrice = "100",
                    baselinePrice = "100",
                    recentVolume = "100",
                    baselineVolume = "100",
                )
            }
            item.withIdentity(index)
        }

        val result = PriceSurgePredictor().evaluate(
            items = items,
            shockSignals = emptyList(),
            today = today,
            now = now,
        )

        assertEquals(2, result.predictions.size)
        assertEquals(null, result.marketOutlook)
    }

    private fun marketItem(
        recentPrice: String,
        baselinePrice: String,
        recentVolume: String,
        baselineVolume: String,
    ) = MarketItem(
        concept = ProduceConcept(
            id = ProduceConceptId("vegetable.test"),
            householdName = "測試蔬菜",
            aliases = emptyList(),
            category = ProduceCategory.VEGETABLE,
            published = true,
            illustrationAsset = null,
            officialVariants = emptyList(),
        ),
        latestEstimate = null,
        wholesaleHistory = marketItemHistory(
            recentPrice,
            baselinePrice,
            recentVolume,
            baselineVolume,
        ),
    )

    private fun marketItemHistory(
        recentPrice: String = "130",
        baselinePrice: String = "100",
        recentVolume: String = "50",
        baselineVolume: String = "100",
    ): List<MarketHistoryPoint> = List(23) { index ->
        MarketHistoryPoint(
            observedOn = today.minusDays(index.toLong()),
            averageNtdPerKg = BigDecimal(if (index < 3) recentPrice else baselinePrice),
            volumeKg = BigDecimal(if (index < 3) recentVolume else baselineVolume),
        )
    }

    private fun MarketItem.withIdentity(index: Int): MarketItem = copy(
        concept = concept.copy(
            id = ProduceConceptId("vegetable.test-$index"),
            householdName = "測試蔬菜$index",
        ),
    )
}
