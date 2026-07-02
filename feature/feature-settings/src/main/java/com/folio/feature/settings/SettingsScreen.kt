package com.folio.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.folio.core.datastore.SettingsDataStore
import com.folio.core.ui.theme.screenMargin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(screenMargin())
        ) {
            // Reading section
            SettingsSectionHeader("Reading")
            TurnModeSelector(
                current = state.turnMode,
                onSelect = { viewModel.setTurnMode(it) }
            )
            ReadingModeSelector(
                current = state.readingMode,
                onSelect = { viewModel.setReadingMode(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Appearance section
            SettingsSectionHeader("Appearance")
            ThemeSelector(
                current = state.appTheme,
                onSelect = { viewModel.setAppTheme(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Haptics section
            SettingsSectionHeader("Haptics")
            HapticsToggle(
                enabled = state.hapticsEnabled,
                onToggle = { viewModel.setHapticsEnabled(it) }
            )
            if (state.hapticsEnabled) {
                HapticsIntensitySlider(
                    intensity = state.hapticsIntensity,
                    onChange = { viewModel.setHapticsIntensity(it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Storage section
            SettingsSectionHeader("Storage")
            StorageBudgetSelector(
                budgetMb = state.storageBudgetMb,
                onChange = { viewModel.setStorageBudgetMb(it) }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // App info
            Text(
                text = "Folio v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun TurnModeSelector(
    current: SettingsDataStore.TurnMode,
    onSelect: (SettingsDataStore.TurnMode) -> Unit
) {
    Text(
        text = "Default Page Turn",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    SettingsDataStore.TurnMode.entries.forEach { mode ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = current == mode,
                onClick = { onSelect(mode) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = mode.name.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ReadingModeSelector(
    current: SettingsDataStore.ReadingMode,
    onSelect: (SettingsDataStore.ReadingMode) -> Unit
) {
    Text(
        text = "Reading Mode",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
    )
    SettingsDataStore.ReadingMode.entries.forEach { mode ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = current == mode,
                onClick = { onSelect(mode) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = mode.name.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ThemeSelector(
    current: SettingsDataStore.AppTheme,
    onSelect: (SettingsDataStore.AppTheme) -> Unit
) {
    Text(
        text = "App Theme",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 4.dp)
    )
    SettingsDataStore.AppTheme.entries.forEach { theme ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = current == theme,
                onClick = { onSelect(theme) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = theme.name.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun HapticsToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Haptic Feedback",
            style = MaterialTheme.typography.bodyLarge
        )
        Switch(checked = enabled, onCheckedChange = onToggle)
    }
}

@Composable
private fun HapticsIntensitySlider(
    intensity: Float,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Text(
            text = "Intensity",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = intensity,
            onValueChange = onChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StorageBudgetSelector(
    budgetMb: Int,
    onChange: (Int) -> Unit
) {
    Text(
        text = "Page Cache Budget: ${budgetMb}MB",
        style = MaterialTheme.typography.bodyLarge
    )
    Slider(
        value = budgetMb.toFloat(),
        onValueChange = { onChange(it.toInt()) },
        valueRange = 100f..2000f,
        steps = 18,
        modifier = Modifier.fillMaxWidth()
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("100MB", style = MaterialTheme.typography.bodySmall)
        Text("2000MB", style = MaterialTheme.typography.bodySmall)
    }
}
