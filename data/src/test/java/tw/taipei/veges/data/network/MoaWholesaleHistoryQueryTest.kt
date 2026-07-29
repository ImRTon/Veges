package tw.taipei.veges.data.network

import java.time.LocalDate
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Test
import tw.taipei.veges.domain.MarketBasis

class MoaWholesaleHistoryQueryTest {
    @Test
    fun `history URL uses ROC dates exact market crop and bounded paging`() {
        val url = buildMoaWholesaleHistoryUrl(
            query = MoaWholesaleHistoryQuery(
                from = LocalDate.of(2025, 7, 27),
                to = LocalDate.of(2026, 7, 26),
                market = MarketBasis.TAIPEI_FIRST,
                cropName = "甘藍-初秋",
            ),
            top = 1_000,
            skip = 2_000,
        ).toHttpUrl()

        assertEquals("114.07.27", url.queryParameter("StartDate"))
        assertEquals("115.07.26", url.queryParameter("EndDate"))
        assertEquals("台北一", url.queryParameter("Market"))
        assertEquals("甘藍-初秋", url.queryParameter("Crop"))
        assertEquals("1000", url.queryParameter("\$top"))
        assertEquals("2000", url.queryParameter("\$skip"))
    }

    @Test
    fun `bulk Taipei market URL omits crop filter`() {
        val url = buildMoaWholesaleHistoryUrl(
            query = MoaWholesaleHistoryQuery(
                from = LocalDate.of(2026, 6, 12),
                to = LocalDate.of(2026, 7, 26),
                market = MarketBasis.TAIPEI_SECOND,
            ),
            top = 1_000,
            skip = 0,
        ).toHttpUrl()

        assertEquals("台北二", url.queryParameter("Market"))
        assertEquals(null, url.queryParameter("Crop"))
        assertEquals("115.06.12", url.queryParameter("StartDate"))
    }
}
