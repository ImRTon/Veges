package tw.taipei.veges.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tw.taipei.veges.data.sync.EstimateRefreshCoordinator
import tw.taipei.veges.data.sync.RoomTemporaryFactorEstimateRefreshCoordinator

@Module
@InstallIn(SingletonComponent::class)
abstract class EstimationModule {
    @Binds
    abstract fun bindEstimateRefreshCoordinator(
        coordinator: RoomTemporaryFactorEstimateRefreshCoordinator,
    ): EstimateRefreshCoordinator
}
