package tw.taipei.veges.data.sync

import org.junit.Assert.assertTrue
import org.junit.Test

class SyncSchedulerContractTest {
    @Test
    fun refreshPipelineAcceptsAQualifiedEstimatorBoundary() = kotlinx.coroutines.test.runTest {
        val coordinator = EstimateRefreshCoordinator { emptyList() }
        val estimates = coordinator.calculateNewEstimates()

        assertTrue(estimates.isEmpty())
    }
}
