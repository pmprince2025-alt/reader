package com.folio.pdfengine

import android.graphics.Bitmap

interface PdfPageRenderer {
    suspend fun openDocument(filePath: String): PdfDocumentSource?
    suspend fun openDocumentFromBytes(bytes: ByteArray): PdfDocumentSource?
    suspend fun renderPageToBitmap(
        document: PdfDocumentSource,
        pageIndex: Int,
        width: Int,
        height: Int
    ): Bitmap?
    fun closeDocument(document: PdfDocumentSource)
}
