package tw.taipei.veges.data.sync

import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import tw.taipei.veges.data.local.VegesDatabase
import tw.taipei.veges.domain.Freshness
import tw.taipei.veges.domain.PriceRefresh
import tw.taipei.veges.domain.PriceRefreshStage
import tw.taipei.veges.domain.UnavailableReason

@Singleton
class SyncStatusRepository @Inject constructor(
    private val database: VegesDatabase,
    private val clock: Clock,
) {
    private val mutableStatus = MutableStateFlow(
        SyncStatus(
            latestValidSourceDate = null,
            wholesaleFreshness = Freshness.UNKNOWN,
            lastSuccessfulRefresh = null,
            lastAttemptedRefresh = null,
            lastFailure = null,
        ),
    )

    val status: Flow<SyncStatus> = mutableStatus.asStateFlow()

    private val mutablePriceRefresh = MutableStateFlow(PriceRefresh())
    val priceRefresh: Flow<PriceRefresh> = mutablePriceRefresh.asStateFlow()

    fun recordStarted() {
        mutablePriceRefresh.value = PriceRefresh(
            isRunning = true,
            stage = PriceRefreshStage.PREPARING,
            fraction = 0.05f,
        )
    }

    fun recordProgress(stage: PriceRefreshStage, fraction: Float) {
        mutablePriceRefresh.value = PriceRefresh(
            isRunning = true,
            stage = stage,
            fraction = fraction.coerceIn(0f, 0.99f),
        )
    }

    suspend fun refreshFromDatabase() {
        val sourceDao = database.sourceDao()
        val now = clock.instant()
        val lastSuccessful = sourceDao.latestSuccessfulRefresh()
        mutableStatus.value = SyncStatus(
            latestValidSourceDate = sourceDao.latestValidObservationDate(),
            wholesaleFreshness = when {
                lastSuccessful == null -> Freshness.UNKNOWN
                Duration.between(lastSuccessful, now) <= CURRENT_AFTER -> Freshness.CURRENT
                else -> Freshness.STALE
            },
            lastSuccessfulRefresh = lastSuccessful,
            lastAttemptedRefresh = sourceDao.latestAttemptedRefresh(),
            lastFailure = null,
        )
        mutablePriceRefresh.value = PriceRefresh(
            isRunning = false,
            stage = PriceRefreshStage.SAVING,
            fraction = 1f,
        )
    }

    fun recordFailure(at: Instant = clock.instant()) {
        val current = mutableStatus.value
        mutableStatus.value = current.copy(
            wholesaleFreshness = if (current.latestValidSourceDate == null) Freshness.UNKNOWN else Freshness.STALE,
            lastAttemptedRefresh = at,
            lastFailure = UnavailableReason.FAILED_REFRESH,
        )
        mutablePriceRefresh.value = mutablePriceRefresh.value.copy(isRunning = false)
    }

    private companion object {
        val CURRENT_AFTER = Duration.ofHours(36)
    }
}
