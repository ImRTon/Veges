package tw.taipei.veges.data.repository

import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import tw.taipei.veges.data.network.CwaWeatherSignalClient
import tw.taipei.veges.domain.MarketShockRepository
import tw.taipei.veges.domain.MarketShockSignal

@Singleton
class CwaMarketShockRepository @Inject constructor(
    private val client: CwaWeatherSignalClient,
    private val clock: Clock,
) : MarketShockRepository {
    private val mutableSignals = MutableStateFlow<List<MarketShockSignal>>(emptyList())

    override val signals: Flow<List<MarketShockSignal>> = mutableSignals.asStateFlow()

    override suspend fun refresh() {
        val now = clock.instant()
        runCatching { client.fetchActiveSignals(now) }
            .onSuccess { mutableSignals.value = it }
            .onFailure {
                mutableSignals.value = mutableSignals.value.filter { signal ->
                    signal.expiresAt > now
                }
            }
    }
}
