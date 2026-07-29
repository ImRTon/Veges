package tw.taipei.veges.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF006C45),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD7F8E7),
    onPrimaryContainer = Color(0xFF002114),
    secondary = Color(0xFF3E6653),
    surface = Color(0xFFFAFCFA),
    surfaceVariant = Color(0xFFE7ECE8),
    onSurface = Color(0xFF171C19),
    onSurfaceVariant = Color(0xFF414944),
    outline = Color(0xFF717A74),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF59DEA0),
    onPrimary = Color(0xFF003824),
    primaryContainer = Color(0xFF005235),
    onPrimaryContainer = Color(0xFF78FBB9),
    secondary = Color(0xFFA5CFB8),
    surface = Color(0xFF101512),
    surfaceVariant = Color(0xFF28312C),
    onSurface = Color(0xFFE1E4E1),
    onSurfaceVariant = Color(0xFFC0C9C2),
    outline = Color(0xFF8B948E),
    error = Color(0xFFFFB4AB),
)

@Composable
fun VegesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
