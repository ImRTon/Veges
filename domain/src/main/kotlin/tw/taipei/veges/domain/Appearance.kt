package tw.taipei.veges.domain

import kotlinx.coroutines.flow.StateFlow

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

interface AppearanceRepository {
    val themeMode: StateFlow<ThemeMode>

    fun setThemeMode(mode: ThemeMode)
}
