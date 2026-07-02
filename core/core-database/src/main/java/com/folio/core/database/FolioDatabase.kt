package com.folio.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BookEntity::class, ShelfEntity::class, BookShelfCrossRef::class, ReadingProgressEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FolioDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun shelfDao(): ShelfDao
    abstract fun readingProgressDao(): ReadingProgressDao
}
