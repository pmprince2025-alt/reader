package com.folio.pdfengine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.ParcelFileDescriptor
import com.shockwave.pdfium.PdfiumCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PdfiumEngine(private val context: Context? = null) : PdfPageRenderer {

    private val pdfiumCore: PdfiumCore? = context?.let { PdfiumCore(it) }

    override suspend fun openDocument(filePath: String): PdfDocumentSource? = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext null
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            PdfiumNativeDocumentSource(pfd, filePath, pdfiumCore)
        } catch (e: SecurityException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun openDocumentFromBytes(bytes: ByteArray): PdfDocumentSource? = withContext(Dispatchers.IO) {
        try {
            val tempFile = File.createTempFile("folio_pdf_", ".pdf")
            tempFile.writeBytes(bytes)
            val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            PdfiumNativeDocumentSource(pfd, tempFile.absolutePath, pdfiumCore, deleteOnClose = true)
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
            if (document !is PdfiumNativeDocumentSource) return@withContext null
            val safeWidth = width.coerceIn(100, 4096)
            val safeHeight = height.coerceIn(100, 4096)
            val pixelCount = safeWidth.toLong() * safeHeight.toLong()
            if (pixelCount > 8_000_000L) {
                val sampleSize = kotlin.math.ceil(
                    kotlin.math.sqrt(pixelCount.toDouble() / 4_000_000.0)
                ).toInt().coerceAtLeast(1)
                val sampledWidth = safeWidth / sampleSize
                val sampledHeight = safeHeight / sampleSize
                val bytes = document.renderPage(pageIndex, sampledWidth, sampledHeight) ?: return@withContext null
                val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            } else {
                val bytes = document.renderPage(pageIndex, safeWidth, safeHeight) ?: return@withContext null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (e: OutOfMemoryError) {
            System.gc()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun closeDocument(document: PdfDocumentSource) {
        document.close()
    }
}
