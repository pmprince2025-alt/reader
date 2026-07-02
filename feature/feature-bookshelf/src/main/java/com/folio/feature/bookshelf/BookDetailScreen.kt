package com.folio.feature.bookshelf

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            // Large cover art
            FolioBookCard(
                imageUrl = book.coverThumbnailPath,
                modifier = Modifier
                    .width(200.dp)
                    .height(300.dp),
                contentDescription = book.title
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title in Display Large serif
            Text(
                text = book.title,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Metadata
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

            // Primary action button
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

            // Secondary actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { /* TODO: Add to shelf */ }) {
                    Text("Add to Shelf", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(
                    onClick = { viewModel.deleteBook(book); onBackClick() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
