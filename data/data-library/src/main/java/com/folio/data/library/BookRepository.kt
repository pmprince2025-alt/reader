package com.folio.data.library

import com.folio.core.database.BookDao
import com.folio.core.database.BookEntity
import com.folio.core.database.ShelfDao
import com.folio.core.database.ShelfEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookRepository @Inject constructor(
    private val bookDao: BookDao,
    private val shelfDao: ShelfDao
) {
    fun getAllBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()
    fun getBooksByTitle(): Flow<List<BookEntity>> = bookDao.getBooksByTitle()
    fun getBooksByDateAdded(): Flow<List<BookEntity>> = bookDao.getBooksByDateAdded()
    fun getBooksByFileSize(): Flow<List<BookEntity>> = bookDao.getBooksByFileSize()
    fun getRecentlyOpened(): Flow<List<BookEntity>> = bookDao.getRecentlyOpened()
    fun searchBooks(query: String): Flow<List<BookEntity>> = bookDao.searchBooks(query)
    suspend fun getBookById(id: Long): BookEntity? = bookDao.getBookById(id)
    suspend fun insertBook(book: BookEntity): Long = bookDao.insertBook(book)
    suspend fun updateBook(book: BookEntity) = bookDao.updateBook(book)
    suspend fun deleteBook(book: BookEntity) = bookDao.deleteBook(book)

    fun getAllShelves(): Flow<List<ShelfEntity>> = shelfDao.getAllShelves()
    suspend fun createShelf(name: String): Long = shelfDao.insertShelf(ShelfEntity(name = name))
    suspend fun deleteShelf(id: Long) = shelfDao.getShelfById(id)?.let { shelfDao.deleteShelf(it) }
}
