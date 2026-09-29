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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    suspend fun prepareWorkQueue() = withContext(Dispatchers.IO) {
        val queueVersion = preferences.getInt(SYNC_QUEUE_VERSION, 0)
        if (queueVersion < CURRENT_SYNC_QUEUE_VERSION) {
            workManager.cancelAllWorkByTag(RefreshPipelineWorker::class.java.name).result.get()
            val migrationEditor = preferences.edit()
                .putInt(SYNC_QUEUE_VERSION, CURRENT_SYNC_QUEUE_VERSION)
                .remove(LAST_FOREGROUND_REQUEST_AT)
                .remove(LAST_CATALOG_HISTORY_REQUEST_AT)
            preferences.all.keys
                .filter { it.startsWith(LAST_HISTORY_REQUEST_PREFIX) }
                .forEach(migrationEditor::remove)
            check(
                migrationEditor.commit(),
            ) {
                "Could not persist sync queue migration"
            }
        }
        ensurePeriodicRefresh()
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
        val taxonomyReady = preferences.getBoolean(TAXONOMY_READY, false)
        if (!shouldEnqueueForegroundCatchUp(taxonomyReady, lastRequestedAtMillis, nowMillis)) return
        if (!preferences.edit().putLong(LAST_FOREGROUND_REQUEST_AT, nowMillis).commit()) return
        enqueueImmediateRefresh()
    }

    fun markTaxonomyReadyAndRequestCatchUp() {
        if (!preferences.edit().putBoolean(TAXONOMY_READY, true).commit()) return
        requestForegroundCatchUp()
    }

    suspend fun requestManualRefresh(): Boolean = withContext(Dispatchers.IO) {
        if (!preferences.getBoolean(TAXONOMY_READY, false)) return@withContext false
        val pending = workManager.getWorkInfosForUniqueWork(ONE_TIME_SYNC_QUEUE)
            .get()
            .filterNot { it.state.isFinished }
        val latestAlreadyPending = pending.any { LATEST_WORK_TAG in it.tags }
        enqueueImmediateRefresh(
            existingWorkPolicy = if (pending.isNotEmpty() && !latestAlreadyPending) {
                ExistingWorkPolicy.APPEND_OR_REPLACE
            } else {
                MANUAL_REFRESH_WORK_POLICY
            },
        )
        true
    }

    override fun requestOneYearHistory(conceptId: ProduceConceptId) {
        val preferenceKey = "$LAST_HISTORY_REQUEST_PREFIX${conceptId.value}"
        val nowMillis = System.currentTimeMillis()
        val lastRequestedAtMillis = preferences.getLong(preferenceKey, 0L)
        if (!shouldEnqueueHistoryRefresh(lastRequestedAtMillis, nowMillis)) return
        if (!preferences.edit().putLong(preferenceKey, nowMillis).commit()) return
        enqueueOneTimeRefresh(
            requestedConceptId = conceptId.value,
            catalogHistory = false,
        )
    }

    private fun enqueueImmediateRefresh(
        existingWorkPolicy: ExistingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE,
    ) {
        enqueueOneTimeRefresh(
            requestedConceptId = null,
            catalogHistory = false,
            existingWorkPolicy = existingWorkPolicy,
        )
    }

    fun requestCatalogHistoryBackfill() {
        if (!preferences.getBoolean(TAXONOMY_READY, false)) return
        val nowMillis = System.currentTimeMillis()
        val lastRequestedAtMillis = preferences.getLong(LAST_CATALOG_HISTORY_REQUEST_AT, 0L)
        if (!shouldEnqueueCatalogHistoryBackfill(lastRequestedAtMillis, nowMillis)) return
        if (!preferences.edit().putLong(LAST_CATALOG_HISTORY_REQUEST_AT, nowMillis).commit()) return
        enqueueOneTimeRefresh(
            requestedConceptId = null,
            catalogHistory = true,
        )
    }

    private fun enqueueOneTimeRefresh(
        requestedConceptId: String?,
        catalogHistory: Boolean,
        existingWorkPolicy: ExistingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE,
    ) {
        val request = OneTimeWorkRequestBuilder<RefreshPipelineWorker>()
            .apply {
                when {
                    requestedConceptId != null -> {
                        addTag(HISTORY_WORK_TAG)
                        setInputData(workDataOf(REQUESTED_CONCEPT_ID_KEY to requestedConceptId))
                    }
                    catalogHistory -> {
                        addTag(HISTORY_WORK_TAG)
                        setInputData(workDataOf(REQUEST_CATALOG_HISTORY_KEY to true))
                    }
                    else -> addTag(LATEST_WORK_TAG)
                }
            }
            .setConstraints(networkConstraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(
            ONE_TIME_SYNC_QUEUE,
            existingWorkPolicy,
            request,
        )
    }

    fun cancelAllSyncWork() {
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
        workManager.cancelUniqueWork(ONE_TIME_SYNC_QUEUE)
    }

    private companion object {
        const val PERIODIC_WORK_NAME = "veges.periodic-sync"
        const val ONE_TIME_SYNC_QUEUE = "veges.one-time-sync"
        const val SCHEDULER_PREFERENCES = "veges.sync-scheduler"
        const val SYNC_QUEUE_VERSION = "sync-queue-version"
        const val CURRENT_SYNC_QUEUE_VERSION = 2
        const val LAST_FOREGROUND_REQUEST_AT = "last-foreground-request-at"
        const val LAST_CATALOG_HISTORY_REQUEST_AT = "last-catalog-history-request-at"
        const val TAXONOMY_READY = "taxonomy-ready"
        const val LAST_HISTORY_REQUEST_PREFIX = "last-history-request-at:"
        const val LATEST_WORK_TAG = "veges.latest-prices"
        const val HISTORY_WORK_TAG = "veges.history"
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}

internal val MIN_FOREGROUND_CATCH_UP_INTERVAL_MILLIS: Long = TimeUnit.MINUTES.toMillis(15)
internal val MIN_HISTORY_REFRESH_INTERVAL_MILLIS: Long = TimeUnit.HOURS.toMillis(24)
internal val MIN_CATALOG_HISTORY_BACKFILL_INTERVAL_MILLIS: Long = TimeUnit.HOURS.toMillis(24)
internal val MANUAL_REFRESH_WORK_POLICY: ExistingWorkPolicy = ExistingWorkPolicy.KEEP
internal const val REQUESTED_CONCEPT_ID_KEY = "requested-concept-id"
internal const val REQUEST_CATALOG_HISTORY_KEY = "request-catalog-history"

internal fun shouldEnqueueForegroundCatchUp(
    taxonomyReady: Boolean,
    lastRequestedAtMillis: Long,
    nowMillis: Long,
): Boolean {
    if (!taxonomyReady) return false
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

internal fun shouldEnqueueCatalogHistoryBackfill(
    lastRequestedAtMillis: Long,
    nowMillis: Long,
): Boolean {
    if (lastRequestedAtMillis <= 0L) return true
    val elapsed = nowMillis - lastRequestedAtMillis
    return elapsed < 0L || elapsed >= MIN_CATALOG_HISTORY_BACKFILL_INTERVAL_MILLIS
}
