package tw.taipei.veges.data.sync

import java.time.Instant
import java.time.LocalDate
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
    fun cachedPreviousThirtyDaysOnlyDownloadsToday() {
        val today = LocalDate.of(2026, 8, 2)
        val completedDates = (1L..30L).map(today::minusDays)

        assertEquals(
            listOf(SyncDateRange(today, today)),
            missingDateRanges(
                from = today.minusDays(29),
                to = today,
                completedDates = completedDates,
            ),
        )
    }

    @Test
    fun completeCacheDoesNotDownloadHistoryAgain() {
        val today = LocalDate.of(2026, 8, 2)
        val completedDates = (0L..29L).map(today::minusDays)

        assertEquals(
            emptyList<SyncDateRange>(),
            missingDateRanges(
                from = today.minusDays(29),
                to = today,
                completedDates = completedDates,
            ),
        )
    }

    @Test
    fun separatedMissingDaysBecomeContinuousDownloadRanges() {
        val from = LocalDate.of(2026, 7, 1)
        val to = LocalDate.of(2026, 7, 7)
        val completedDates = listOf(
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 4),
            LocalDate.of(2026, 7, 7),
        )

        assertEquals(
            listOf(
                SyncDateRange(LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 3)),
                SyncDateRange(LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 6)),
            ),
            missingDateRanges(from, to, completedDates),
        )
    }

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
