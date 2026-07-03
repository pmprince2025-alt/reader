package com.folio.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {
    @Query("SELECT * FROM reading_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startedAt DESC")
    fun getSessionsForBook(bookId: Long): Flow<List<ReadingSessionEntity>>

    @Insert
    suspend fun insertSession(session: ReadingSessionEntity): Long

    @Query("SELECT COALESCE(SUM(pagesRead), 0) FROM reading_sessions WHERE startedAt >= :since")
    suspend fun getTotalPagesReadSince(since: Long): Int

    @Query("SELECT COUNT(DISTINCT date(startedAt / 1000, 'unixepoch')) FROM reading_sessions WHERE startedAt >= :since")
    suspend fun getReadingDaysSince(since: Long): Int
}
