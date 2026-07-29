package tw.taipei.veges.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshPipelineContractTest {
    @Test
    fun estimatorBoundaryCanFailClosedWithoutPublishingRows() = kotlinx.coroutines.test.runTest {
        val coordinator = EstimateRefreshCoordinator { emptyList() }

        assertEquals(0, coordinator.calculateNewEstimates().size)
    }
}
