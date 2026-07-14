package tw.taipei.veges.data.repository

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import tw.taipei.veges.data.local.VegesDatabase
import tw.taipei.veges.data.local.TaxonomyConceptWithDetails
import tw.taipei.veges.data.sync.SyncScheduler
import tw.taipei.veges.domain.Estimate
import tw.taipei.veges.domain.HomeItem
import tw.taipei.veges.domain.HomeRepository
import tw.taipei.veges.domain.ProduceConcept
import tw.taipei.veges.domain.UnavailableReason

class RoomHomeRepository @Inject constructor(
    private val database: VegesDatabase,
    private val syncScheduler: SyncScheduler,
) : HomeRepository {
    override fun observeHome(): Flow<List<HomeItem>> = combine(
        database.taxonomyDao().observeTrackedConcepts(),
        database.estimateDao().observeTrackedLatest(),
    ) { concepts, estimates ->
        val estimateByConcept = estimates
            .groupBy { it.conceptId }
            .mapValues { (_, rows) -> rows.maxWithOrNull(compareBy({ it.sourceDate }, { it.calculatedAt })) }
        concepts.map { conceptEntity ->
            val concept = TaxonomyConceptWithDetails(
                concept = conceptEntity,
                aliases = emptyList(),
                variants = emptyList(),
            ).toDomain()
            val latest = estimateByConcept[conceptEntity.stableId]?.toDomain()
            HomeItem(
                concept = concept,
                latestEstimate = latest,
                unavailableReason = latest?.unavailableReason ?: UnavailableReason.MISSING_SOURCE_DATA.takeIf { latest == null },
            )
        }
    }

    override fun requestRefresh() {
        syncScheduler.requestForegroundCatchUp()
    }
}
