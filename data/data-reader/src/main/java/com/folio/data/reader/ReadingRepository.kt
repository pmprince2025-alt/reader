package com.folio.data.reader

import com.folio.core.database.ReadingProgressDao
import com.folio.core.database.ReadingProgressEntity
import com.folio.pdfengine.PdfDocumentSource
import com.folio.pdfengine.PdfPageRenderer
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingRepository @Inject constructor(
    private val readingProgressDao: ReadingProgressDao,
    private val pdfRenderer: PdfPageRenderer
) {
    fun getProgress(bookId: Long): Flow<ReadingProgressEntity?> =
        readingProgressDao.getProgressForBook(bookId)

    suspend fun getProgressOnce(bookId: Long): ReadingProgressEntity? =
        readingProgressDao.getProgressForBookOnce(bookId)

    suspend fun saveProgress(progress: ReadingProgressEntity) =
        readingProgressDao.upsertProgress(progress)

    suspend fun openDocument(filePath: String): PdfDocumentSource? =
        pdfRenderer.openDocument(filePath)

    suspend fun renderPage(document: PdfDocumentSource, index: Int, width: Int, height: Int) =
        pdfRenderer.renderPageToBitmap(document, index, width, height)

    fun closeDocument(document: PdfDocumentSource) =
        pdfRenderer.closeDocument(document)
}
