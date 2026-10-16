package com.cir0cuit.devtoys.core.designsystem

import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val PlexSans = FontFamily(
    Font(R.font.plex_sans_regular, FontWeight.Normal),
    Font(R.font.plex_sans_medium, FontWeight.Medium),
    Font(R.font.plex_sans_semibold, FontWeight.SemiBold),
)

val PlexMono = FontFamily(
    Font(R.font.plex_mono_regular, FontWeight.Normal),
    Font(R.font.plex_mono_medium, FontWeight.Medium),
)

/** Code and data text: inputs, outputs, hashes, tables. */
val CodeTextStyle = TextStyle(fontFamily = PlexMono, fontSize = 14.sp, lineHeight = 21.sp)

/** Recessed surface for code panes, darker than the page in both themes. */
val LocalSlabColor = staticCompositionLocalOf { Color.Unspecified }

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = PlexSans, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = tracking.sp
)

private val DevToysTypography = Typography(
    displaySmall = style(34, 40, FontWeight.SemiBold, -0.6),
    headlineSmall = style(24, 30, FontWeight.SemiBold, -0.3),
    titleLarge = style(20, 26, FontWeight.Medium, -0.2),
    titleMedium = style(16, 22, FontWeight.Medium),
    titleSmall = style(14, 20, FontWeight.Medium),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 20, FontWeight.Normal),
    bodySmall = style(12, 16, FontWeight.Normal),
    labelLarge = style(14, 20, FontWeight.Medium),
    labelMedium = style(12, 16, FontWeight.Medium),
    labelSmall = style(11, 16, FontWeight.Medium),
)

private val DevToysShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val Steel = object {
    val darkBackground = Color(0xFF15171A)
    val darkSurface = Color(0xFF1C1F23)
    val darkSurfaceHigh = Color(0xFF24282D)
    val darkOutline = Color(0xFF3A3F45)
    val darkInk = Color(0xFFE6E8EB)
    val darkMuted = Color(0xFF9AA1A9)
    val lightBackground = Color(0xFFF4F5F7)
    val lightSurface = Color(0xFFFFFFFF)
    val lightSurfaceHigh = Color(0xFFE9ECEF)
    val lightOutline = Color(0xFFC9CED4)
    val lightInk = Color(0xFF1A1D21)
    val lightMuted = Color(0xFF5F6670)
}

private val baseDark = darkColorScheme(
    background = Steel.darkBackground, onBackground = Steel.darkInk,
    surface = Steel.darkBackground, onSurface = Steel.darkInk,
    surfaceVariant = Steel.darkSurfaceHigh, onSurfaceVariant = Steel.darkMuted,
    surfaceContainerLowest = Color(0xFF0F1113), surfaceContainerLow = Steel.darkSurface,
    surfaceContainer = Steel.darkSurface, surfaceContainerHigh = Steel.darkSurfaceHigh,
    surfaceContainerHighest = Color(0xFF2C3136),
    outline = Steel.darkOutline, outlineVariant = Color(0xFF2C3136),
)

private val baseLight = lightColorScheme(
    background = Steel.lightBackground, onBackground = Steel.lightInk,
    surface = Steel.lightBackground, onSurface = Steel.lightInk,
    surfaceVariant = Steel.lightSurfaceHigh, onSurfaceVariant = Steel.lightMuted,
    surfaceContainerLowest = Steel.lightSurface, surfaceContainerLow = Steel.lightSurface,
    surfaceContainer = Steel.lightSurface, surfaceContainerHigh = Steel.lightSurfaceHigh,
    surfaceContainerHighest = Color(0xFFDDE1E5),
    outline = Steel.lightOutline, outlineVariant = Color(0xFFDDE1E5),
)

/** Accent presets in Settings order; index 0 is dynamic color (system default). */
val AccentPresets: List<Pair<String, Color>> = listOf(
    "Dynamic" to Color.Unspecified,
    "Blue" to Color(0xFF2F7DE1),
    "Violet" to Color(0xFF7C5CD6),
    "Green" to Color(0xFF2E9E5B),
    "Orange" to Color(0xFFE08A1E),
    "Red" to Color(0xFFDB4437),
    "Yellow" to Color(0xFFD7B21C),
    "Teal" to Color(0xFF13968A),
    "Pink" to Color(0xFFD9467F),
    "Cyan" to Color(0xFF119FB8),
    "Indigo" to Color(0xFF4A5BC4),
)

private fun ColorScheme.withAccent(accent: Color, dark: Boolean): ColorScheme {
    val onAccent = if (dark) Color(0xFF0F1113) else Color.White
    val container = accent.copy(alpha = if (dark) 0.24f else 0.16f).compositeOver(background)
    val onContainer = if (dark) Color.White else Color(0xFF101215)
    return copy(
        primary = accent, onPrimary = onAccent,
        primaryContainer = container, onPrimaryContainer = onContainer,
        secondary = accent, onSecondary = onAccent,
        secondaryContainer = container, onSecondaryContainer = onContainer,
        tertiary = accent, onTertiary = onAccent,
        tertiaryContainer = container, onTertiaryContainer = onContainer,
    )
}

@Composable
fun DevToysTheme(
    darkTheme: Boolean,
    accent: Int,
    amoled: Boolean,
    highContrast: Boolean,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var scheme = when {
        accent == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamic = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            val base = if (darkTheme) baseDark else baseLight
            // Keep the steel neutrals; take only the accent roles from the wallpaper.
            base.copy(
                primary = dynamic.primary, onPrimary = dynamic.onPrimary,
                primaryContainer = dynamic.primaryContainer, onPrimaryContainer = dynamic.onPrimaryContainer,
                secondary = dynamic.secondary, onSecondary = dynamic.onSecondary,
                secondaryContainer = dynamic.secondaryContainer, onSecondaryContainer = dynamic.onSecondaryContainer,
                tertiary = dynamic.tertiary, onTertiary = dynamic.onTertiary,
                tertiaryContainer = dynamic.tertiaryContainer, onTertiaryContainer = dynamic.onTertiaryContainer,
            )
        }
        accent in 1 until AccentPresets.size ->
            (if (darkTheme) baseDark else baseLight).withAccent(AccentPresets[accent].second, darkTheme)
        else -> if (darkTheme) baseDark else baseLight
    }
    if (darkTheme && amoled) {
        scheme = scheme.copy(
            background = Color.Black, surface = Color.Black,
            surfaceContainerLow = Color(0xFF0B0C0E), surfaceContainer = Color(0xFF0B0C0E),
            surfaceContainerLowest = Color(0xFF070809),
        )
    }
    if (highContrast) {
        val ink = if (darkTheme) Color.White else Color.Black
        scheme = scheme.copy(
            onBackground = ink, onSurface = ink, onSurfaceVariant = ink,
            outline = ink, outlineVariant = if (darkTheme) Color.LightGray else Color.DarkGray,
        )
    }
    val slab = if (darkTheme) scheme.surfaceContainerLowest else scheme.surfaceContainerHigh

    CompositionLocalProvider(LocalSlabColor provides slab) {
        MaterialTheme(colorScheme = scheme, typography = DevToysTypography, shapes = DevToysShapes, content = content)
    }
}
