package com.folio.feature.bookshelf

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.folio.core.database.BookEntity
import com.folio.core.ui.components.FolioBookCard
import com.folio.core.ui.theme.screenMargin
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookId: Long,
    onBackClick: () -> Unit,
    onStartReading: (Long) -> Unit,
    viewModel: BookshelfViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val book = remember(bookId, state.books) { state.books.find { it.id == bookId } }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showShelfSheet by remember { mutableStateOf(false) }
    var showCreateShelfDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    book?.let { b ->
                        IconButton(onClick = { viewModel.toggleFavorite(b) }) {
                            Icon(
                                if (b.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Toggle Favorite",
                                tint = if (b.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { shareBook(context, b) }) {
                            Icon(Icons.Filled.Share, contentDescription = "Share")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (book == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Book not found", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(screenMargin()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            FolioBookCard(
                imageUrl = book.coverThumbnailPath,
                modifier = Modifier
                    .width(200.dp)
                    .height(300.dp),
                contentDescription = book.title
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = book.title,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
            Text(
                text = buildString {
                    append("${book.pageCount} pages")
                    if (book.fileSize > 0) {
                        append(" · ${book.fileSize / 1024}KB")
                    }
                    val lastOpened = book.lastOpened
                    if (lastOpened != null) {
                        append(" · Last opened ${dateFormat.format(Date(lastOpened))}")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onStartReading(book.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (book.lastOpened != null) "Continue Reading" else "Start Reading",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { showShelfSheet = true }) {
                    Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Shelf", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }

    if (showShelfSheet) {
        ModalBottomSheet(
            onDismissRequest = { showShelfSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Add to Shelf",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (state.shelves.isEmpty()) {
                    Text(
                        "No shelves yet. Create one below.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    state.shelves.forEach { shelf ->
                        val isInShelf = false // For simplicity we always show add option
                        Surface(
                            onClick = {
                                viewModel.addBookToShelf(book.id, shelf.id)
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text(shelf.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Icon(Icons.Filled.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                TextButton(
                    onClick = { showCreateShelfDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Create New Shelf")
                }
            }
        }
    }

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

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Book") },
            text = { Text("Are you sure you want to delete \"${book.title}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteBook(book)
                        onBackClick()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

private fun shareBook(context: Context, book: BookEntity) {
    val uri = book.uri ?: return
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(uri))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share ${book.title}"))
}
