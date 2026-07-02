package com.folio.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.outlined.ViewModule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolioTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onSearchClick: (() -> Unit)? = null,
    isGridView: Boolean = true,
    onViewToggle: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },
        modifier = modifier,
        actions = {
            onSearchClick?.let {
                IconButton(onClick = it) {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }
            }
            onViewToggle?.let {
                IconButton(onClick = it) {
                    Icon(
                        if (isGridView) Icons.Outlined.ViewModule else Icons.Outlined.ViewAgenda,
                        contentDescription = if (isGridView) "Grid view" else "Shelf view"
                    )
                }
            }
            actions()
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
