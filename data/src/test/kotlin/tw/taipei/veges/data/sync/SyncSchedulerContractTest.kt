package tw.taipei.veges.data.sync

import org.junit.Assert.assertTrue
import org.junit.Test

class SyncSchedulerContractTest {
    @Test
    fun refreshPipelineUsesFailClosedEstimatorUntilArtifactIsApproved() = kotlinx.coroutines.test.runTest {
        val estimates = FailClosedEstimateRefreshCoordinator().calculateNewEstimates()
        assertTrue(estimates.isEmpty())
    }
}
