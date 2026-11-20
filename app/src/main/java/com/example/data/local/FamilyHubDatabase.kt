package com.example.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CustomFieldDao
import com.example.data.local.dao.FamilyEventDao
import com.example.data.local.dao.FamilyMemberDao
import com.example.data.local.dao.GalleryPhotoDao
import com.example.data.local.dao.MemoryDao
import com.example.data.local.dao.RelationshipDao
import com.example.data.local.entity.CustomFieldEntity
import com.example.data.local.entity.FamilyEventEntity
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.GalleryPhotoEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.RelationshipEntity

@Database(
    entities = [
        FamilyMemberEntity::class,
        RelationshipEntity::class,
        CustomFieldEntity::class,
        MemoryEntity::class,
        GalleryPhotoEntity::class,
        FamilyEventEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(FamilyHubConverters::class)
abstract class FamilyHubDatabase : RoomDatabase() {
    abstract fun familyMemberDao(): FamilyMemberDao
    abstract fun relationshipDao(): RelationshipDao
    abstract fun customFieldDao(): CustomFieldDao
    abstract fun memoryDao(): MemoryDao
    abstract fun galleryPhotoDao(): GalleryPhotoDao
    abstract fun familyEventDao(): FamilyEventDao

    companion object {
        const val DATABASE_NAME = "family_hub_db"

        /**
         * Safe non-destructive migration template for future schema upgrades.
         * Never use fallbackToDestructiveMigration() so family data survives app updates.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Future schema additions go here using ALTER TABLE / CREATE TABLE
            }
        }
    }
}
