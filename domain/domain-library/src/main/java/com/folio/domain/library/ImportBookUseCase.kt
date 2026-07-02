package com.folio.domain.library

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.folio.core.database.BookEntity
import com.folio.data.library.BookRepository
import com.folio.pdfengine.PdfPageRenderer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportBookUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookRepository: BookRepository,
    private val pdfRenderer: PdfPageRenderer
) {
    suspend operator fun invoke(uri: Uri): Long? = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileName(uri) ?: "Untitled.pdf"
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val file = File(context.filesDir, "imports/${System.currentTimeMillis()}_$fileName")
            file.parentFile?.mkdirs()
            inputStream.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            val doc = pdfRenderer.openDocument(file.absolutePath) ?: run {
                file.delete(); return@withContext null
            }
            val bookId = bookRepository.insertBook(
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
            bookId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun generateThumbnail(filePath: String, bookId: Long) {
        try {
            val doc = pdfRenderer.openDocument(filePath) ?: return
            val bitmap = pdfRenderer.renderPageToBitmap(doc, 0, 240, 360) ?: return
            val thumbFile = File(context.cacheDir, "thumbnails/${bookId}_thumb.png")
            thumbFile.parentFile?.mkdirs()
            FileOutputStream(thumbFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, out)
            }
            bitmap.recycle()
            doc.close()
            bookRepository.getBookById(bookId)?.let {
                bookRepository.updateBook(it.copy(coverThumbnailPath = thumbFile.absolutePath))
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun getFileName(uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            else null
        }
    }
}
