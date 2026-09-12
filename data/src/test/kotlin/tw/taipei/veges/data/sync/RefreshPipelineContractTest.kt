package tw.taipei.veges.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshPipelineContractTest {
    @Test
    fun zeroChangePublicationSkipsEstimateAndAlertRecomputation() {
        assertFalse(
            SyncResult.Published(
                runId = "unchanged",
                accepted = 0,
                invalid = 0,
                sourceDays = 0,
            ).hasPublishedSourceChanges(),
        )
    }

    @Test
    fun acceptedRecordsTriggerDownstreamProcessing() {
        assertTrue(
            SyncResult.Published(
                runId = "records",
                accepted = 1,
                invalid = 0,
                sourceDays = 0,
            ).hasPublishedSourceChanges(),
        )
    }

    @Test
    fun discoveredSourceDaysTriggerDownstreamProcessing() {
        assertTrue(
            SyncResult.Published(
                runId = "days",
                accepted = 0,
                invalid = 0,
                sourceDays = 1,
            ).hasPublishedSourceChanges(),
        )
    }

    @Test
    fun failedSyncNeverTriggersDownstreamProcessing() {
        assertFalse(
            SyncResult.Failed(runId = "failed", reason = "network")
                .hasPublishedSourceChanges(),
        )
    }

    @Test
    fun estimatorBoundaryCanFailClosedWithoutPublishingRows() = kotlinx.coroutines.test.runTest {
        val coordinator = EstimateRefreshCoordinator { emptyList() }

        assertEquals(0, coordinator.calculateNewEstimates().size)
    }
}
