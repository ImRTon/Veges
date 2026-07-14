package tw.taipei.veges.data.network

import javax.inject.Inject
import kotlinx.serialization.decodeFromString

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
