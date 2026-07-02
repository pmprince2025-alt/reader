package com.folio.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShelfDao {
    @Query("SELECT * FROM shelves ORDER BY sortOrder ASC")
    fun getAllShelves(): Flow<List<ShelfEntity>>

    @Query("SELECT * FROM shelves WHERE id = :shelfId")
    suspend fun getShelfById(shelfId: Long): ShelfEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShelf(shelf: ShelfEntity): Long

    @Update
    suspend fun updateShelf(shelf: ShelfEntity)

    @Delete
    suspend fun deleteShelf(shelf: ShelfEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addBookToShelf(crossRef: BookShelfCrossRef)

    @Delete
    suspend fun removeBookFromShelf(crossRef: BookShelfCrossRef)

    @Query("SELECT b.* FROM books b INNER JOIN book_shelf_cross_ref c ON b.id = c.bookId WHERE c.shelfId = :shelfId")
    fun getBooksInShelf(shelfId: Long): Flow<List<BookEntity>>
}
