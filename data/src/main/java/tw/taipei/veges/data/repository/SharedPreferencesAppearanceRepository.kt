package tw.taipei.veges.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tw.taipei.veges.domain.AppearanceRepository
import tw.taipei.veges.domain.ThemeMode

@Singleton
class SharedPreferencesAppearanceRepository @Inject constructor(
    @ApplicationContext context: Context,
) : AppearanceRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val mutableThemeMode = MutableStateFlow(
        preferences.getString(THEME_MODE_KEY, null)
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.SYSTEM,
    )

    override val themeMode: StateFlow<ThemeMode> = mutableThemeMode.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        preferences.edit().putString(THEME_MODE_KEY, mode.name).apply()
        mutableThemeMode.value = mode
    }

    private companion object {
        const val PREFERENCES_NAME = "appearance"
        const val THEME_MODE_KEY = "theme-mode"
    }
}
