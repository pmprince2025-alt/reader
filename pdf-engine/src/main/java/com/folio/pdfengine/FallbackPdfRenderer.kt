package com.folio.pdfengine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FallbackPdfRenderer : PdfPageRenderer {
    override suspend fun openDocument(filePath: String): PdfDocumentSource? = withContext(Dispatchers.IO) {
        try {
            val file = java.io.File(filePath)
            if (!file.exists()) return@withContext null
            val pfd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            PdfiumDocumentSource(pfd, filePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun openDocumentFromBytes(bytes: ByteArray): PdfDocumentSource? = withContext(Dispatchers.IO) {
        try {
            val tempFile = java.io.File.createTempFile("folio_pdf_", ".pdf")
            tempFile.writeBytes(bytes)
            val pfd = android.os.ParcelFileDescriptor.open(tempFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            PdfiumDocumentSource(pfd, tempFile.absolutePath, deleteOnClose = true)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun renderPageToBitmap(
        document: PdfDocumentSource,
        pageIndex: Int,
        width: Int,
        height: Int
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val bytes = document.renderPage(pageIndex, width, height)
            if (bytes != null) {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun closeDocument(document: PdfDocumentSource) {
        document.close()
    }
}
