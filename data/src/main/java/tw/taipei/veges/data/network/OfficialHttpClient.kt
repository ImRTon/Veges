package tw.taipei.veges.data.network

import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class OfficialHttpPayload(
    val body: String,
    val retrievedAt: Instant,
    val etag: String?,
    val lastModified: String?,
    val notModified: Boolean = false,
)

data class OfficialHttpResponse(
    val code: Int,
    val body: String?,
    val etag: String?,
    val lastModified: String?,
)

fun interface OfficialHttpTransport {
    fun execute(request: Request): OfficialHttpResponse
}

class OkHttpOfficialHttpTransport @Inject constructor() : OfficialHttpTransport {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    override fun execute(request: Request): OfficialHttpResponse =
        client.newCall(request).execute().use { response ->
            OfficialHttpResponse(
                code = response.code,
                body = response.body?.string(),
                etag = response.header("ETag"),
                lastModified = response.header("Last-Modified"),
            )
        }
}

class OfficialHttpException(
    val url: String,
    val statusCode: Int? = null,
    cause: Throwable? = null,
) : IOException("Official source request failed: $url${statusCode?.let { " ($it)" } ?: ""}", cause)

class OfficialHttpClient @Inject constructor(
    private val transport: OfficialHttpTransport,
) {
    suspend fun get(
        url: String,
        etag: String? = null,
        lastModified: String? = null,
        maxAttempts: Int = 3,
    ): OfficialHttpPayload = withContext(Dispatchers.IO) {
        require(maxAttempts in 1..3) { "Retry count must be bounded between 1 and 3 attempts" }

        var lastFailure: OfficialHttpException? = null
        repeat(maxAttempts) { attempt ->
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json, text/csv;q=0.9")
                .header("User-Agent", "Veges-contract-client/0.1")
                .apply {
                    etag?.let { header("If-None-Match", it) }
                    lastModified?.let { header("If-Modified-Since", it) }
                }
                .build()

            try {
                val response = transport.execute(request)
                if (response.code in 200..299) {
                    return@withContext OfficialHttpPayload(
                        body = response.body.orEmpty(),
                        retrievedAt = Instant.now(),
                        etag = response.etag,
                        lastModified = response.lastModified,
                    )
                }
                if (response.code == HTTP_NOT_MODIFIED) {
                    return@withContext OfficialHttpPayload(
                        body = "",
                        retrievedAt = Instant.now(),
                        etag = response.etag ?: etag,
                        lastModified = response.lastModified ?: lastModified,
                        notModified = true,
                    )
                }

                val failure = OfficialHttpException(url, response.code)
                lastFailure = failure
                if (response.code !in RETRYABLE_STATUS_CODES) throw failure
            } catch (exception: IOException) {
                val failure = if (exception is OfficialHttpException) {
                    exception
                } else {
                    OfficialHttpException(url, cause = exception)
                }
                lastFailure = failure
                if (failure.statusCode != null && failure.statusCode !in RETRYABLE_STATUS_CODES) {
                    throw failure
                }
            }

            if (attempt < maxAttempts - 1) Thread.sleep(RETRY_BACKOFF_MILLIS[attempt])
        }

        throw requireNotNull(lastFailure)
    }

    private companion object {
        const val HTTP_NOT_MODIFIED = 304
        val RETRYABLE_STATUS_CODES = setOf(408, 425, 429, 500, 502, 503, 504)
        val RETRY_BACKOFF_MILLIS = longArrayOf(250, 1_000)
    }
}
