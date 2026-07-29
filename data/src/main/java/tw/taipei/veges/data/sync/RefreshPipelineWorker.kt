package tw.taipei.veges.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import java.time.Instant
import tw.taipei.veges.data.alerts.AlertEvaluationCoordinator
import tw.taipei.veges.data.alerts.NotificationDeliveryCoordinator

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RefreshWorkerEntryPoint {
    fun syncWorker(): SyncWorkerDelegate

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
            val dependencies = EntryPointAccessors.fromApplication(
                applicationContext,
                RefreshWorkerEntryPoint::class.java,
            )
            val syncResult = dependencies.syncWorker().run(
                inputData.getString(REQUESTED_CONCEPT_ID_KEY),
            )
            if (syncResult is SyncResult.Failed) return retryOrFail()

            val now = dependencies.clock().instant()
            val estimates = dependencies.estimateRefreshCoordinator().calculateNewEstimates()
            estimates.forEach { estimate ->
                dependencies.alertEvaluationCoordinator().evaluate(estimate, now)
            }
            dependencies.notificationDeliveryCoordinator().deliverPending(now)
            Result.success()
        } catch (cancellation: kotlinx.coroutines.CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            retryOrFail()
        }
    }

    private fun retryOrFail(): Result =
        if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()

    private companion object {
        const val MAX_RETRY_ATTEMPTS = 3
    }
}

fun interface SyncWorkerDelegate {
    suspend fun run(requestedConceptId: String?): SyncResult
}
