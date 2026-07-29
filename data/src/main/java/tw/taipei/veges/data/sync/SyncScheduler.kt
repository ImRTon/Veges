package tw.taipei.veges.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext
import tw.taipei.veges.domain.HistoryRefreshRequester
import tw.taipei.veges.domain.ProduceConceptId

class SyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : HistoryRefreshRequester {
    private val workManager: WorkManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        WorkManager.getInstance(context)
    }
    private val preferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        context.getSharedPreferences(SCHEDULER_PREFERENCES, Context.MODE_PRIVATE)
    }

    fun ensurePeriodicRefresh() {
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RefreshPipelineWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(1, TimeUnit.DAYS)
                .setConstraints(networkConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build(),
        )
    }

    fun requestForegroundCatchUp() {
        val nowMillis = System.currentTimeMillis()
        val lastRequestedAtMillis = preferences.getLong(LAST_FOREGROUND_REQUEST_AT, 0L)
        if (!shouldEnqueueForegroundCatchUp(lastRequestedAtMillis, nowMillis)) return
        if (!preferences.edit().putLong(LAST_FOREGROUND_REQUEST_AT, nowMillis).commit()) return
        enqueueImmediateRefresh()
    }

    fun requestManualRefresh() {
        enqueueImmediateRefresh()
    }

    override fun requestOneYearHistory(conceptId: ProduceConceptId) {
        val preferenceKey = "$LAST_HISTORY_REQUEST_PREFIX${conceptId.value}"
        val nowMillis = System.currentTimeMillis()
        val lastRequestedAtMillis = preferences.getLong(preferenceKey, 0L)
        if (!shouldEnqueueHistoryRefresh(lastRequestedAtMillis, nowMillis)) return
        if (!preferences.edit().putLong(preferenceKey, nowMillis).commit()) return
        workManager.enqueueUniqueWork(
            "$HISTORY_WORK_PREFIX${conceptId.value}",
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<RefreshPipelineWorker>()
                .setInputData(workDataOf(REQUESTED_CONCEPT_ID_KEY to conceptId.value))
                .setConstraints(networkConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build(),
        )
    }

    private fun enqueueImmediateRefresh() {
        workManager.enqueueUniqueWork(
            FOREGROUND_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<RefreshPipelineWorker>()
                .setConstraints(networkConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build(),
        )
    }

    fun cancelAllSyncWork() {
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
        workManager.cancelUniqueWork(FOREGROUND_WORK_NAME)
    }

    private companion object {
        const val PERIODIC_WORK_NAME = "veges.periodic-sync"
        const val FOREGROUND_WORK_NAME = "veges.foreground-catch-up"
        const val SCHEDULER_PREFERENCES = "veges.sync-scheduler"
        const val LAST_FOREGROUND_REQUEST_AT = "last-foreground-request-at"
        const val LAST_HISTORY_REQUEST_PREFIX = "last-history-request-at:"
        const val HISTORY_WORK_PREFIX = "veges.concept-history."
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}

internal val MIN_FOREGROUND_CATCH_UP_INTERVAL_MILLIS: Long = TimeUnit.MINUTES.toMillis(15)
internal val MIN_HISTORY_REFRESH_INTERVAL_MILLIS: Long = TimeUnit.HOURS.toMillis(24)
internal const val REQUESTED_CONCEPT_ID_KEY = "requested-concept-id"

internal fun shouldEnqueueForegroundCatchUp(
    lastRequestedAtMillis: Long,
    nowMillis: Long,
): Boolean {
    if (lastRequestedAtMillis <= 0L) return true
    val elapsed = nowMillis - lastRequestedAtMillis
    return elapsed < 0L || elapsed >= MIN_FOREGROUND_CATCH_UP_INTERVAL_MILLIS
}

internal fun shouldEnqueueHistoryRefresh(
    lastRequestedAtMillis: Long,
    nowMillis: Long,
): Boolean {
    if (lastRequestedAtMillis <= 0L) return true
    val elapsed = nowMillis - lastRequestedAtMillis
    return elapsed < 0L || elapsed >= MIN_HISTORY_REFRESH_INTERVAL_MILLIS
}
