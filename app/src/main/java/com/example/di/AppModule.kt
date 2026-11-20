package com.example.di

import android.content.Context
import androidx.room.Room
import com.example.data.local.FamilyHubDatabase
import com.example.data.local.dao.CustomFieldDao
import com.example.data.local.dao.FamilyEventDao
import com.example.data.local.dao.FamilyMemberDao
import com.example.data.local.dao.GalleryPhotoDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.RelationshipDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FamilyHubDatabase {
        return Room.databaseBuilder(
            context,
            FamilyHubDatabase::class.java,
            FamilyHubDatabase.DATABASE_NAME
        )
            .addMigrations(FamilyHubDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideFamilyMemberDao(db: FamilyHubDatabase): FamilyMemberDao = db.familyMemberDao()

    @Provides
    fun provideRelationshipDao(db: FamilyHubDatabase): RelationshipDao = db.relationshipDao()

    @Provides
    fun provideCustomFieldDao(db: FamilyHubDatabase): CustomFieldDao = db.customFieldDao()

    @Provides
    fun provideMemoryDao(db: FamilyHubDatabase): MemoryDao = db.memoryDao()

    @Provides
    fun provideGalleryPhotoDao(db: FamilyHubDatabase): GalleryPhotoDao = db.galleryPhotoDao()

    @Provides
    fun provideFamilyEventDao(db: FamilyHubDatabase): FamilyEventDao = db.familyEventDao()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}
