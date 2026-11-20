package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CustomFieldEntity
import com.example.data.local.entity.FamilyEventEntity
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.GalleryPhotoEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.RelationshipEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyMemberDao {
    @Query("SELECT * FROM family_members ORDER BY fullName COLLATE NOCASE ASC")
    fun observeAllMembers(): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members ORDER BY fullName COLLATE NOCASE ASC")
    suspend fun getAllMembersOnce(): List<FamilyMemberEntity>

    @Query("SELECT * FROM family_members WHERE id = :id")
    fun observeMemberById(id: Long): Flow<FamilyMemberEntity?>

    @Query("SELECT * FROM family_members WHERE id = :id")
    suspend fun getMemberById(id: Long): FamilyMemberEntity?

    @Query("SELECT * FROM family_members ORDER BY createdAt DESC LIMIT :limit")
    fun observeRecentlyAdded(limit: Int = 8): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecentlyUpdated(limit: Int = 8): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE lastViewedAt IS NOT NULL ORDER BY lastViewedAt DESC LIMIT :limit")
    fun observeRecentlyViewed(limit: Int = 8): Flow<List<FamilyMemberEntity>>

    @Query("SELECT * FROM family_members WHERE isPinned = 1 OR isFavorite = 1 ORDER BY isPinned DESC, fullName COLLATE NOCASE ASC")
    fun observePinnedAndFavorites(): Flow<List<FamilyMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: FamilyMemberEntity): Long

    @Update
    suspend fun updateMember(member: FamilyMemberEntity)

    @Query("UPDATE family_members SET isPinned = :pinned, updatedAt = :timestamp WHERE id = :memberId")
    suspend fun setPinned(memberId: Long, pinned: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE family_members SET isFavorite = :favorite, updatedAt = :timestamp WHERE id = :memberId")
    suspend fun setFavorite(memberId: Long, favorite: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE family_members SET lastViewedAt = :timestamp WHERE id = :memberId")
    suspend fun markViewed(memberId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM family_members WHERE id = :memberId")
    suspend fun deleteMemberById(memberId: Long)

    @Query("DELETE FROM family_members")
    suspend fun deleteAllMembers()
}

@Dao
interface RelationshipDao {
    @Query("SELECT * FROM relationships")
    fun observeAllRelationships(): Flow<List<RelationshipEntity>>

    @Query("SELECT * FROM relationships")
    suspend fun getAllRelationshipsOnce(): List<RelationshipEntity>

    @Query("SELECT * FROM relationships WHERE fromMemberId = :memberId OR toMemberId = :memberId")
    fun observeRelationshipsForMember(memberId: Long): Flow<List<RelationshipEntity>>

    @Query("SELECT * FROM relationships WHERE fromMemberId = :memberId")
    suspend fun getOutgoingRelationships(memberId: Long): List<RelationshipEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRelationship(relationship: RelationshipEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(relationships: List<RelationshipEntity>)

    @Query("DELETE FROM relationships WHERE fromMemberId = :memberId")
    suspend fun deleteOutgoingForMember(memberId: Long)

    @Query("DELETE FROM relationships WHERE (fromMemberId = :memberA AND toMemberId = :memberB) OR (fromMemberId = :memberB AND toMemberId = :memberA)")
    suspend fun deleteBetweenMembers(memberA: Long, memberB: Long)

    @Query("DELETE FROM relationships WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM relationships")
    suspend fun deleteAllRelationships()
}

@Dao
interface CustomFieldDao {
    @Query("SELECT * FROM custom_fields WHERE memberId = :memberId ORDER BY id ASC")
    fun observeForMember(memberId: Long): Flow<List<CustomFieldEntity>>

    @Query("SELECT * FROM custom_fields")
    suspend fun getAllOnce(): List<CustomFieldEntity>

    @Query("SELECT * FROM custom_fields WHERE memberId = :memberId ORDER BY id ASC")
    suspend fun getForMemberOnce(memberId: Long): List<CustomFieldEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(field: CustomFieldEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(fields: List<CustomFieldEntity>)

    @Update
    suspend fun update(field: CustomFieldEntity)

    @Query("DELETE FROM custom_fields WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM custom_fields WHERE memberId = :memberId")
    suspend fun deleteForMember(memberId: Long)

    @Query("DELETE FROM custom_fields")
    suspend fun deleteAll()
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE memberId = :memberId ORDER BY createdAt DESC")
    fun observeForMember(memberId: Long): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories")
    suspend fun getAllOnce(): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE memberId = :memberId ORDER BY createdAt DESC")
    suspend fun getForMemberOnce(memberId: Long): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(memory: MemoryEntity): Long

    @Update
    suspend fun update(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM memories")
    suspend fun deleteAll()
}

@Dao
interface GalleryPhotoDao {
    @Query("SELECT * FROM gallery_photos ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GalleryPhotoEntity>>

    @Query("SELECT * FROM gallery_photos WHERE memberId = :memberId ORDER BY createdAt DESC")
    fun observeForMember(memberId: Long): Flow<List<GalleryPhotoEntity>>

    @Query("SELECT * FROM gallery_photos")
    suspend fun getAllOnce(): List<GalleryPhotoEntity>

    @Query("SELECT * FROM gallery_photos WHERE memberId = :memberId")
    suspend fun getForMemberOnce(memberId: Long): List<GalleryPhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: GalleryPhotoEntity): Long

    @Update
    suspend fun update(photo: GalleryPhotoEntity)

    @Query("DELETE FROM gallery_photos WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM gallery_photos")
    suspend fun deleteAll()
}

@Dao
interface FamilyEventDao {
    @Query("SELECT * FROM family_events ORDER BY eventDateIso ASC")
    fun observeAll(): Flow<List<FamilyEventEntity>>

    @Query("SELECT * FROM family_events")
    suspend fun getAllOnce(): List<FamilyEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: FamilyEventEntity): Long

    @Update
    suspend fun update(event: FamilyEventEntity)

    @Delete
    suspend fun delete(event: FamilyEventEntity)

    @Query("DELETE FROM family_events WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM family_events")
    suspend fun deleteAll()
}
