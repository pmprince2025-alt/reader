package com.folio.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun screenMargin(): Dp {
    val config = LocalConfiguration.current
    return if (config.screenWidthDp >= 600) 32.dp else 20.dp
}

@Composable
fun adaptiveCornerRadius(): Dp {
    val config = LocalConfiguration.current
    return if (config.screenWidthDp >= 600) 20.dp else 16.dp
}

@Composable
fun isTablet(): Boolean {
    val config = LocalConfiguration.current
    return config.screenWidthDp >= 600
}
