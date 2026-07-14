package tw.taipei.veges

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import tw.taipei.veges.data.sync.SyncScheduler

@HiltAndroidApp
class VegesApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        syncScheduler.ensurePeriodicRefresh()
    }
}
