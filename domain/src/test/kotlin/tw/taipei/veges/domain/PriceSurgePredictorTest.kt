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
    fun typhoonCausedRainIsMarketContextWithoutChangingItemScores() {
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
        val predictor = PriceSurgePredictor()
        val withWeather = predictor.evaluate(
            items = items,
            shockSignals = listOf(
                MarketShockSignal(
                    kind = MarketShockKind.HEAVY_RAIN,
                    severity = BigDecimal.ONE,
                    headline = "豪雨特報",
                    affectedAreas = setOf("雲林縣"),
                    effectiveAt = now.minusSeconds(3_600),
                    expiresAt = now.plusSeconds(10_800),
                    cause = MarketShockKind.TYPHOON,
                ),
            ),
            today = today,
            now = now,
        )
        val withoutWeather = predictor.evaluate(
            items = items,
            shockSignals = emptyList(),
            today = today,
            now = now,
        )
        val outlook = requireNotNull(withWeather.marketOutlook)

        assertEquals(PriceSurgeReasonKind.TYPHOON, outlook.primaryReason.kind)
        assertEquals("颱風來襲，整體蔬果價格可能上揚", outlook.primaryReason.headline)
        assertEquals(
            withoutWeather.predictions.map { it.riskScore to it.projectedRisePercent },
            withWeather.predictions.map { it.riskScore to it.projectedRisePercent },
        )
        assertTrue(withWeather.predictions.flatMap { it.reasons }.none { it.kind.isWeather() })
        assertEquals(
            setOf(
                PriceSurgeReasonKind.VOLUME_CONTRACTION,
                PriceSurgeReasonKind.PRICE_MOMENTUM,
                PriceSurgeReasonKind.RECENT_PRICE_ANOMALY,
            ),
            withWeather.predictions.first().reasons.map { it.kind }.toSet(),
        )
    }

    @Test
    fun classifiesEachQualifiedWeatherContext() {
        val items = marketBreadthItems()
        val cases = listOf(
            MarketShockKind.TYPHOON to PriceSurgeReasonKind.TYPHOON,
            MarketShockKind.HEAVY_RAIN to PriceSurgeReasonKind.HEAVY_RAIN,
            MarketShockKind.EXTREME_HEAT to PriceSurgeReasonKind.EXTREME_HEAT,
        )

        cases.forEach { (kind, expectedReason) ->
            val result = PriceSurgePredictor().evaluate(
                items = items,
                shockSignals = listOf(activeShock(kind, setOf("雲林縣"))),
                today = today,
                now = now,
            )

            assertEquals(expectedReason, requireNotNull(result.marketOutlook).primaryReason.kind)
        }
    }

    @Test
    fun missingOrUnknownWarningAreaDoesNotBecomePriceReason() {
        listOf(emptySet(), setOf("臺北市"), setOf("臺灣東北部海面")).forEach { areas ->
            val result = PriceSurgePredictor().evaluate(
                items = marketBreadthItems(),
                shockSignals = listOf(activeShock(MarketShockKind.TYPHOON, areas)),
                today = today,
                now = now,
            )

            assertEquals(
                PriceSurgeReasonKind.VOLUME_CONTRACTION,
                requireNotNull(result.marketOutlook).primaryReason.kind,
            )
        }
    }

    @Test
    fun subthresholdAnomalyDoesNotIncreaseMarketProjection() {
        val predictor = PriceSurgePredictor()
        val neutralAnomaly = predictor.evaluate(
            items = listOf(
                marketItem(
                    recentPrice = "120",
                    baselinePrice = "100",
                    recentVolume = "50",
                    baselineVolume = "100",
                    olderPrice = "120",
                ),
            ),
            shockSignals = emptyList(),
            today = today,
            now = now,
        ).predictions.single()
        val belowThresholdAnomaly = predictor.evaluate(
            items = listOf(
                marketItem(
                    recentPrice = "120",
                    baselinePrice = "100",
                    recentVolume = "50",
                    baselineVolume = "100",
                    olderPrice = "110",
                ),
            ),
            shockSignals = emptyList(),
            today = today,
            now = now,
        ).predictions.single()

        assertEquals(neutralAnomaly.projectedRisePercent, belowThresholdAnomaly.projectedRisePercent)
        assertTrue(
            belowThresholdAnomaly.reasons.none {
                it.kind == PriceSurgeReasonKind.RECENT_PRICE_ANOMALY
            },
        )
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
        olderPrice: String = baselinePrice,
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
            olderPrice,
        ),
    )

    private fun marketItemHistory(
        recentPrice: String = "130",
        baselinePrice: String = "100",
        recentVolume: String = "50",
        baselineVolume: String = "100",
        olderPrice: String = baselinePrice,
    ): List<MarketHistoryPoint> = List(23) { index ->
        MarketHistoryPoint(
            observedOn = today.minusDays(index.toLong()),
            averageNtdPerKg = BigDecimal(
                when {
                    index < 3 -> recentPrice
                    index < 10 -> baselinePrice
                    else -> olderPrice
                },
            ),
            volumeKg = BigDecimal(if (index < 3) recentVolume else baselineVolume),
        )
    }

    private fun MarketItem.withIdentity(index: Int): MarketItem = copy(
        concept = concept.copy(
            id = ProduceConceptId("vegetable.test-$index"),
            householdName = "測試蔬菜$index",
        ),
    )

    private fun marketBreadthItems(): List<MarketItem> = (0 until 10).map { index ->
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

    private fun activeShock(
        kind: MarketShockKind,
        affectedAreas: Set<String>,
    ) = MarketShockSignal(
        kind = kind,
        severity = BigDecimal.ONE,
        headline = "官方警報",
        affectedAreas = affectedAreas,
        effectiveAt = now.minusSeconds(3_600),
        expiresAt = now.plusSeconds(3_600),
    )

    private fun PriceSurgeReasonKind.isWeather(): Boolean =
        this == PriceSurgeReasonKind.TYPHOON ||
            this == PriceSurgeReasonKind.HEAVY_RAIN ||
            this == PriceSurgeReasonKind.EXTREME_HEAT
}
