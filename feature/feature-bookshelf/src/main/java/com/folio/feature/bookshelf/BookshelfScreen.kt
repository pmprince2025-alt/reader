package com.folio.feature.bookshelf

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.folio.core.database.BookEntity
import com.folio.core.ui.components.EmptyLibraryIllustration
import com.folio.core.ui.components.FolioBookCard
import com.folio.core.ui.components.FolioTopBar
import com.folio.core.ui.theme.screenMargin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    pendingImportUri: Uri? = null,
    onBookClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: BookshelfViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSearch by remember { mutableStateOf(false) }
    var longPressedBook by remember { mutableStateOf<BookEntity?>(null) }
    var showShelfPicker by remember { mutableStateOf(false) }
    var showCreateShelfDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var pendingBookForShelf by remember { mutableLongStateOf(0L) }
    var pendingRenameBook by remember { mutableStateOf<BookEntity?>(null) }

    LaunchedEffect(pendingImportUri) {
        pendingImportUri?.let { viewModel.importUri(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importUri(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FolioTopBar(
                title = "Folio",
                isGridView = state.isGridView,
                onViewToggle = { viewModel.toggleView() },
                onSearchClick = { showSearch = !showSearch },
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }

        AnimatedVisibility(visible = showSearch) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.updateSearch(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search your library...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearch("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.isImporting -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Importing PDF...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            state.books.isEmpty() && state.searchQuery.isBlank() -> {
                EmptyLibraryState(importLauncher)
            }
            else -> {
                BookshelfContent(
                    state = state,
                    onBookClick = onBookClick,
                    onSortOptionChange = { viewModel.setSortOption(it) },
                    onLongClick = { longPressedBook = it }
                )
            }
        }

        if (state.books.isNotEmpty() || state.searchQuery.isNotBlank()) {
            FloatingActionButton(
                onClick = { importLauncher.launch(arrayOf("application/pdf")) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(screenMargin()),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Import PDF")
            }
        }
    }

    // Long-press context sheet
    longPressedBook?.let { book ->
        ModalBottomSheet(
            onDismissRequest = { longPressedBook = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ListItem(
                    headlineContent = { Text("Move to Shelf") },
                    leadingContent = { Icon(Icons.Filled.Folder, contentDescription = null) },
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            showShelfPicker = true
                            pendingBookForShelf = book.id
                        }
                    )
                )
                ListItem(
                    headlineContent = { Text("Rename") },
                    leadingContent = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            showRenameDialog = true
                            pendingRenameBook = book
                        }
                    )
                )
                ListItem(
                    headlineContent = { Text("Details") },
                    leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                    modifier = Modifier.combinedClickable(
                        onClick = { longPressedBook = null }
                    )
                )
                ListItem(
                    headlineContent = { Text("Delete") },
                    leadingContent = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            viewModel.deleteBook(book)
                            longPressedBook = null
                        }
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Shelf picker dialog
    if (showShelfPicker) {
        AlertDialog(
            onDismissRequest = { showShelfPicker = false },
            title = { Text("Move to Shelf") },
            text = {
                Column {
                    if (state.shelves.isEmpty()) {
                        Text("No shelves yet. Create one first.")
                    } else {
                        state.shelves.forEach { shelf ->
                            TextButton(
                                onClick = {
                                    viewModel.addBookToShelf(pendingBookForShelf, shelf.id)
                                    showShelfPicker = false
                                    longPressedBook = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(shelf.name)
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    TextButton(
                        onClick = {
                            showShelfPicker = false
                            showCreateShelfDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("New Shelf")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShelfPicker = false }) { Text("Close") }
            }
        )
    }

    // Create shelf dialog
    if (showCreateShelfDialog) {
        var shelfName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateShelfDialog = false },
            title = { Text("New Shelf") },
            text = {
                OutlinedTextField(
                    value = shelfName,
                    onValueChange = { shelfName = it },
                    label = { Text("Shelf name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (shelfName.isNotBlank()) {
                            viewModel.createShelf(shelfName)
                            showCreateShelfDialog = false
                        }
                    }
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateShelfDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Rename dialog
    if (showRenameDialog && pendingRenameBook != null) {
        var newName by remember { mutableStateOf(pendingRenameBook?.title ?: "") }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Book title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.renameBook(pendingRenameBook!!.id, newName)
                            showRenameDialog = false
                            longPressedBook = null
                            pendingRenameBook = null
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BookshelfContent(
    state: BookshelfUiState,
    onBookClick: (Long) -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
    onLongClick: (BookEntity) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortOption.entries.forEach { option ->
                    FilterChip(
                        selected = state.sortOption == option,
                        onClick = { onSortOptionChange(option) },
                        label = {
                            Text(
                                when (option) {
                                    SortOption.TITLE -> "Title"
                                    SortOption.DATE_ADDED -> "Date Added"
                                    SortOption.LAST_OPENED -> "Recent"
                                    SortOption.FILE_SIZE -> "File Size"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
            }
        }
        if (state.recentlyOpened.isNotEmpty()) {
            item {
                Text(
                    text = "Continue Reading",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = screenMargin(), vertical = 8.dp)
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = screenMargin()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.recentlyOpened) { book ->
                        Column(modifier = Modifier.width(120.dp)) {
                            Box(modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                            ) {
                                Surface(
                                    onClick = { onBookClick(book.id) },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    FolioBookCard(
                                        imageUrl = book.coverThumbnailPath,
                                        modifier = Modifier.fillMaxSize(),
                                        contentDescription = book.title
                                    )
                                }
                                val currentPage = state.recentProgress[book.id] ?: 0
                                if (currentPage > 0 && book.pageCount > 0) {
                                    CircularProgressIndicator(
                                        progress = currentPage.toFloat() / book.pageCount,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(8.dp)
                                            .size(24.dp),
                                        strokeWidth = 2.dp,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        indicatorColor = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = book.title,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = screenMargin(), vertical = 8.dp),
                    thickness = 2.dp,
                    color = Color(0xFF6B4226).copy(alpha = 0.25f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = screenMargin(), vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "All Books",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${state.books.size} books",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state.isGridView) {
            val chunks = state.books.chunked(3)
            items(chunks.size) { index ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = screenMargin()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    chunks[index].forEach { book ->
                        BookGridItem(
                            book = book,
                            onClick = { onBookClick(book.id) },
                            onLongClick = onLongClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - chunks[index].size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        } else {
            items(state.books) { book ->
                ShelfListItem(
                    book = book,
                    onClick = { onBookClick(book.id) },
                    onLongClick = onLongClick
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookGridItem(
    book: BookEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (BookEntity) -> Unit = {}
) {
    Column(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongClick(book) }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)
        ) {
            FolioBookCard(
                imageUrl = book.coverThumbnailPath,
                modifier = Modifier.fillMaxSize(),
                contentDescription = book.title
            )
        }
        Text(
            text = book.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ShelfListItem(
    book: BookEntity,
    onClick: () -> Unit,
    onLongClick: (BookEntity) -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = screenMargin(), vertical = 4.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongClick(book) }
            ),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FolioBookCard(
                imageUrl = book.coverThumbnailPath,
                modifier = Modifier
                    .width(48.dp)
                    .height(72.dp),
                contentDescription = book.title
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${book.pageCount} pages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyLibraryState(importLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            EmptyLibraryIllustration(
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your library is empty",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Import PDFs to start building your personal bookshelf",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { importLauncher.launch(arrayOf("application/pdf")) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Import PDFs")
            }
        }
    }
}
