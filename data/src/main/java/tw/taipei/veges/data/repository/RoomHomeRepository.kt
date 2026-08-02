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
import tw.taipei.veges.domain.PriceRefresh
import tw.taipei.veges.domain.ProduceConcept
import tw.taipei.veges.domain.UnavailableReason
import tw.taipei.veges.data.sync.SyncStatusRepository

class RoomHomeRepository @Inject constructor(
    private val database: VegesDatabase,
    private val syncScheduler: SyncScheduler,
    private val syncStatusRepository: SyncStatusRepository,
) : HomeRepository {
    override fun observeHome(): Flow<List<HomeItem>> = combine(
        database.taxonomyDao().observeTrackedConcepts(),
        database.estimateDao().observeTrackedHistory(),
        database.alertDao().observeAllRules(),
    ) { concepts, estimates, alertRules ->
        val estimateByConcept = estimates
            .groupBy { it.conceptId }
            .mapValues { (_, rows) ->
                rows.sortedWith(compareByDescending<tw.taipei.veges.data.local.EstimateEntity> { it.sourceDate }
                    .thenByDescending { it.calculatedAt })
            }
        concepts.map { conceptEntity ->
            val concept = TaxonomyConceptWithDetails(
                concept = conceptEntity,
                aliases = emptyList(),
                variants = emptyList(),
            ).toDomain()
            val ordered = estimateByConcept[conceptEntity.stableId].orEmpty()
            val latestEntity = ordered.firstOrNull()
            val latest = latestEntity?.toDomain()
            val previous = latestEntity?.let { current ->
                ordered.firstOrNull { it.basis == current.basis && it.sourceDate < current.sourceDate }
            }?.toDomain()
            HomeItem(
                concept = concept,
                latestEstimate = latest,
                previousEstimate = previous,
                activeAlertCount = alertRules.count {
                    it.conceptId == conceptEntity.stableId && it.enabled
                },
                unavailableReason = latest?.unavailableReason ?: UnavailableReason.MISSING_SOURCE_DATA.takeIf { latest == null },
            )
        }
    }

    override fun observePriceRefresh(): Flow<PriceRefresh> = syncStatusRepository.priceRefresh

    override fun requestRefresh() {
        syncScheduler.requestManualRefresh()
    }
}
