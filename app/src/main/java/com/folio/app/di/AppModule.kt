package com.folio.app.di

import android.content.Context
import com.folio.core.database.*
import com.folio.core.datastore.SettingsDataStore
import com.folio.pdfengine.FallbackPdfRenderer
import com.folio.pdfengine.PdfPageRenderer
import com.folio.pdfengine.PdfiumEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FolioDatabase {
        return androidx.room.Room.databaseBuilder(
            context,
            FolioDatabase::class.java,
            "folio_database"
        ).addMigrations(FolioDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideBookDao(db: FolioDatabase): BookDao = db.bookDao()

    @Provides
    @Singleton
    fun provideShelfDao(db: FolioDatabase): ShelfDao = db.shelfDao()

    @Provides
    @Singleton
    fun provideReadingProgressDao(db: FolioDatabase): ReadingProgressDao = db.readingProgressDao()

    @Provides
    @Singleton
    fun provideBookmarkDao(db: FolioDatabase): BookmarkDao = db.bookmarkDao()

    @Provides
    @Singleton
    fun provideReadingSessionDao(db: FolioDatabase): ReadingSessionDao = db.readingSessionDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun providePdfPageRenderer(@ApplicationContext context: Context): PdfPageRenderer {
        return try {
            PdfiumEngine(context)
        } catch (e: Exception) {
            FallbackPdfRenderer()
        }
    }
}
