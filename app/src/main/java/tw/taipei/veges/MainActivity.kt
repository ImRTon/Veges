package tw.taipei.veges

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import tw.taipei.veges.data.sync.SyncScheduler
import tw.taipei.veges.designsystem.VegesTheme
import tw.taipei.veges.domain.AppearanceRepository
import tw.taipei.veges.domain.ThemeMode

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { }

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var appearanceRepository: AppearanceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by appearanceRepository.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        Color.TRANSPARENT,
                        Color.TRANSPARENT,
                    ) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(
                        LightNavigationScrim,
                        DarkNavigationScrim,
                    ) { darkTheme },
                )
                onDispose { }
            }
            VegesTheme(darkTheme = darkTheme) {
                VegesApp(onRequestNotificationPermission = ::requestNotificationPermission)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        syncScheduler.requestForegroundCatchUp()
    }

    fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private companion object {
        val LightNavigationScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DarkNavigationScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
