package tw.taipei.veges.data.sync

import javax.inject.Inject
import tw.taipei.veges.data.local.EstimateEntity

/**
 * Runtime estimation boundary. It returns no estimates until an approved, eligible model artifact
 * is bundled; this keeps the refresh chain fail-closed while still allowing the pipeline to be wired.
 */
fun interface EstimateRefreshCoordinator {
    suspend fun calculateNewEstimates(): List<EstimateEntity>
}

class FailClosedEstimateRefreshCoordinator @Inject constructor() : EstimateRefreshCoordinator {
    override suspend fun calculateNewEstimates(): List<EstimateEntity> = emptyList()
}
