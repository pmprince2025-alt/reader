package com.folio.feature.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.view.WindowManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.folio.core.database.*
import com.folio.core.datastore.SettingsDataStore
import com.folio.pdfengine.PdfDocumentSource
import com.folio.pdfengine.PdfPageRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ReaderUiState(
    val title: String = "",
    val currentPage: Int = 0,
    val pageCount: Int = 0,
    val zoomLevel: Float = 1f,
    val scrollX: Float = 0f,
    val scrollY: Float = 0f,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isChromeVisible: Boolean = false,
    val readingMode: ReadingMode = ReadingMode.STANDARD,
    val turnMode: TurnMode = TurnMode.CURL,
    val tocEntries: List<TocEntry> = emptyList(),
    val brightness: Float = 1f,
    val bookmarks: List<BookmarkItem> = emptyList(),
    val bookmarkPageIndices: Set<Int> = emptySet(),
    val isCurrentPageBookmarked: Boolean = false
)

data class TocEntry(val title: String, val pageIndex: Int)
data class BookmarkItem(val id: Long, val pageIndex: Int, val label: String?, val createdAt: Long)
enum class ReadingMode { STANDARD, SEPIA, NIGHT }
enum class TurnMode { CURL, SLIDE, SCROLL }

@HiltViewModel
class ReaderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao,
    private val readingProgressDao: ReadingProgressDao,
    private val bookmarkDao: BookmarkDao,
    private val readingSessionDao: ReadingSessionDao,
    private val pdfRenderer: PdfPageRenderer,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var document: PdfDocumentSource? = null
    private var bookId: Long = 0
    private val currentPageBitmaps = mutableMapOf<Int, Bitmap>()
    private val bitmapMutex = Mutex()
    private var screenWidth = 1080
    private var screenHeight = 1440

    init {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val display = wm?.defaultDisplay
        val size = Point()
        display?.getRealSize(size)
        screenWidth = size.x.coerceAtLeast(1080)
        screenHeight = size.y.coerceAtLeast(1440)

        viewModelScope.launch {
            try {
                settingsDataStore.turnMode.collect { mode ->
                    val mapped = when (mode) {
                        SettingsDataStore.TurnMode.CURL -> TurnMode.CURL
                        SettingsDataStore.TurnMode.SLIDE -> TurnMode.SLIDE
                        SettingsDataStore.TurnMode.SCROLL -> TurnMode.SCROLL
                    }
                    _uiState.update { it.copy(turnMode = mapped) }
                }
            } catch (_: Exception) {}
        }
        viewModelScope.launch {
            try {
                settingsDataStore.readingMode.collect { mode ->
                    val mapped = when (mode) {
                        SettingsDataStore.ReadingMode.STANDARD -> ReadingMode.STANDARD
                        SettingsDataStore.ReadingMode.SEPIA -> ReadingMode.SEPIA
                        SettingsDataStore.ReadingMode.NIGHT -> ReadingMode.NIGHT
                    }
                    _uiState.update { it.copy(readingMode = mapped) }
                }
            } catch (_: Exception) {}
        }
    }

    val hapticsEnabled = settingsDataStore.hapticsEnabled
        .catch { emit(true) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val hapticsIntensity = settingsDataStore.hapticsIntensity
        .catch { emit(1.0f) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

    fun loadBook(id: Long) {
        bookId = id
        viewModelScope.launch {
            try {
                document?.close()
                bitmapMutex.withLock {
                    currentPageBitmaps.values.forEach { it.recycle() }
                    currentPageBitmaps.clear()
                }
                _uiState.update { ReaderUiState(readingMode = _uiState.value.readingMode, turnMode = _uiState.value.turnMode) }

                val book = bookDao.getBookById(id) ?: run {
                    _uiState.update { it.copy(errorMessage = "Book not found", isLoading = false) }
                    return@launch
                }

                val doc = pdfRenderer.openDocument(book.filePath)
                if (doc == null) {
                    val errMsg = when {
                        book.isPasswordProtected -> "This PDF is password-protected. Please unlock it with a different app and re-import."
                        book.isCorrupted -> "This PDF appears to be corrupted and cannot be opened."
                        else -> "Could not open PDF. The file may be corrupted or password-protected."
                    }
                    _uiState.update { it.copy(errorMessage = errMsg, isLoading = false) }
                    return@launch
                }
                document = doc

                val progress = readingProgressDao.getProgressForBookOnce(id)
                val startPage = progress?.currentPage ?: 0
                val savedZoom = progress?.zoomLevel ?: 1f
                val savedScrollX = progress?.scrollX ?: 0f
                val savedScrollY = progress?.scrollY ?: 0f

                val tocEntries = doc.getTableOfContents().map { toc ->
                    TocEntry(title = toc.title, pageIndex = toc.pageIndex)
                }

                _uiState.update {
                    it.copy(
                        title = book.title,
                        pageCount = doc.pageCount,
                        currentPage = startPage,
                        zoomLevel = savedZoom,
                        scrollX = savedScrollX,
                        scrollY = savedScrollY,
                        isLoading = false,
                        tocEntries = tocEntries
                    )
                }

                loadPageBitmaps(startPage)

                if (book.coverThumbnailPath == null) {
                    launch { generateThumbnail(doc, book.id) }
                }

                bookDao.updateBook(book.copy(lastOpened = System.currentTimeMillis()))
            } catch (e: Throwable) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Error loading book", isLoading = false) }
            }
        }
    }

    private suspend fun loadPageBitmaps(centerPage: Int) {
        val doc = document ?: return
        val start = (centerPage - 2).coerceAtLeast(0)
        val end = (centerPage + 2).coerceAtMost(doc.pageCount - 1)

        withContext(Dispatchers.IO) {
            for (i in start..end) {
                bitmapMutex.withLock {
                    if (i !in currentPageBitmaps) {
                        val bitmap = pdfRenderer.renderPageToBitmap(doc, i, screenWidth, screenHeight)
                        if (bitmap != null) {
                            currentPageBitmaps[i] = bitmap
                        }
                    }
                }
            }
            bitmapMutex.withLock {
                currentPageBitmaps.keys.filter { it < start || it > end }.forEach {
                    currentPageBitmaps[it]?.recycle()
                    currentPageBitmaps.remove(it)
                }
            }
        }
    }

    fun navigateToPage(page: Int) {
        val doc = document ?: return
        val clamped = page.coerceIn(0, doc.pageCount - 1)
        _uiState.update { it.copy(currentPage = clamped) }
        viewModelScope.launch { loadPageBitmaps(clamped) }
        saveProgress()
        refreshBookmarkState()
    }

    fun setZoom(zoom: Float) {
        _uiState.update { it.copy(zoomLevel = zoom.coerceIn(1f, 5f)) }
    }

    fun setScroll(x: Float, y: Float) {
        _uiState.update { it.copy(scrollX = x, scrollY = y) }
    }

    fun toggleChrome() {
        _uiState.update { it.copy(isChromeVisible = !it.isChromeVisible) }
    }

    fun chromeAutoHide() {
        _uiState.update { it.copy(isChromeVisible = false) }
    }

    fun setBrightness(level: Float) {
        _uiState.update { it.copy(brightness = level.coerceIn(0f, 1f)) }
    }

    fun setReadingMode(mode: ReadingMode) {
        _uiState.update { it.copy(readingMode = mode) }
    }

    fun setTurnMode(mode: TurnMode) {
        _uiState.update { it.copy(turnMode = mode) }
    }

    fun getPageBitmap(page: Int): Bitmap? = currentPageBitmaps[page]

    fun onPageTurnComplete(newPage: Int) {
        navigateToPage(newPage)
    }

    fun saveProgress() {
        val state = _uiState.value
        viewModelScope.launch {
            readingProgressDao.upsertProgress(
                ReadingProgressEntity(
                    bookId = bookId,
                    currentPage = state.currentPage,
                    scrollX = state.scrollX,
                    scrollY = state.scrollY,
                    zoomLevel = state.zoomLevel
                )
            )
        }
    }

    fun refreshBookmarkState() {
        viewModelScope.launch {
            val page = _uiState.value.currentPage
            val isBookmarked = bookmarkDao.isPageBookmarked(bookId, page)
            val bookmarks = bookmarkDao.getBookmarksForBookOnce(bookId)
            _uiState.update {
                it.copy(
                    isCurrentPageBookmarked = isBookmarked,
                    bookmarks = bookmarks.map { bm ->
                        BookmarkItem(id = bm.id, pageIndex = bm.pageIndex, label = bm.label, createdAt = bm.createdAt)
                    },
                    bookmarkPageIndices = bookmarks.map { it.pageIndex }.toSet()
                )
            }
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val page = _uiState.value.currentPage
            if (bookmarkDao.isPageBookmarked(bookId, page)) {
                bookmarkDao.deleteBookmarkByPage(bookId, page)
            } else {
                bookmarkDao.insertBookmark(BookmarkEntity(bookId = bookId, pageIndex = page))
            }
            refreshBookmarkState()
        }
    }

    fun deleteBookmark(bookmarkId: Long) {
        viewModelScope.launch {
            val bm = bookmarkDao.getBookmarkById(bookmarkId) ?: return@launch
            bookmarkDao.deleteBookmark(bm)
            refreshBookmarkState()
        }
    }

    private suspend fun generateThumbnail(doc: PdfDocumentSource, bookId: Long) {
        try {
            val bitmap = pdfRenderer.renderPageToBitmap(doc, 0, 240, 360) ?: return
            val thumbFile = java.io.File(
                context.cacheDir, "thumbnails/${bookId}_thumb.png"
            )
            thumbFile.parentFile?.mkdirs()
            java.io.FileOutputStream(thumbFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, out)
            }
            bitmap.recycle()
            bookDao.updateBook(
                bookDao.getBookById(bookId)?.copy(
                    coverThumbnailPath = thumbFile.absolutePath
                ) ?: return
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        document?.close()
        currentPageBitmaps.values.forEach { it.recycle() }
        currentPageBitmaps.clear()
    }
}
