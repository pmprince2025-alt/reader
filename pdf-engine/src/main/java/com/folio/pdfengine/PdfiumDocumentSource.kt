package com.folio.pdfengine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

/**
 * PdfDocumentSource backed by Android's built-in PdfRenderer.
 * Used as the universal fallback when PdfiumAndroid native JNI is unavailable.
 * This handles all standard PDF rendering but cannot extract bookmarks (TOC).
 */
internal class PdfiumDocumentSource(
    private val pfd: ParcelFileDescriptor,
    private val filePath: String,
    private val deleteOnClose: Boolean = false
) : PdfDocumentSource {

    private var renderer: PdfRenderer? = try {
        PdfRenderer(pfd)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    override val pageCount: Int get() = renderer?.pageCount ?: 0

    override fun getPageWidth(index: Int): Float {
        val page = renderer?.openPage(index) ?: return 0f
        val width = page.width.toFloat()
        page.close()
        return width
    }

    override fun getPageHeight(index: Int): Float {
        val page = renderer?.openPage(index) ?: return 0f
        val height = page.height.toFloat()
        page.close()
        return height
    }

    override suspend fun renderPage(index: Int, width: Int, height: Int): ByteArray? {
        val page = renderer?.openPage(index) ?: return null
        val bitmap = try {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            page.close()
            return null
        }
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()

        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    override fun getTableOfContents(): List<PdfDocumentSource.TocEntry> {
        return emptyList()
    }

    override fun close() {
        renderer?.close()
        renderer = null
        try { pfd.close() } catch (_: Exception) {}
        if (deleteOnClose) {
            try { File(filePath).delete() } catch (_: Exception) {}
        }
    }
}
