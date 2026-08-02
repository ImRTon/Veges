package tw.taipei.veges.data.sync

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncSchedulerPolicyTest {
    @Test
    fun firstForegroundEntryRequestsCatchUp() {
        assertTrue(
            shouldEnqueueForegroundCatchUp(
                taxonomyReady = true,
                lastRequestedAtMillis = 0L,
                nowMillis = 1L,
            ),
        )
    }

    @Test
    fun firstForegroundEntryWaitsForTaxonomyImport() {
        assertFalse(
            shouldEnqueueForegroundCatchUp(
                taxonomyReady = false,
                lastRequestedAtMillis = 0L,
                nowMillis = 1L,
            ),
        )
    }

    @Test
    fun repeatedForegroundEntryInsideIntervalIsSuppressed() {
        val lastRequestedAt = 10_000L

        assertFalse(
            shouldEnqueueForegroundCatchUp(
                taxonomyReady = true,
                lastRequestedAtMillis = lastRequestedAt,
                nowMillis = lastRequestedAt + MIN_FOREGROUND_CATCH_UP_INTERVAL_MILLIS - 1L,
            ),
        )
    }

    @Test
    fun foregroundEntryAfterIntervalRequestsCatchUp() {
        val lastRequestedAt = 10_000L

        assertTrue(
            shouldEnqueueForegroundCatchUp(
                taxonomyReady = true,
                lastRequestedAtMillis = lastRequestedAt,
                nowMillis = lastRequestedAt + MIN_FOREGROUND_CATCH_UP_INTERVAL_MILLIS,
            ),
        )
    }

    @Test
    fun clockRollbackDoesNotSuppressCatchUpIndefinitely() {
        assertTrue(
            shouldEnqueueForegroundCatchUp(
                taxonomyReady = true,
                lastRequestedAtMillis = 20_000L,
                nowMillis = 10_000L,
            ),
        )
    }

    @Test
    fun firstConceptHistoryRequestIsEnqueued() {
        assertTrue(shouldEnqueueHistoryRefresh(lastRequestedAtMillis = 0L, nowMillis = 1L))
    }

    @Test
    fun conceptHistoryRequestInsideOneDayIsSuppressed() {
        val lastRequestedAt = 10_000L

        assertFalse(
            shouldEnqueueHistoryRefresh(
                lastRequestedAtMillis = lastRequestedAt,
                nowMillis = lastRequestedAt + MIN_HISTORY_REFRESH_INTERVAL_MILLIS - 1L,
            ),
        )
    }

    @Test
    fun conceptHistoryRequestAfterOneDayIsEnqueued() {
        val lastRequestedAt = 10_000L

        assertTrue(
            shouldEnqueueHistoryRefresh(
                lastRequestedAtMillis = lastRequestedAt,
                nowMillis = lastRequestedAt + MIN_HISTORY_REFRESH_INTERVAL_MILLIS,
            ),
        )
    }
}
