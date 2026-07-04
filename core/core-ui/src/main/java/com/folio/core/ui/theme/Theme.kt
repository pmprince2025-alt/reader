package com.folio.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PlasmaViolet,
    onPrimary = Color.White,
    secondary = PlasmaCyan,
    onSecondary = Obsidian,
    tertiary = Warning,
    onTertiary = Color.White,
    surface = Color.White,
    onSurface = Obsidian,
    surfaceVariant = StandardBackground,
    onSurfaceVariant = Color(0xFF64748B),
    background = Color(0xFFF8FAFC),
    onBackground = Obsidian,
    surfaceTint = PlasmaViolet,
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Error,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = PlasmaViolet,
    onPrimary = Color.White,
    primaryContainer = PlasmaViolet.copy(alpha = 0.15f),
    secondary = PlasmaCyan,
    onSecondary = Obsidian,
    secondaryContainer = PlasmaCyan.copy(alpha = 0.15f),
    tertiary = Warning,
    onTertiary = Color.White,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    background = Obsidian,
    onBackground = TextPrimary,
    surfaceTint = PlasmaCyan,
    outline = Border,
    outlineVariant = Border.copy(alpha = 0.5f),
    error = Error,
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
