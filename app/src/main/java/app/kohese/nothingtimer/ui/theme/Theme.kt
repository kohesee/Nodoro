package app.kohese.nothingtimer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ColorScheme
import app.kohese.nothingtimer.AccentColor
import app.kohese.nothingtimer.ThemeMode

private val AmoledDarkColorScheme = darkColorScheme(
    primary = Paper,
    onPrimary = Ink,
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Paper,
    secondary = Color(0xFFD0D0D0),
    onSecondary = Ink,
    secondaryContainer = Color(0xFF2E2E2E),
    onSecondaryContainer = Paper,
    tertiary = Color(0xFFBBBBBB),
    onTertiary = Ink,
    tertiaryContainer = Color(0xFF2A2A2A),
    onTertiaryContainer = Paper,
    background = Ink,
    onBackground = Paper,
    surface = Ink,
    onSurface = Paper,
    surfaceVariant = DarkGray,
    onSurfaceVariant = Color(0xFFC8C8C8),
    surfaceTint = Paper,
    surfaceDim = Ink,
    surfaceBright = Color(0xFF2A2A2A),
    surfaceContainerLowest = Ink,
    surfaceContainerLow = Color(0xFF0A0A0A),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF242424),
    outline = MidGray,
    outlineVariant = Color(0xFF3A3A3A)
)

private val StandardDarkColorScheme = darkColorScheme(
    primary = Paper,
    onPrimary = Ink,
    primaryContainer = Color(0xFF383838),
    onPrimaryContainer = Paper,
    secondary = Color(0xFFD0D0D0),
    onSecondary = Ink,
    secondaryContainer = Color(0xFF3A3A3A),
    onSecondaryContainer = Paper,
    tertiary = Color(0xFFBBBBBB),
    onTertiary = Ink,
    tertiaryContainer = Color(0xFF383838),
    onTertiaryContainer = Paper,
    background = SoftBlack,
    onBackground = Paper,
    surface = Color(0xFF1E1E1E),
    onSurface = Paper,
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFC8C8C8),
    surfaceTint = Paper,
    surfaceDim = SoftBlack,
    surfaceBright = Color(0xFF3A3A3A),
    surfaceContainerLowest = Color(0xFF0E0E0E),
    surfaceContainerLow = Color(0xFF1A1A1A),
    surfaceContainer = Color(0xFF202020),
    surfaceContainerHigh = Color(0xFF292929),
    surfaceContainerHighest = Color(0xFF333333),
    outline = MidGray,
    outlineVariant = Color(0xFF444444)
)

private val MonochromeLightColorScheme = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = Color(0xFFE2E2E2),
    onPrimaryContainer = Ink,
    secondary = Color(0xFF303030),
    onSecondary = Paper,
    secondaryContainer = Color(0xFFE0E0E0),
    onSecondaryContainer = Ink,
    tertiary = Color(0xFF444444),
    onTertiary = Paper,
    tertiaryContainer = Color(0xFFE2E2E2),
    onTertiaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = SoftWhite,
    onSurfaceVariant = Color(0xFF3A3A3A),
    surfaceTint = Ink,
    surfaceDim = Color(0xFFDDDDDD),
    surfaceBright = Paper,
    surfaceContainerLowest = Paper,
    surfaceContainerLow = Color(0xFFF7F7F7),
    surfaceContainer = Color(0xFFF1F1F1),
    surfaceContainerHigh = Color(0xFFEBEBEB),
    surfaceContainerHighest = Color(0xFFE4E4E4),
    outline = MidGray,
    outlineVariant = Color(0xFFCCCCCC)
)

