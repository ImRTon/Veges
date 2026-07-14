package tw.taipei.veges.data.network

import java.io.IOException
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.test.runTest
import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialHttpClientTest {
    @Test
    fun retriesRetryableFailureAndReturnsLaterSuccess() = runTest {
        val attempts = AtomicInteger(0)
        val client = OfficialHttpClient(OfficialHttpTransport { _ ->
            if (attempts.incrementAndGet() < 3) {
                OfficialHttpResponse(503, null, null, null)
            } else {
                OfficialHttpResponse(200, "[]", "etag-1", "Tue, 14 Jul 2026 02:00:00 GMT")
            }
        })

        val response = client.get("https://example.test/source", maxAttempts = 3)

        assertEquals(3, attempts.get())
        assertEquals("[]", response.body)
        assertEquals("etag-1", response.etag)
    }

    @Test
    fun doesNotRetryNonRetryableFailure() = runTest {
        val attempts = AtomicInteger(0)
        val client = OfficialHttpClient(OfficialHttpTransport { _ ->
            attempts.incrementAndGet()
            OfficialHttpResponse(403, "forbidden", null, null)
        })

        val error = runCatching { client.get("https://example.test/source", maxAttempts = 3) }.exceptionOrNull()

        assertTrue(error is OfficialHttpException)
        assertEquals(1, attempts.get())
    }

    @Test
    fun returnsNotModifiedWithoutDecodingAnEmptyBody() = runTest {
        val client = OfficialHttpClient(OfficialHttpTransport { request: Request ->
            assertEquals("etag-1", request.header("If-None-Match"))
            OfficialHttpResponse(304, null, "etag-1", null)
        })

        val response = client.get("https://example.test/source", etag = "etag-1")

        assertTrue(response.notModified)
        assertEquals("", response.body)
        assertEquals(Instant::class, response.retrievedAt::class)
    }

    @Test
    fun convertsTransportIOExceptionToBoundedFailure() = runTest {
        val client = OfficialHttpClient(OfficialHttpTransport { _ -> throw IOException("offline") })

        val error = runCatching { client.get("https://example.test/source", maxAttempts = 1) }.exceptionOrNull()

        assertTrue(error is OfficialHttpException)
        assertEquals("offline", error?.cause?.message)
    }
}
