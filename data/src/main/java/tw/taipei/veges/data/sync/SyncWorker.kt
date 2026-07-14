package tw.taipei.veges.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val coordinator: WholesaleSyncCoordinator,
    private val statusRepository: SyncStatusRepository,
    private val historyRetention: HistoryRetention,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = try {
        when (coordinator.synchronize()) {
            is SyncResult.Published -> {
                statusRepository.refreshFromDatabase()
                historyRetention.pruneReplaceableWholesaleHistory()
                Result.success()
            }
            is SyncResult.Failed -> {
                statusRepository.recordFailure()
                if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
            }
        }
    } catch (failure: Exception) {
        statusRepository.recordFailure()
        if (runAttemptCount < MAX_RETRY_ATTEMPTS) Result.retry() else Result.failure()
    }

    private companion object {
        const val MAX_RETRY_ATTEMPTS = 3
    }
}
