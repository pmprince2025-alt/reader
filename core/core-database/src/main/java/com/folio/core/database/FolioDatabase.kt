package com.folio.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [BookEntity::class, ShelfEntity::class, BookShelfCrossRef::class, ReadingProgressEntity::class, BookmarkEntity::class, ReadingSessionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class FolioDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun shelfDao(): ShelfDao
    abstract fun readingProgressDao(): ReadingProgressDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun readingSessionDao(): ReadingSessionDao

    companion object {
        val MIGRATION_1_2 = Migration(1, 2) { db ->
            db.execSQL("ALTER TABLE books ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS bookmarks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    bookId INTEGER NOT NULL,
                    pageIndex INTEGER NOT NULL,
                    label TEXT,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY (bookId) REFERENCES books(id) ON DELETE CASCADE,
                    UNIQUE(bookId, pageIndex)
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_bookId ON bookmarks(bookId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_bookId_pageIndex ON bookmarks(bookId, pageIndex)")
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS reading_sessions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    bookId INTEGER NOT NULL,
                    pagesRead INTEGER NOT NULL DEFAULT 0,
                    startPage INTEGER NOT NULL DEFAULT 0,
                    endPage INTEGER NOT NULL DEFAULT 0,
                    startedAt INTEGER NOT NULL,
                    durationMinutes INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (bookId) REFERENCES books(id) ON DELETE CASCADE
                )
            """)
            db.execSQL("CREATE INDEX IF NOT EXISTS index_reading_sessions_bookId ON reading_sessions(bookId)")
        }
    }
}
