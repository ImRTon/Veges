package tw.taipei.veges.data.network

import java.time.Instant
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.taipei.veges.domain.MarketBasis
import tw.taipei.veges.domain.SourceDayState
import tw.taipei.veges.domain.UnavailableReason

class SourceContractTest {
    @Test
    fun moaFixturePreservesOfficialFieldsAndMarketIdentity() {
        val payload = fixture("contracts/moa-wholesale-success.json")
        val records = strictSourceJson.decodeFromString<List<MoaWholesaleRecordDto>>(payload)

        assertEquals(2, records.size)
        assertEquals("LA1", records.first().cropCode)
        assertEquals("104", records.first().marketCode)
        assertEquals("台北二", records.first().marketName)
        assertEquals("115.07.14", records.first().transactionDate)

        val second = validateWholesale(records[1], Instant.parse("2026-07-14T02:00:00Z"))
        assertTrue(second is tw.taipei.veges.data.network.WholesaleValidation.Valid)
        assertEquals(
            MarketBasis.TAIPEI_FIRST,
            (second as tw.taipei.veges.data.network.WholesaleValidation.Valid).observation.market,
        )
    }

    @Test
    fun rocDateAndRetailUnitAreNormalizedWithoutLosingSourceMeaning() {
        val payload = fixture("contracts/moa-wholesale-success.json")
        val record = strictSourceJson.decodeFromString<List<MoaWholesaleRecordDto>>(payload).first()
        val result = validateWholesale(record, Instant.parse("2026-07-14T02:00:00Z"))

        val observation = (result as WholesaleValidation.Valid).observation
        assertEquals("2026-07-14", observation.observedOn.toString())
        assertEquals("NTD_PER_KILOGRAM", observation.averagePrice?.unit?.name)

        val retail = TaipeiRetailCsvParser.parse(fixture("contracts/taipei-retail-december-2025.csv"))
        val validated = validateTaipeiRetail(retail.first { it.itemName == "甘藍" })
        assertEquals("NTD_PER_TAI_JIN", (validated as RetailValidation.Valid).average.unit.name)
    }

    @Test
    fun invalidWholesaleValuesNeverBecomeZeroPrice() {
        val record = MoaWholesaleRecordDto(
            transactionDate = "115.07.14",
            kindCode = "N04",
            cropCode = "LA1",
            cropName = "甘藍-初秋",
            marketCode = "104",
            marketName = "台北二",
            upperPrice = number("0"),
            middlePrice = number("0"),
            lowerPrice = number("0"),
            averagePrice = number("0"),
            volume = number("0"),
        )

        val result = validateWholesale(record, Instant.parse("2026-07-14T02:00:00Z"))

        assertTrue(result is WholesaleValidation.Invalid)
    }

    @Test
    fun officialRestSentinelBecomesClosedWithoutCreatingZeroPrice() {
        val payload = fixture("contracts/moa-wholesale-closure.json")
        val records = strictSourceJson.decodeFromString<List<MoaWholesaleRecordDto>>(payload)

        val results = records.map {
            validateWholesale(it, Instant.parse("2026-07-23T02:00:00Z"))
        }

        assertEquals(4, results.size)
        assertTrue(results.all { it is WholesaleValidation.Closed })
        assertEquals(
            setOf(MarketBasis.TAIPEI_FIRST, MarketBasis.TAIPEI_SECOND),
            results.map { (it as WholesaleValidation.Closed).market }.toSet(),
        )
        assertEquals(
            setOf("N04", "N06"),
            results.map { (it as WholesaleValidation.Closed).kindCode }.toSet(),
        )
        assertEquals(
            setOf("2026-07-23"),
            results.map { (it as WholesaleValidation.Closed).observedOn.toString() }.toSet(),
        )
    }

    @Test
    fun malformedRestSentinelStillFailsClosed() {
        val record = MoaWholesaleRecordDto(
            transactionDate = "115.07.23",
            kindCode = "N04",
            cropCode = "rest",
            cropName = "休市",
            marketCode = "104",
            marketName = "台北二",
            upperPrice = number("1"),
            middlePrice = number("0"),
            lowerPrice = number("0"),
            averagePrice = number("0"),
            volume = number("0"),
        )

        assertTrue(
            validateWholesale(record, Instant.parse("2026-07-23T02:00:00Z")) is
                WholesaleValidation.Invalid,
        )
    }

    @Test
    fun nullableNamesFromOutOfScopeMarketAreIgnoredWithoutWeakeningTaipeiValidation() {
        val outOfScope = MoaWholesaleRecordDto(
            transactionDate = "115.07.24",
            kindCode = null,
            cropCode = "FE800",
            cropName = null,
            marketCode = "105",
            marketName = "台北市場",
            upperPrice = number("51"),
            middlePrice = number("34"),
            lowerPrice = number("27"),
            averagePrice = number("36"),
            volume = number("450"),
        )
        val supportedMarketMissingName = outOfScope.copy(marketCode = "104")

        assertTrue(
            validateWholesale(outOfScope, Instant.parse("2026-07-24T02:00:00Z")) is
                WholesaleValidation.OutOfScope,
        )
        assertTrue(
            validateWholesale(supportedMarketMissingName, Instant.parse("2026-07-24T02:00:00Z")) is
                WholesaleValidation.Invalid,
        )
    }

    @Test
    fun retailDashIsUnavailableAndNotZero() {
        val result = validateTaipeiRetail(
            TaipeiRetailRecordDto("75", "臺北市", "63000", "芒果(在來)", "-"),
        )

        assertEquals("芒果(在來)", (result as RetailValidation.Unavailable).itemName)
    }

    @Test
    fun sourceDayClassificationKeepsMissingDistinctFromClosure() {
        assertEquals(SourceDayState.CLOSED, classifySourceDay(false, false, true))
        assertEquals(SourceDayState.MISSING, classifySourceDay(false, false, false))
        assertEquals(SourceDayState.INVALID, classifySourceDay(true, true, false))
        assertEquals(SourceDayState.VALID, classifySourceDay(true, false, false))
    }

    @Test
    fun csvParserRejectsSchemaDrift() {
        val error = runCatching {
            TaipeiRetailCsvParser.parse("wrong,header\n1,2")
        }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
    }

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResourceAsStream(name))
            .bufferedReader()
            .use { it.readText() }

    private fun number(value: String) = kotlinx.serialization.json.JsonPrimitive(value)
}
