package tw.taipei.veges.data.network

import java.time.LocalDate
import javax.inject.Inject
import kotlinx.serialization.decodeFromString
import okhttp3.HttpUrl.Companion.toHttpUrl
import tw.taipei.veges.domain.MarketBasis

data class MoaWholesaleSnapshot(
    val records: List<MoaWholesaleRecordDto>,
    val retrievedAt: java.time.Instant,
    val etag: String?,
    val lastModified: String?,
    val notModified: Boolean,
)

data class MoaCropSnapshot(
    val records: List<MoaCropDto>,
    val retrievedAt: java.time.Instant,
)

data class TaipeiRetailSnapshot(
    val records: List<TaipeiRetailRecordDto>,
    val retrievedAt: java.time.Instant,
)

data class MoaWholesaleHistoryQuery(
    val from: LocalDate,
    val to: LocalDate,
    val market: MarketBasis,
    val cropName: String? = null,
)

data class MoaWholesaleHistorySnapshot(
    val records: List<MoaWholesaleRecordDto>,
    val retrievedAt: java.time.Instant,
    val pagesFetched: Int,
)

class MoaWholesaleClient @Inject constructor(
    private val http: OfficialHttpClient,
) {
    suspend fun fetch(
        etag: String? = null,
        lastModified: String? = null,
    ): MoaWholesaleSnapshot {
        val payload = http.get(OfficialEndpoints.moaWholesale, etag, lastModified)
        return MoaWholesaleSnapshot(
            records = strictSourceJson.decodeFromString(payload.body),
            retrievedAt = payload.retrievedAt,
            etag = payload.etag,
            lastModified = payload.lastModified,
            notModified = payload.notModified,
        )
    }

    suspend fun fetchHistory(
        query: MoaWholesaleHistoryQuery,
        pageSize: Int = HISTORY_PAGE_SIZE,
    ): MoaWholesaleHistorySnapshot {
        require(query.from <= query.to) { "History start must not be after end" }
        require(pageSize in 1..HISTORY_PAGE_SIZE) { "History page size must be bounded" }
        val records = mutableListOf<MoaWholesaleRecordDto>()
        var retrievedAt = java.time.Instant.EPOCH
        var pagesFetched = 0
        repeat(MAX_HISTORY_PAGES) { page ->
            val payload = http.get(
                buildMoaWholesaleHistoryUrl(
                    query = query,
                    top = pageSize,
                    skip = page * pageSize,
                ),
            )
            val pageRecords = strictSourceJson.decodeFromString<List<MoaWholesaleRecordDto>>(payload.body)
            records += pageRecords
            retrievedAt = maxOf(retrievedAt, payload.retrievedAt)
            pagesFetched += 1
            if (pageRecords.size < pageSize) {
                return MoaWholesaleHistorySnapshot(
                    records = records.distinctBy {
                        "${it.transactionDate}:${it.marketCode}:${it.cropCode}"
                    },
                    retrievedAt = retrievedAt,
                    pagesFetched = pagesFetched,
                )
            }
        }
        error("History query exceeded $MAX_HISTORY_PAGES bounded pages")
    }

    private companion object {
        const val HISTORY_PAGE_SIZE = 1_000
        const val MAX_HISTORY_PAGES = 20
    }
}

internal fun buildMoaWholesaleHistoryUrl(
    query: MoaWholesaleHistoryQuery,
    top: Int,
    skip: Int,
): String {
    val builder = OfficialEndpoints.moaWholesale
        .toHttpUrl()
        .newBuilder()
        .addQueryParameter("\$top", top.toString())
        .addQueryParameter("\$skip", skip.toString())
        .addQueryParameter("StartDate", query.from.toRocDateString())
        .addQueryParameter("EndDate", query.to.toRocDateString())
        .addQueryParameter("Market", query.market.moaMarketName())
    query.cropName?.takeIf(String::isNotBlank)?.let { builder.addQueryParameter("Crop", it) }
    return builder.build().toString()
}

private fun LocalDate.toRocDateString(): String =
    "%03d.%02d.%02d".format(year - 1911, monthValue, dayOfMonth)

private fun MarketBasis.moaMarketName(): String = when (this) {
    MarketBasis.TAIPEI_FIRST -> "台北一"
    MarketBasis.TAIPEI_SECOND -> "台北二"
    MarketBasis.TAIPEI_COMBINED -> error("Combined basis is not an official market query")
}

class MoaCropClient @Inject constructor(
    private val http: OfficialHttpClient,
) {
    suspend fun fetch(): MoaCropSnapshot {
        val payload = http.get(OfficialEndpoints.moaCropMetadata)
        val response = strictSourceJson.decodeFromString<MoaCropResponseDto>(payload.body)
        require(response.result == "OK") { "MOA crop metadata returned ${response.result}" }
        return MoaCropSnapshot(response.data, payload.retrievedAt)
    }
}

class TaipeiRetailClient @Inject constructor(
    private val http: OfficialHttpClient,
) {
    suspend fun fetch(resourceUrl: String = OfficialEndpoints.taipeiRetailDecember2025): TaipeiRetailSnapshot {
        val payload = http.get(resourceUrl)
        return TaipeiRetailSnapshot(
            records = TaipeiRetailCsvParser.parse(payload.body),
            retrievedAt = payload.retrievedAt,
        )
    }
}
