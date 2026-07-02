package com.folio.feature.reader

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.folio.core.database.BookDao
import com.folio.core.database.ReadingProgressDao
import com.folio.core.database.ReadingProgressEntity
import com.folio.core.datastore.SettingsDataStore
import com.folio.pdfengine.PdfDocumentSource
import com.folio.pdfengine.PdfPageRenderer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val brightness: Float = 1f
)

data class TocEntry(val title: String, val pageIndex: Int)
enum class ReadingMode { STANDARD, SEPIA, NIGHT }
enum class TurnMode { CURL, SLIDE, SCROLL }

@HiltViewModel
class ReaderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao,
    private val readingProgressDao: ReadingProgressDao,
    private val pdfRenderer: PdfPageRenderer,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var document: PdfDocumentSource? = null
    private var bookId: Long = 0
    private val currentPageBitmaps = mutableMapOf<Int, Bitmap>()

    init {
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
                currentPageBitmaps.values.forEach { it.recycle() }
                currentPageBitmaps.clear()
                _uiState.update { ReaderUiState(readingMode = _uiState.value.readingMode, turnMode = _uiState.value.turnMode) }

                val book = bookDao.getBookById(id) ?: run {
                    _uiState.update { it.copy(errorMessage = "Book not found", isLoading = false) }
                    return@launch
                }

                val doc = pdfRenderer.openDocument(book.filePath)
                if (doc == null) {
                    _uiState.update { it.copy(errorMessage = "Could not open PDF. The file may be corrupted or password-protected.", isLoading = false) }
                    return@launch
                }
                document = doc

                val progress = readingProgressDao.getProgressForBookOnce(id)
                val startPage = progress?.currentPage ?: 0

                val tocEntries = doc.getTableOfContents().map { toc ->
                    TocEntry(title = toc.title, pageIndex = toc.pageIndex)
                }

                _uiState.update {
                    it.copy(
                        title = book.title,
                        pageCount = doc.pageCount,
                        currentPage = startPage,
                        isLoading = false,
                        tocEntries = tocEntries
                    )
                }

                loadPageBitmaps(startPage)

                if (book.coverThumbnailPath == null) {
                    launch {
                        generateThumbnail(doc, book.id)
                    }
                }
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
                if (i !in currentPageBitmaps) {
                    val bitmap = pdfRenderer.renderPageToBitmap(doc, i, 1080, 1440)
                    if (bitmap != null) {
                        currentPageBitmaps[i] = bitmap
                    }
                }
            }
            currentPageBitmaps.keys.filter { it < start || it > end }.forEach {
                currentPageBitmaps[it]?.recycle()
                currentPageBitmaps.remove(it)
            }
        }
    }

    fun navigateToPage(page: Int) {
        val doc = document ?: return
        val clamped = page.coerceIn(0, doc.pageCount - 1)
        _uiState.update { it.copy(currentPage = clamped) }
        viewModelScope.launch { loadPageBitmaps(clamped) }
        saveProgress()
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
