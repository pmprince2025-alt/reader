package com.folio.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.folio.core.datastore.SettingsDataStore
import com.folio.core.ui.theme.screenMargin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onCheckForUpdates: () -> Unit = {},
    currentVersion: String = "1.0.0",
    updateStatus: String? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(screenMargin())
        ) {
            // Reading section
            SectionHeader("Reading")
            SettingsCard {
                TurnModeSelector(
                    current = state.turnMode,
                    onSelect = { viewModel.setTurnMode(it) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
                ReadingModeSelector(
                    current = state.readingMode,
                    onSelect = { viewModel.setReadingMode(it) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Appearance section
            SectionHeader("Appearance")
            SettingsCard {
                ThemeSelector(
                    current = state.appTheme,
                    onSelect = { viewModel.setAppTheme(it) }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
                HapticsToggle(
                    enabled = state.hapticsEnabled,
                    onToggle = { viewModel.setHapticsEnabled(it) }
                )
                if (state.hapticsEnabled) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                    HapticsIntensitySlider(
                        intensity = state.hapticsIntensity,
                        onChange = { viewModel.setHapticsIntensity(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Storage section
            SectionHeader("Storage")
            SettingsCard {
                StorageBudgetSelector(
                    budgetMb = state.storageBudgetMb,
                    onChange = { viewModel.setStorageBudgetMb(it) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Feedback section
            SectionHeader("Feedback")
            SettingsCard {
                CheckForUpdatesRow(
                    onClick = onCheckForUpdates,
                    status = updateStatus
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
                SendFeedbackRow()
            }

            Spacer(modifier = Modifier.height(20.dp))

            // About section
            SectionHeader("About")
            SettingsCard {
                AboutRow(version = currentVersion)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun RadioIconContainer(icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
private fun RadioIndicator(selected: Boolean) {
    val color by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        label = "radioColor"
    )
    Canvas(modifier = Modifier.size(22.dp)) {
        if (selected) {
            drawCircle(
                color = color,
                radius = size.minDimension / 2f
            )
            drawCircle(
                color = MaterialTheme.colorScheme.surface,
                radius = size.minDimension / 4f
            )
        } else {
            drawCircle(
                color = color,
                radius = size.minDimension / 2f - 1.5.dp.toPx(),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

@Composable
private fun RadioSettingRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String = "",
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioIconContainer(icon = icon)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        RadioIndicator(selected = selected)
    }
}

@Composable
private fun TurnModeSelector(
    current: SettingsDataStore.TurnMode,
    onSelect: (SettingsDataStore.TurnMode) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsDataStore.TurnMode.entries.forEachIndexed { index, mode ->
            RadioSettingRow(
                icon = {
                    Icon(
                        Icons.Outlined.SwapHoriz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                title = when (mode) {
                    SettingsDataStore.TurnMode.CURL -> "Page Curl"
                    SettingsDataStore.TurnMode.SLIDE -> "Slide"
                    SettingsDataStore.TurnMode.SCROLL -> "Scroll"
                },
                subtitle = when (mode) {
                    SettingsDataStore.TurnMode.CURL -> "Realistic page turn animation"
                    SettingsDataStore.TurnMode.SLIDE -> "Smooth sliding transition"
                    SettingsDataStore.TurnMode.SCROLL -> "Continuous scrolling"
                },
                selected = current == mode,
                onClick = { onSelect(mode) }
            )
            if (index < SettingsDataStore.TurnMode.entries.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun ReadingModeSelector(
    current: SettingsDataStore.ReadingMode,
    onSelect: (SettingsDataStore.ReadingMode) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsDataStore.ReadingMode.entries.forEachIndexed { index, mode ->
            RadioSettingRow(
                icon = {
                    Icon(
                        Icons.Outlined.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                title = when (mode) {
                    SettingsDataStore.ReadingMode.STANDARD -> "Standard"
                    SettingsDataStore.ReadingMode.SEPIA -> "Sepia"
                    SettingsDataStore.ReadingMode.NIGHT -> "Night"
                },
                subtitle = when (mode) {
                    SettingsDataStore.ReadingMode.STANDARD -> "Classic white background"
                    SettingsDataStore.ReadingMode.SEPIA -> "Warm paper-like tone"
                    SettingsDataStore.ReadingMode.NIGHT -> "Dark mode for low light"
                },
                selected = current == mode,
                onClick = { onSelect(mode) }
            )
            if (index < SettingsDataStore.ReadingMode.entries.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun ThemeSelector(
    current: SettingsDataStore.AppTheme,
    onSelect: (SettingsDataStore.AppTheme) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SettingsDataStore.AppTheme.entries.forEachIndexed { index, theme ->
            RadioSettingRow(
                icon = {
                    Icon(
                        Icons.Outlined.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                title = when (theme) {
                    SettingsDataStore.AppTheme.SYSTEM -> "System"
                    SettingsDataStore.AppTheme.LIGHT -> "Light"
                    SettingsDataStore.AppTheme.DARK -> "Dark"
                },
                subtitle = when (theme) {
                    SettingsDataStore.AppTheme.SYSTEM -> "Follow device theme"
                    SettingsDataStore.AppTheme.LIGHT -> "Always light mode"
                    SettingsDataStore.AppTheme.DARK -> "Always dark mode"
                },
                selected = current == theme,
                onClick = { onSelect(theme) }
            )
            if (index < SettingsDataStore.AppTheme.entries.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun CustomSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        label = "trackColor"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 2.dp,
        label = "thumbOffset"
    )

    Box(
        modifier = Modifier
            .width(46.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onCheckedChange(!checked) }
            .background(trackColor),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun HapticsToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!enabled) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioIconContainer(
            icon = {
                Icon(
                    Icons.Outlined.Vibration,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Haptic Feedback",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Vibration on page turns",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        CustomSwitch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun HapticsIntensitySlider(
    intensity: Float,
    onChange: (Float) -> Unit
) {
    val percentage = (intensity * 100).toInt()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioIconContainer(
                icon = {
                    Icon(
                        Icons.Outlined.Vibration,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Intensity",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Adjust vibration strength",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${percentage}%",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        GradientSlider(
            value = intensity,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 54.dp, end = 16.dp)
        )
    }
}

@Composable
private fun GradientSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackHeight = 6.dp
    val thumbRadius = 10.dp

    BoxWithConstraints(
        modifier = modifier
            .height(thumbRadius * 2 + 4.dp)
            .fillMaxWidth()
    ) {
        val trackWidth = constraints.maxWidth.toFloat() - thumbRadius.toPx() * 2
        val thumbOffset: Dp = (trackWidth * value.coerceIn(0f, 1f)).toDp()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .align(Alignment.CenterStart)
                .padding(horizontal = thumbRadius)
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val fillWidth = size.width * value.coerceIn(0f, 1f)

                drawRoundRect(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                    size = Size(size.width, size.height)
                )
                if (fillWidth > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.primary
                            )
                        ),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                        size = Size(fillWidth, size.height)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbRadius * 2)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                )
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        val newValue = (change.position.x / trackWidth).coerceIn(0f, 1f)
                        onValueChange(newValue)
                    }
                }
        )
    }
}

@Composable
private fun StorageBudgetSelector(
    budgetMb: Int,
    onChange: (Int) -> Unit
) {
    val budgetFraction = (budgetMb - 100).toFloat() / (2000 - 100).toFloat()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioIconContainer(
                icon = {
                    Icon(
                        Icons.Outlined.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Cache Limit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Maximum storage for offline pages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${budgetMb}MB",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.secondary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        GradientSlider(
            value = budgetFraction,
            onValueChange = { onChange((100 + (it * 1900)).toInt()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 54.dp, end = 16.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 54.dp, end = 16.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "100MB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = "2000MB",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun CheckForUpdatesRow(
    onClick: () -> Unit,
    status: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 0.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.secondary)
        )
        Spacer(modifier = Modifier.width(14.dp))
        RadioIconContainer(
            icon = {
                Icon(
                    Icons.Outlined.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Check for Updates",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (status != null) {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SendFeedbackRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioIconContainer(
            icon = {
                Icon(
                    Icons.Outlined.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Send Feedback",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Report a bug or suggest a feature",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AboutRow(version: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioIconContainer(
            icon = {
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Folio Reader",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Version $version",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "v$version",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}
