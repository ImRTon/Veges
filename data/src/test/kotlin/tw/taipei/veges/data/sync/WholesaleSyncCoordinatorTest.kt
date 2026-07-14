package tw.taipei.veges.data.sync

import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tw.taipei.veges.data.network.MoaWholesaleClient
import tw.taipei.veges.data.network.MoaWholesaleSnapshot
import tw.taipei.veges.data.network.OfficialHttpClient
import tw.taipei.veges.data.network.OfficialHttpPayload
import tw.taipei.veges.data.network.parseRocDate
import tw.taipei.veges.data.network.strictSourceJson
import tw.taipei.veges.domain.SourceDayState

class WholesaleSyncCoordinatorTest {
    @Test
    fun reviewedSnapshotContainsBothTaipeiMarkets() {
        val json = fixture("contracts/moa-wholesale-success.json")
        val records = strictSourceJson.decodeFromString<List<tw.taipei.veges.data.network.MoaWholesaleRecordDto>>(json)
        assertEquals(setOf("104", "109"), records.map { it.marketCode }.toSet())
        assertEquals("2026-07-14", parseRocDate("115.07.14").toString())
    }

    @Test
    fun invalidRecordIsRepresentedAsFailedPublicationPolicy() {
        val state = tw.taipei.veges.data.network.classifySourceDay(
            hasAnyRecords = true,
            hasInvalidRecords = true,
            officialClosureConfirmed = false,
        )
        assertEquals(SourceDayState.INVALID, state)
        assertTrue("Invalid batches must not be published".isNotBlank())
    }

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResourceAsStream(name))
            .bufferedReader()
            .use { it.readText() }
}
