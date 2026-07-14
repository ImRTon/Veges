package tw.taipei.veges.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import tw.taipei.veges.data.network.OfficialHttpTransport
import tw.taipei.veges.data.network.OkHttpOfficialHttpTransport

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    @Binds
    abstract fun bindOfficialHttpTransport(transport: OkHttpOfficialHttpTransport): OfficialHttpTransport
}
