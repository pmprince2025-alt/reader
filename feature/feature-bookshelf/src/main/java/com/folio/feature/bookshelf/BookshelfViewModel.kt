package com.folio.feature.bookshelf

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.folio.core.database.BookDao
import com.folio.core.database.ReadingProgressDao
import com.folio.core.database.ShelfDao
import com.folio.core.database.BookEntity
import com.folio.core.database.BookShelfCrossRef
import com.folio.core.database.ShelfEntity
import com.folio.app.PendingImport
import com.folio.core.datastore.SettingsDataStore
import com.folio.core.datastore.SortOptionName
import com.folio.domain.library.ImportBookUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
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
    val importError: String? = null,
    val favoritesOnly: Boolean = false,
    val selectedShelfId: Long? = null,
    val showDeleteConfirm: BookEntity? = null
)

data class LocalBookshelfState(
    val isGridView: Boolean = true,
    val searchQuery: String = "",
    val isImporting: Boolean = false,
    val recentProgress: Map<Long, Int> = emptyMap(),
    val favoritesOnly: Boolean = false
)

enum class SortOption { TITLE, DATE_ADDED, LAST_OPENED, FILE_SIZE }

@HiltViewModel
class BookshelfViewModel @Inject constructor(
    private val bookDao: BookDao,
    private val shelfDao: ShelfDao,
    private val readingProgressDao: ReadingProgressDao,
    private val importBookUseCase: ImportBookUseCase,
    private val settingsDataStore: SettingsDataStore,
    private val pendingImport: PendingImport,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _sortOption = MutableStateFlow(SortOption.LAST_OPENED)
    private val _isGridView = MutableStateFlow(true)
    private val _searchQuery = MutableStateFlow("")
    private val _isImporting = MutableStateFlow(false)
    private val _importError = MutableStateFlow<String?>(null)
    private val _recentProgress = MutableStateFlow<Map<Long, Int>>(emptyMap())
    private val _favoritesOnly = MutableStateFlow(false)
    private val _selectedShelfId = MutableStateFlow<Long?>(null)
    private val _showDeleteConfirm = MutableStateFlow<BookEntity?>(null)

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
        viewModelScope.launch {
            settingsDataStore.sortOption.collect { saved ->
                val mapped = when (saved) {
                    SortOptionName.TITLE -> SortOption.TITLE
                    SortOptionName.DATE_ADDED -> SortOption.DATE_ADDED
                    SortOptionName.FILE_SIZE -> SortOption.FILE_SIZE
                    else -> SortOption.LAST_OPENED
                }
                _sortOption.value = mapped
            }
        }
        viewModelScope.launch {
            settingsDataStore.isGridView.collect { saved ->
                _isGridView.value = saved
            }
        }
        val pendingUri = pendingImport.consume()
        if (pendingUri != null) {
            importUri(pendingUri)
        }
    }

    private val _localState = combine(
        _isGridView, _searchQuery, _isImporting, _recentProgress, _favoritesOnly
    ) { isGrid, query, importing, progress, favOnly ->
        LocalBookshelfState(isGrid, query, importing, progress, favOnly)
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
        _localState,
        _selectedShelfId
    ) { books, shelves, recent, local, shelfId ->
        val filtered = if (local.searchQuery.isBlank()) books
        else books.filter { it.title.contains(local.searchQuery, ignoreCase = true) }
        val favFiltered = if (local.favoritesOnly) filtered.filter { it.isFavorite } else filtered
        BookshelfUiState(
            books = favFiltered,
            shelves = shelves,
            recentlyOpened = recent,
            recentProgress = local.recentProgress,
            sortOption = _sortOption.value,
            isGridView = local.isGridView,
            searchQuery = local.searchQuery,
            isLoading = false,
            isImporting = local.isImporting,
            importError = _importError.value,
            favoritesOnly = local.favoritesOnly,
            selectedShelfId = shelfId,
            showDeleteConfirm = _showDeleteConfirm.value
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookshelfUiState())

    fun toggleView() {
        val new = !_isGridView.value
        _isGridView.value = new
        viewModelScope.launch { settingsDataStore.setGridView(new) }
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
        viewModelScope.launch {
            val name = when (option) {
                SortOption.TITLE -> SortOptionName.TITLE
                SortOption.DATE_ADDED -> SortOptionName.DATE_ADDED
                SortOption.LAST_OPENED -> SortOptionName.LAST_OPENED
                SortOption.FILE_SIZE -> SortOptionName.FILE_SIZE
            }
            settingsDataStore.setSortOption(name)
        }
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun toggleFavoritesOnly() {
        _favoritesOnly.value = !_favoritesOnly.value
    }

    fun setSelectedShelf(id: Long?) {
        _selectedShelfId.value = id
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

    fun requestDelete(book: BookEntity) {
        _showDeleteConfirm.value = book
    }

    fun cancelDelete() {
        _showDeleteConfirm.value = null
    }

    fun confirmDelete() {
        val book = _showDeleteConfirm.value ?: return
        _showDeleteConfirm.value = null
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

    fun toggleFavorite(book: BookEntity) {
        viewModelScope.launch {
            bookDao.updateBook(book.copy(isFavorite = !book.isFavorite))
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
        importUris(listOf(uri))
    }

    fun importUris(uris: List<Uri>) {
        viewModelScope.launch {
            _isImporting.value = true
            _importError.value = null
            var successCount = 0
            var failCount = 0
            for (uri in uris) {
                val result = importBookUseCase(uri)
                if (result != null) successCount++ else failCount++
            }
            if (failCount > 0 && successCount == 0) {
                _importError.value = "Import failed for all $failCount file(s)"
            } else if (failCount > 0) {
                _importError.value = "Imported $successCount file(s), $failCount failed"
            }
            _isImporting.value = false
        }
    }
}
