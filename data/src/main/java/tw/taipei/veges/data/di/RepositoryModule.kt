package tw.taipei.veges.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tw.taipei.veges.data.repository.RoomProduceRepository
import tw.taipei.veges.data.repository.RoomTrackingRepository
import tw.taipei.veges.domain.AlertRuleRepository
import tw.taipei.veges.data.repository.RoomHomeRepository
import tw.taipei.veges.data.repository.RoomDetailRepository
import tw.taipei.veges.domain.DetailRepository
import tw.taipei.veges.domain.HomeRepository
import tw.taipei.veges.domain.MarketShockRepository
import tw.taipei.veges.domain.ProduceRepository
import tw.taipei.veges.domain.TrackingRepository
import tw.taipei.veges.data.repository.CwaMarketShockRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindProduceRepository(repository: RoomProduceRepository): ProduceRepository

    @Binds
    abstract fun bindTrackingRepository(repository: RoomTrackingRepository): TrackingRepository

    @Binds
    abstract fun bindAlertRuleRepository(repository: RoomTrackingRepository): AlertRuleRepository

    @Binds
    abstract fun bindHomeRepository(repository: RoomHomeRepository): HomeRepository

    @Binds
    abstract fun bindDetailRepository(repository: RoomDetailRepository): DetailRepository

    @Binds
    abstract fun bindMarketShockRepository(
        repository: CwaMarketShockRepository,
    ): MarketShockRepository
}
