package com.folio.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = LeatherBrown,
    onPrimary = Color.White,
    primaryContainer = LeatherBrownLight,
    onPrimaryContainer = Color.White,
    secondary = InkBlue,
    onSecondary = Color.White,
    secondaryContainer = InkBlueLight,
    onSecondaryContainer = Color.White,
    tertiary = GoldAccent,
    onTertiary = Color.White,
    tertiaryContainer = GoldAccentLight,
    surface = WarmPaper,
    onSurface = NearBlack,
    surfaceVariant = WarmPaperLight,
    onSurfaceVariant = WarmGray,
    background = WarmPaperLight,
    onBackground = NearBlack,
    surfaceTint = LeatherBrown,
    outline = WarmGray.copy(alpha = 0.5f),
    outlineVariant = WarmGray.copy(alpha = 0.2f),
    error = ErrorRed,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = LeatherBrownLight,
    onPrimary = Color.White,
    primaryContainer = LeatherBrownDark,
    onPrimaryContainer = Color(0xFFE8D9C4),
    secondary = InkBlueLight,
    onSecondary = Color.White,
    secondaryContainer = InkBlueDark,
    onSecondaryContainer = Color(0xFFB0C4DE),
    tertiary = GoldAccentLight,
    onTertiary = NearBlack,
    tertiaryContainer = GoldAccent,
    surface = DarkSurface,
    onSurface = Color(0xFFEBE4DA),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = WarmGrayLight,
    background = DarkBackground,
    onBackground = Color(0xFFEBE4DA),
    surfaceTint = LeatherBrownLight,
    outline = WarmGrayLight.copy(alpha = 0.3f),
    outlineVariant = WarmGrayLight.copy(alpha = 0.15f),
    error = Color(0xFFEF5350),
    onError = Color.White
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
