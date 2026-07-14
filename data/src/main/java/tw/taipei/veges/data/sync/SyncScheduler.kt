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
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class SyncScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val workManager: WorkManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        WorkManager.getInstance(context)
    }

    fun ensurePeriodicRefresh() {
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RefreshPipelineWorker>(1, TimeUnit.DAYS)
                .setConstraints(networkConstraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build(),
        )
    }

    fun requestForegroundCatchUp() {
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
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    }
}
