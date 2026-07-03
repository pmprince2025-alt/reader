package com.folio.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String? = null,
    val filePath: String,
    val uri: String? = null,
    val pageCount: Int = 0,
    val fileSize: Long = 0,
    val coverThumbnailPath: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
    val lastOpened: Long? = null,
    val isCorrupted: Boolean = false,
    val isPasswordProtected: Boolean = false,
    val isFavorite: Boolean = false,
    val contentHash: String? = null
)
