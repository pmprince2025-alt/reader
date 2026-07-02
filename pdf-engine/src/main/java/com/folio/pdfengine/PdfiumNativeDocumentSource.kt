package com.folio.pdfengine

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.shockwave.pdfium.PdfiumCore
import java.io.ByteArrayOutputStream
import java.io.File

internal class PdfiumNativeDocumentSource(
    private val pfd: ParcelFileDescriptor,
    private val filePath: String,
    private val pdfiumCore: PdfiumCore? = null,
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

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    override fun getTableOfContents(): List<PdfDocumentSource.TocEntry> {
        val core = pdfiumCore ?: return emptyList()
        return try {
            val tocPfd = ParcelFileDescriptor.open(File(filePath), ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfDocument = core.newDocument(tocPfd, null)
            val bookmarks = core.getTableOfContents(pdfDocument)
            val entries = mutableListOf<PdfDocumentSource.TocEntry>()
            fun flatten(list: List<com.shockwave.pdfium.PdfDocument.Bookmark>, depth: Int) {
                for (bm in list) {
                    entries.add(
                        PdfDocumentSource.TocEntry(
                            title = bm.title,
                            pageIndex = bm.pageIdx.toInt(),
                            depth = depth
                        )
                    )
                    if (bm.hasChildren()) {
                        flatten(bm.children, depth + 1)
                    }
                }
            }
            flatten(bookmarks, 0)
            core.closeDocument(pdfDocument)
            tocPfd.close()
            entries
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
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
