package tw.taipei.veges.data.network

import java.math.BigDecimal
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import tw.taipei.veges.data.BuildConfig
import tw.taipei.veges.domain.MarketShockKind
import tw.taipei.veges.domain.MarketShockSignal

class CwaWeatherSignalClient @Inject constructor(
    private val http: OfficialHttpClient,
) {
    suspend fun fetchActiveSignals(now: Instant): List<MarketShockSignal> {
        val authorization = BuildConfig.CWA_API_KEY
        if (authorization.isBlank()) return emptyList()

        return supervisorScope {
            DATASETS.map { dataset ->
                async {
                    runCatching {
                        val payload = http.get(buildUrl(dataset.id, authorization))
                        CwaWarningParser.parse(
                            raw = payload.body,
                            kind = dataset.kind,
                            now = now,
                        )
                    }
                }
            }.flatMap { it.await().getOrDefault(emptyList()) }
        }
    }

    private fun buildUrl(datasetId: String, authorization: String): String =
        "${OfficialEndpoints.cwaDataStore}/$datasetId"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("Authorization", authorization)
            .addQueryParameter("format", "JSON")
            .build()
            .toString()

    private data class Dataset(
        val id: String,
        val kind: MarketShockKind,
    )

    private companion object {
        val DATASETS = listOf(
            Dataset("W-C0034-001", MarketShockKind.TYPHOON),
            Dataset("W-C0033-003", MarketShockKind.HEAVY_RAIN),
            Dataset("W-C0033-005", MarketShockKind.EXTREME_HEAT),
        )
    }
}

internal object CwaWarningParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(
        raw: String,
        kind: MarketShockKind,
        now: Instant,
    ): List<MarketShockSignal> {
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull()
            ?: return emptyList()
        if (root.string("success") != "true") return emptyList()
        val infos = root["records"]
            ?.asObject()
            ?.get("info")
            ?.asArray()
            .orEmpty()

        return infos.mapNotNull { element ->
            val info = element.asObject() ?: return@mapNotNull null
            val headline = info.string("headline").orEmpty()
            val urgency = info.string("urgency").orEmpty()
            val effectiveAt = info.instant("effective") ?: return@mapNotNull null
            val expiresAt = info.instant("expires") ?: return@mapNotNull null
            if (urgency.equals("Past", ignoreCase = true) ||
                headline.contains("解除") ||
                effectiveAt > now ||
                expiresAt <= now
            ) {
                return@mapNotNull null
            }
            MarketShockSignal(
                kind = kind,
                severity = info.severity(),
                headline = headline.ifBlank { kind.defaultHeadline() },
                affectedAreas = info["area"]
                    ?.asArray()
                    .orEmpty()
                    .mapNotNull { it.asObject()?.string("areaDesc") }
                    .filter(String::isNotBlank)
                    .toSet(),
                effectiveAt = effectiveAt,
                expiresAt = expiresAt,
            )
        }
    }

    private fun JsonObject.severity(): BigDecimal = when (string("severity")?.lowercase()) {
        "extreme" -> BigDecimal.ONE
        "severe" -> BigDecimal("0.9")
        "moderate" -> BigDecimal("0.7")
        else -> BigDecimal("0.6")
    }

    private fun JsonObject.instant(name: String): Instant? =
        string(name)?.let { value ->
            runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
        }

    private fun JsonObject.string(name: String): String? =
        get(name)?.runCatching { jsonPrimitive.content }?.getOrNull()

    private fun JsonElement.asObject(): JsonObject? = this as? JsonObject

    private fun JsonElement.asArray(): JsonArray? = this as? JsonArray

    private fun MarketShockKind.defaultHeadline(): String = when (this) {
        MarketShockKind.TYPHOON -> "颱風警報"
        MarketShockKind.HEAVY_RAIN -> "豪雨特報"
        MarketShockKind.EXTREME_HEAT -> "高溫資訊"
    }
}
