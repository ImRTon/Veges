package tw.taipei.veges.data.repository

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tw.taipei.veges.data.local.OfficialVariantEntity
import tw.taipei.veges.data.local.SourceObservationEntity
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.PriceUnit
import tw.taipei.veges.domain.SourceDayState
import tw.taipei.veges.domain.SourceKind

class VariantMarketPriceAggregationTest {
    @Test
    fun `mango varieties keep separate volume weighted prices`() {
        val prices = variantMarketPrices(
            variants = variants("R1" to "芒果-愛文", "R4" to "芒果-凱特"),
            rows = listOf(
                observation("R1", MarketBasis.TAIPEI_FIRST, "100", "10"),
                observation("R1", MarketBasis.TAIPEI_SECOND, "120", "30"),
                observation("R4", MarketBasis.TAIPEI_FIRST, "60", "10"),
                observation("R4", MarketBasis.TAIPEI_SECOND, "80", "10"),
            ),
            basis = MarketBasis.TAIPEI_COMBINED,
        )

        val aiwen = prices.single { it.commodityCode.value == "R1" }
        val keitt = prices.single { it.commodityCode.value == "R4" }

        assertEquals(0, requireNotNull(aiwen.wholesaleAverage).amount.compareTo(BigDecimal("69")))
        assertEquals(0, requireNotNull(keitt.wholesaleAverage).amount.compareTo(BigDecimal("42")))
        assertEquals(0, requireNotNull(aiwen.volumeKg).compareTo(BigDecimal("40")))
        assertEquals(0, requireNotNull(keitt.volumeKg).compareTo(BigDecimal("20")))
    }

    @Test
    fun `single market basis uses only that market`() {
        val price = variantMarketPrices(
            variants = variants("R1" to "芒果-愛文"),
            rows = listOf(
                observation("R1", MarketBasis.TAIPEI_FIRST, "100", "10"),
                observation("R1", MarketBasis.TAIPEI_SECOND, "120", "30"),
            ),
            basis = MarketBasis.TAIPEI_FIRST,
        ).single()

        assertEquals(0, requireNotNull(price.wholesaleAverage).amount.compareTo(BigDecimal("60")))
        assertEquals(MarketBasis.TAIPEI_FIRST, price.basis)
    }

    @Test
    fun `combined price requires both Taipei markets on the same date`() {
        val price = variantMarketPrices(
            variants = variants("R1" to "芒果-愛文"),
            rows = listOf(
                observation("R1", MarketBasis.TAIPEI_FIRST, "100", "10"),
            ),
            basis = MarketBasis.TAIPEI_COMBINED,
        ).single()

        assertNull(price.wholesaleAverage)
        assertNull(price.observedOn)
    }

    private fun variants(vararg entries: Pair<String, String>): List<OfficialVariantEntity> =
        entries.flatMap { (code, name) ->
            listOf(
                OfficialVariantEntity("fruit.mango", code, name, MarketBasis.TAIPEI_FIRST),
                OfficialVariantEntity("fruit.mango", code, name, MarketBasis.TAIPEI_SECOND),
            )
        }

    private fun observation(
        code: String,
        market: MarketBasis,
        average: String,
        volume: String,
    ) = SourceObservationEntity(
        observationId = "$code-$market",
        sourceKind = SourceKind.MOA_WHOLESALE,
        market = market,
        commodityCode = code,
        officialName = "芒果",
        observedOn = LocalDate.parse("2026-07-29"),
        lowerPrice = BigDecimal(average),
        averagePrice = BigDecimal(average),
        upperPrice = BigDecimal(average),
        priceUnit = PriceUnit.NTD_PER_KILOGRAM,
        volume = BigDecimal(volume),
        volumeUnit = "公斤",
        state = SourceDayState.VALID,
        sourceUrl = null,
        attribution = "農業部",
        retrievedAt = Instant.parse("2026-07-29T00:00:00Z"),
        syncRunId = "test",
    )
}
