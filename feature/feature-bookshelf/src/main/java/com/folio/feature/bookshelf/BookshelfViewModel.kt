package com.folio.feature.bookshelf

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.folio.core.database.BookDao
import com.folio.core.database.ReadingProgressDao
import com.folio.core.database.ShelfDao
import com.folio.core.database.BookEntity
import com.folio.core.database.BookShelfCrossRef
import com.folio.core.database.ShelfEntity
import com.folio.pdfengine.PdfPageRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

data class BookshelfUiState(
    val books: List<BookEntity> = emptyList(),
    val shelves: List<ShelfEntity> = emptyList(),
    val recentlyOpened: List<BookEntity> = emptyList(),
    val recentProgress: Map<Long, Int> = emptyMap(),
    val sortOption: SortOption = SortOption.LAST_OPENED,
    val isGridView: Boolean = true,
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isImporting: Boolean = false,
    val importError: String? = null
)

data class LocalBookshelfState(
    val isGridView: Boolean = true,
    val searchQuery: String = "",
    val isImporting: Boolean = false,
    val recentProgress: Map<Long, Int> = emptyMap()
)

enum class SortOption { TITLE, DATE_ADDED, LAST_OPENED, FILE_SIZE }

@HiltViewModel
class BookshelfViewModel @Inject constructor(
    private val bookDao: BookDao,
    private val shelfDao: ShelfDao,
    private val readingProgressDao: ReadingProgressDao,
    private val pdfRenderer: PdfPageRenderer,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _sortOption = MutableStateFlow(SortOption.LAST_OPENED)
    private val _isGridView = MutableStateFlow(true)
    private val _searchQuery = MutableStateFlow("")
    private val _isImporting = MutableStateFlow(false)
    private val _importError = MutableStateFlow<String?>(null)
    private val _recentProgress = MutableStateFlow<Map<Long, Int>>(emptyMap())

    init {
        viewModelScope.launch {
            bookDao.getRecentlyOpened().collect { books ->
                val progressMap = mutableMapOf<Long, Int>()
                for (book in books) {
                    val progress = readingProgressDao.getProgressForBookOnce(book.id)
                    if (progress != null) {
                        progressMap[book.id] = progress.currentPage
                    }
                }
                _recentProgress.value = progressMap
            }
        }
    }

    private val _localState = combine(
        _isGridView, _searchQuery, _isImporting, _recentProgress
    ) { isGrid, query, importing, progress ->
        LocalBookshelfState(isGrid, query, importing, progress)
    }

    val uiState: StateFlow<BookshelfUiState> = combine(
        _sortOption.flatMapLatest { option ->
            when (option) {
                SortOption.TITLE -> bookDao.getBooksByTitle()
                SortOption.DATE_ADDED -> bookDao.getBooksByDateAdded()
                SortOption.LAST_OPENED -> bookDao.getAllBooks()
                SortOption.FILE_SIZE -> bookDao.getBooksByFileSize()
            }
        },
        shelfDao.getAllShelves(),
        bookDao.getRecentlyOpened(),
        _localState
    ) { books, shelves, recent, local ->
        val filtered = if (local.searchQuery.isBlank()) books
        else books.filter { it.title.contains(local.searchQuery, ignoreCase = true) }
        BookshelfUiState(
            books = filtered,
            shelves = shelves,
            recentlyOpened = recent,
            recentProgress = local.recentProgress,
            sortOption = _sortOption.value,
            isGridView = local.isGridView,
            searchQuery = local.searchQuery,
            isLoading = false,
            isImporting = local.isImporting,
            importError = _importError.value
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookshelfUiState())

    fun toggleView() {
        _isGridView.value = !_isGridView.value
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun renameBook(bookId: Long, newTitle: String) {
        viewModelScope.launch {
            val book = bookDao.getBookById(bookId) ?: return@launch
            bookDao.updateBook(book.copy(title = newTitle))
        }
    }

    fun createShelf(name: String) {
        viewModelScope.launch {
            shelfDao.insertShelf(ShelfEntity(name = name))
        }
    }

    fun renameShelf(shelfId: Long, newName: String) {
        viewModelScope.launch {
            val shelf = shelfDao.getShelfById(shelfId) ?: return@launch
            shelfDao.updateShelf(shelf.copy(name = newName))
        }
    }

    fun deleteShelf(shelfId: Long) {
        viewModelScope.launch {
            val shelf = shelfDao.getShelfById(shelfId) ?: return@launch
            shelfDao.deleteShelf(shelf)
        }
    }

    fun addBookToShelf(bookId: Long, shelfId: Long) {
        viewModelScope.launch {
            shelfDao.addBookToShelf(
                BookShelfCrossRef(bookId = bookId, shelfId = shelfId)
            )
        }
    }

    fun removeBookFromShelf(bookId: Long, shelfId: Long) {
        viewModelScope.launch {
            shelfDao.removeBookFromShelf(
                BookShelfCrossRef(bookId = bookId, shelfId = shelfId)
            )
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(book.filePath)
                if (file.exists()) file.delete()
                if (book.coverThumbnailPath != null) {
                    val thumb = File(book.coverThumbnailPath)
                    if (thumb.exists()) thumb.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            bookDao.deleteBook(book)
        }
    }

    fun importUri(uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            _importError.value = null
            try {
                val fileName = getFileName(uri) ?: "Untitled.pdf"
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: run {
                        _importError.value = "Could not open the selected file"
                        return@launch
                    }
                val file = File(context.filesDir, "imports/${System.currentTimeMillis()}_$fileName")
                file.parentFile?.mkdirs()
                withContext(Dispatchers.IO) {
                    inputStream.use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                val doc = pdfRenderer.openDocument(file.absolutePath)
                if (doc == null) {
                    file.delete()
                    _importError.value = "Could not open PDF. The file may be corrupted or password-protected."
                    return@launch
                }
                val bookId = bookDao.insertBook(
                    BookEntity(
                        title = fileName.removeSuffix(".pdf").removeSuffix(".PDF"),
                        filePath = file.absolutePath,
                        uri = uri.toString(),
                        pageCount = doc.pageCount,
                        fileSize = file.length(),
                        dateAdded = System.currentTimeMillis()
                    )
                )
                doc.close()
                generateThumbnail(file.absolutePath, bookId)
            } catch (e: Exception) {
                e.printStackTrace()
                _importError.value = "Import failed: ${e.message ?: "Unknown error"}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    private suspend fun generateThumbnail(filePath: String, bookId: Long) {
        withContext(Dispatchers.IO) {
            try {
                val doc = pdfRenderer.openDocument(filePath) ?: return@withContext
                val bitmap = pdfRenderer.renderPageToBitmap(doc, 0, 240, 360) ?: return@withContext
                val thumbFile = File(context.cacheDir, "thumbnails/${bookId}_thumb.png")
                thumbFile.parentFile?.mkdirs()
                FileOutputStream(thumbFile).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, out)
                }
                bitmap.recycle()
                doc.close()
                val book = bookDao.getBookById(bookId) ?: return@withContext
                bookDao.updateBook(book.copy(coverThumbnailPath = thumbFile.absolutePath))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) {
                it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            } else null
        }
    }
}
