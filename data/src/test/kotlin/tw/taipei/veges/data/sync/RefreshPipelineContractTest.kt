package tw.taipei.veges.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshPipelineContractTest {
    @Test
    fun failClosedEstimatorProducesNoUnqualifiedEstimates() = kotlinx.coroutines.test.runTest {
        assertEquals(0, FailClosedEstimateRefreshCoordinator().calculateNewEstimates().size)
    }
}
