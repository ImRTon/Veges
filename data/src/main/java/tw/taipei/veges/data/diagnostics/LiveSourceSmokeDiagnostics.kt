package tw.taipei.veges.data.diagnostics

import java.time.Instant
import kotlinx.coroutines.runBlocking
import tw.taipei.veges.data.network.MoaCropClient
import tw.taipei.veges.data.network.MoaWholesaleClient
import tw.taipei.veges.data.network.OfficialHttpClient
import tw.taipei.veges.data.network.OkHttpOfficialHttpTransport
import tw.taipei.veges.data.network.TaipeiRetailClient

/** Best-effort diagnostics; callers should treat a thrown exception as a non-blocking warning. */
fun runLiveSourceSmokeDiagnostics(): Result<LiveSourceSmokeReport> = runCatching {
    runBlocking {
        val http = OfficialHttpClient(OkHttpOfficialHttpTransport())
        val crops = MoaCropClient(http).fetch()
        val wholesale = MoaWholesaleClient(http).fetch()
        val retail = TaipeiRetailClient(http).fetch()
        LiveSourceSmokeReport(
            checkedAt = Instant.now().toString(),
            cropRecords = crops.records.size,
            wholesaleRecords = wholesale.records.size,
            retailRecords = retail.records.size,
            wholesaleNotModified = wholesale.notModified,
        )
    }
}

data class LiveSourceSmokeReport(
    val checkedAt: String,
    val cropRecords: Int,
    val wholesaleRecords: Int,
    val retailRecords: Int,
    val wholesaleNotModified: Boolean,
)
