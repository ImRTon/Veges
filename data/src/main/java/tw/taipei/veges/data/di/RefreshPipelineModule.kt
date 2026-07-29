package tw.taipei.veges.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tw.taipei.veges.data.sync.SyncWorker
import tw.taipei.veges.data.sync.SyncWorkerDelegate
import tw.taipei.veges.data.sync.WholesaleSyncCoordinator
import tw.taipei.veges.data.sync.SyncScheduler
import tw.taipei.veges.domain.HistoryRefreshRequester

@Module
@InstallIn(SingletonComponent::class)
abstract class RefreshPipelineModule {
    @Binds
    abstract fun bindSyncWorkerDelegate(delegate: SyncWorkerDelegateImpl): SyncWorkerDelegate

    @Binds
    abstract fun bindHistoryRefreshRequester(scheduler: SyncScheduler): HistoryRefreshRequester
}

class SyncWorkerDelegateImpl @javax.inject.Inject constructor(
    private val coordinator: WholesaleSyncCoordinator,
    private val statusRepository: tw.taipei.veges.data.sync.SyncStatusRepository,
    private val historyRetention: tw.taipei.veges.data.sync.HistoryRetention,
) : SyncWorkerDelegate {
    override suspend fun run(requestedConceptId: String?): tw.taipei.veges.data.sync.SyncResult {
        val result = if (requestedConceptId == null) {
            coordinator.synchronize()
        } else {
            coordinator.synchronizeConceptHistory(requestedConceptId)
        }
        when (result) {
            is tw.taipei.veges.data.sync.SyncResult.Published -> {
                statusRepository.refreshFromDatabase()
                historyRetention.pruneReplaceableWholesaleHistory()
            }
            is tw.taipei.veges.data.sync.SyncResult.Failed -> statusRepository.recordFailure()
        }
        return result
    }
}
