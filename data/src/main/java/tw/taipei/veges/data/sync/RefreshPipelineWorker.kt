package tw.taipei.veges.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import tw.taipei.veges.data.alerts.AlertEvaluationCoordinator
import tw.taipei.veges.data.alerts.LocalNotificationPublisher

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RefreshWorkerEntryPoint {
    fun syncWorker(): SyncWorkerDelegate

    fun estimateRefreshCoordinator(): EstimateRefreshCoordinator

    fun alertEvaluationCoordinator(): AlertEvaluationCoordinator

    fun notificationPublisher(): LocalNotificationPublisher
}

class RefreshPipelineWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val dependencies = EntryPointAccessors.fromApplication(
            applicationContext,
            RefreshWorkerEntryPoint::class.java,
        )
        val syncResult = dependencies.syncWorker().run()
        if (syncResult is SyncResult.Failed) return Result.retry()

        dependencies.notificationPublisher().ensureChannels()
        val estimates = dependencies.estimateRefreshCoordinator().calculateNewEstimates()
        estimates.forEach { estimate ->
            dependencies.alertEvaluationCoordinator().evaluate(estimate, Instant.now())
        }
        return Result.success()
    }
}

fun interface SyncWorkerDelegate {
    suspend fun run(): SyncResult
}
