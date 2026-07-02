package com.folio.pdfengine

interface PdfDocumentSource {
    val pageCount: Int
    fun getPageWidth(index: Int): Float
    fun getPageHeight(index: Int): Float
    suspend fun renderPage(index: Int, width: Int, height: Int): ByteArray?
    fun getTableOfContents(): List<PdfDocumentSource.TocEntry>
    fun close()

    data class TocEntry(
        val title: String,
        val pageIndex: Int,
        val depth: Int = 0
    )
}
