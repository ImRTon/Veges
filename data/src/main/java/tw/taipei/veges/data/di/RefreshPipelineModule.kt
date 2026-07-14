package tw.taipei.veges.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tw.taipei.veges.data.sync.SyncWorker
import tw.taipei.veges.data.sync.SyncWorkerDelegate
import tw.taipei.veges.data.sync.WholesaleSyncCoordinator

@Module
@InstallIn(SingletonComponent::class)
abstract class RefreshPipelineModule {
    @Binds
    abstract fun bindSyncWorkerDelegate(delegate: SyncWorkerDelegateImpl): SyncWorkerDelegate
}

class SyncWorkerDelegateImpl @javax.inject.Inject constructor(
    private val coordinator: WholesaleSyncCoordinator,
) : SyncWorkerDelegate {
    override suspend fun run() = coordinator.synchronize()
}
