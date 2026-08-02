package tw.taipei.veges

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tw.taipei.veges.data.catalog.TaxonomyBundleImporter
import tw.taipei.veges.data.sync.SyncScheduler

@HiltAndroidApp
class VegesApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var taxonomyBundleImporter: TaxonomyBundleImporter

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            runCatching {
                syncScheduler.prepareWorkQueue()
                taxonomyBundleImporter.importAsset(this@VegesApplication)
            }.onSuccess {
                syncScheduler.markTaxonomyReadyAndRequestCatchUp()
            }.onFailure { failure ->
                Log.e(
                    "VegesBootstrap",
                    "Bundled taxonomy import failed: ${failure::class.java.simpleName}",
                )
            }
        }
    }
}