/** Builds a full tonal scheme (accent plus faintly tinted neutrals) from a single hue. */
private fun accentColorScheme(hue: Float, dark: Boolean, amoled: Boolean): ColorScheme {
    fun c(s: Float, l: Float) = Color.hsl(hue, s, l)
    val tertiaryHue = (hue + 60f) % 360f
    fun t(s: Float, l: Float) = Color.hsl(tertiaryHue, s, l)
    return if (dark) {
        val bg = if (amoled) Color.Black else c(0.14f, 0.07f)
        darkColorScheme(
            primary = c(0.85f, 0.76f), onPrimary = c(0.9f, 0.14f),
            primaryContainer = c(0.55f, 0.28f), onPrimaryContainer = c(0.9f, 0.9f),
            secondary = c(0.35f, 0.76f), onSecondary = c(0.4f, 0.15f),
            secondaryContainer = c(0.25f, 0.28f), onSecondaryContainer = c(0.5f, 0.9f),
            tertiary = t(0.55f, 0.76f), onTertiary = t(0.6f, 0.15f),
            tertiaryContainer = t(0.35f, 0.28f), onTertiaryContainer = t(0.6f, 0.9f),
            background = bg, onBackground = c(0.1f, 0.92f),
            surface = bg, onSurface = c(0.1f, 0.92f),
            surfaceVariant = c(0.15f, 0.24f), onSurfaceVariant = c(0.1f, 0.8f),
            surfaceTint = c(0.85f, 0.76f),
            surfaceDim = bg, surfaceBright = c(0.12f, 0.24f),
            surfaceContainerLowest = if (amoled) Color.Black else c(0.14f, 0.05f),
            surfaceContainerLow = if (amoled) c(0.1f, 0.06f) else c(0.14f, 0.09f),
            surfaceContainer = if (amoled) c(0.1f, 0.09f) else c(0.14f, 0.12f),
            surfaceContainerHigh = if (amoled) c(0.1f, 0.12f) else c(0.14f, 0.16f),
            surfaceContainerHighest = if (amoled) c(0.1f, 0.16f) else c(0.14f, 0.2f),
            outline = c(0.1f, 0.6f), outlineVariant = c(0.12f, 0.3f)
        )
    } else {
        lightColorScheme(
            primary = c(0.7f, 0.38f), onPrimary = Color.White,
            primaryContainer = c(0.85f, 0.9f), onPrimaryContainer = c(0.9f, 0.1f),
            secondary = c(0.3f, 0.4f), onSecondary = Color.White,
            secondaryContainer = c(0.4f, 0.9f), onSecondaryContainer = c(0.5f, 0.12f),
            tertiary = t(0.45f, 0.4f), onTertiary = Color.White,
            tertiaryContainer = t(0.6f, 0.9f), onTertiaryContainer = t(0.7f, 0.12f),
            background = c(0.4f, 0.98f), onBackground = c(0.2f, 0.1f),
            surface = c(0.4f, 0.98f), onSurface = c(0.2f, 0.1f),
            surfaceVariant = c(0.25f, 0.9f), onSurfaceVariant = c(0.15f, 0.3f),
            surfaceTint = c(0.7f, 0.38f),
            surfaceDim = c(0.2f, 0.87f), surfaceBright = c(0.4f, 0.98f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = c(0.35f, 0.96f),
            surfaceContainer = c(0.3f, 0.94f),
            surfaceContainerHigh = c(0.28f, 0.92f),
            surfaceContainerHighest = c(0.25f, 0.9f),
            outline = c(0.1f, 0.5f), outlineVariant = c(0.2f, 0.8f)
        )
    }
}
@Composable
fun NodoroTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    amoled: Boolean = false,
    dynamicColor: Boolean = true,
    accent: AccentColor = AccentColor.MONO,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor -> {
            val dynamic = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (isDark && amoled) {
                dynamic.copy(background = Color.Black, surface = Color.Black)
            } else {
                dynamic
            }
        }
        accent != AccentColor.MONO -> accentColorScheme(accent.hue, isDark, isDark && amoled)
        isDark -> {
            if (amoled) AmoledDarkColorScheme else StandardDarkColorScheme
        }
        else -> MonochromeLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}