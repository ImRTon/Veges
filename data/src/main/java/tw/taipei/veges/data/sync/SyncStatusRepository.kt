package tw.taipei.veges.data.sync

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tw.taipei.veges.data.local.VegesDatabase
import tw.taipei.veges.domain.Freshness
import tw.taipei.veges.domain.PriceRefresh
import tw.taipei.veges.domain.PriceRefreshKind
import tw.taipei.veges.domain.PriceRefreshOutcome
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
    val priceRefresh: StateFlow<PriceRefresh> = mutablePriceRefresh.asStateFlow()
    private var sourceDateBeforeRefresh: LocalDate? = null

    suspend fun recordStarted(kind: PriceRefreshKind = PriceRefreshKind.LATEST) {
        sourceDateBeforeRefresh = database.sourceDao().latestValidObservationDate()
        mutablePriceRefresh.value = mutablePriceRefresh.value.copy(
            isRunning = true,
            stage = PriceRefreshStage.PREPARING,
            fraction = 0.05f,
            kind = kind,
            outcome = null,
        )
    }

    fun recordProgress(stage: PriceRefreshStage, fraction: Float) {
        mutablePriceRefresh.value = mutablePriceRefresh.value.copy(
            isRunning = true,
            stage = stage,
            fraction = fraction.coerceIn(0f, 0.99f),
            outcome = null,
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
        val previousRefresh = mutablePriceRefresh.value
        mutablePriceRefresh.value = previousRefresh.copy(
            isRunning = false,
            stage = PriceRefreshStage.SAVING,
            fraction = 1f,
            completionVersion = previousRefresh.completionVersion + 1,
            outcome = if (mutableStatus.value.latestValidSourceDate?.isAfter(
                    sourceDateBeforeRefresh ?: LocalDate.MIN,
                ) == true
            ) PriceRefreshOutcome.NEW_DATE else PriceRefreshOutcome.SAME_DATE,
        )
    }

    fun recordFailure(
        at: Instant = clock.instant(),
        kind: PriceRefreshKind? = null,
    ) {
        val current = mutableStatus.value
        mutableStatus.value = current.copy(
            wholesaleFreshness = if (current.latestValidSourceDate == null) Freshness.UNKNOWN else Freshness.STALE,
            lastAttemptedRefresh = at,
            lastFailure = UnavailableReason.FAILED_REFRESH,
        )
        val previousRefresh = mutablePriceRefresh.value
        mutablePriceRefresh.value = previousRefresh.copy(
            isRunning = false,
            kind = kind ?: previousRefresh.kind,
            completionVersion = previousRefresh.completionVersion + 1,
            outcome = PriceRefreshOutcome.FAILED,
        )
    }

    private companion object {
        val CURRENT_AFTER = Duration.ofHours(36)
    }
}
