package com.folio.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LeatherBrown,
    onPrimary = Color.White,
    primaryContainer = LeatherBrownLight,
    secondary = InkBlue,
    onSecondary = Color.White,
    secondaryContainer = InkBlueLight,
    surface = WarmPaper,
    onSurface = NearBlack,
    onSurfaceVariant = WarmGray,
    background = WarmPaper,
    onBackground = NearBlack,
    surfaceTint = LeatherBrown,
    outline = WarmGray.copy(alpha = 0.5f)
)

private val DarkColorScheme = darkColorScheme(
    primary = LeatherBrownLight,
    onPrimary = Color.White,
    primaryContainer = LeatherBrownDark,
    secondary = InkBlueLight,
    onSecondary = Color.White,
    secondaryContainer = InkBlueDark,
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE8E4DD),
    onSurfaceVariant = Color(0xFF9E948C),
    background = DarkBackground,
    onBackground = Color(0xFFE8E4DD),
    surfaceTint = LeatherBrownLight,
    outline = Color(0xFF9E948C).copy(alpha = 0.3f)
)

@Composable
fun FolioTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit
) {
    val isDark = when (darkTheme) {
        true -> true
        false -> false
        null -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FolioTypography,
        content = content
    )
}
