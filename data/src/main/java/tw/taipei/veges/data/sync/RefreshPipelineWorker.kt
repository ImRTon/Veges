package tw.taipei.veges.data.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Instant
import tw.taipei.veges.data.alerts.AlertEvaluationCoordinator
import tw.taipei.veges.data.alerts.NotificationDeliveryCoordinator
import tw.taipei.veges.domain.PriceRefreshKind

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RefreshWorkerEntryPoint {
    fun syncWorker(): SyncWorkerDelegate

    fun syncStatusRepository(): SyncStatusRepository

    fun syncScheduler(): SyncScheduler

    fun estimateRefreshCoordinator(): EstimateRefreshCoordinator

    fun alertEvaluationCoordinator(): AlertEvaluationCoordinator

    fun notificationDeliveryCoordinator(): NotificationDeliveryCoordinator

    fun clock(): Clock
}

class RefreshPipelineWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return try {
            setForeground(createForegroundInfo())
            val dependencies = EntryPointAccessors.fromApplication(
                applicationContext,
                RefreshWorkerEntryPoint::class.java,
            )
            val requestedConceptId = inputData.getString(REQUESTED_CONCEPT_ID_KEY)
            val catalogHistory = inputData.getBoolean(REQUEST_CATALOG_HISTORY_KEY, false)
            val syncResult = dependencies.syncWorker().run(
                requestedConceptId = requestedConceptId,
                catalogHistory = catalogHistory,
            )
            if (syncResult is SyncResult.Failed) return retryOrFail()

            val now = dependencies.clock().instant()
            if (syncResult.hasPublishedSourceChanges()) {
                val estimates = dependencies.estimateRefreshCoordinator().calculateNewEstimates()
                estimates.forEach { estimate ->
                    dependencies.alertEvaluationCoordinator().evaluate(estimate, now)
                }
            }
            dependencies.notificationDeliveryCoordinator().deliverPending(now)
            if (requestedConceptId == null && !catalogHistory) {
                dependencies.syncScheduler().requestCatalogHistoryBackfill()
            }
            Result.success()
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            runCatching {
                EntryPointAccessors.fromApplication(
                    applicationContext,
                    RefreshWorkerEntryPoint::class.java,
                ).syncStatusRepository().recordFailure(
                    kind = if (inputData.getString(REQUESTED_CONCEPT_ID_KEY) != null ||
                        inputData.getBoolean(REQUEST_CATALOG_HISTORY_KEY, false)
                    ) PriceRefreshKind.HISTORY else PriceRefreshKind.LATEST,
                )
            }
            retryOrFail()
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notificationManager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "行情更新",
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = "下載最新與歷史價格時顯示進度"
                },
            )
        }
        val launchIntent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                applicationContext,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val notificationText = when {
            inputData.getBoolean(REQUEST_CATALOG_HISTORY_KEY, false) ->
                "正在下載歷史行情，完成後會自動更新"
            inputData.getString(REQUESTED_CONCEPT_ID_KEY) != null ->
                "正在下載品項歷史行情，完成後會自動更新"
            else -> "正在下載最新行情，完成後會自動更新"
        }
        val notification = NotificationCompat.Builder(
            applicationContext,
            NOTIFICATION_CHANNEL_ID,
        )
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("正在更新蔬果行情")
            .setContentText(notificationText)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .apply { contentIntent?.let(::setContentIntent) }
            .build()
        return ForegroundInfo(
            id.hashCode().and(Int.MAX_VALUE).coerceAtLeast(1),
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
    }

    private fun retryOrFail(): Result =
        if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()

    private companion object {
        const val MAX_RETRY_ATTEMPTS = 3
        const val NOTIFICATION_CHANNEL_ID = "price-refresh"
    }
}

fun interface SyncWorkerDelegate {
    suspend fun run(requestedConceptId: String?, catalogHistory: Boolean): SyncResult
}

internal fun SyncResult.hasPublishedSourceChanges(): Boolean =
    this is SyncResult.Published && (accepted > 0 || sourceDays > 0)
