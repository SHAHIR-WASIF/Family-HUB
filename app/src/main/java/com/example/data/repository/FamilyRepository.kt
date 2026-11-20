package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.FamilyHubDatabase
import com.example.data.local.entity.CustomFieldEntity
import com.example.data.local.entity.FamilyEventEntity
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.GalleryPhotoEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.RelationshipEntity
import com.example.data.local.entity.RelationshipType
import com.example.data.media.MediaStorageManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

data class MemberFullProfile(
    val member: FamilyMemberEntity,
    val outgoingRelationships: List<RelationshipEntity>,
    val customFields: List<CustomFieldEntity>,
    val memories: List<MemoryEntity>,
    val photos: List<GalleryPhotoEntity>
)

data class RelationshipDraft(
    val fatherId: Long? = null,
    val motherId: Long? = null,
    val spouseIds: List<Long> = emptyList(),
    val childIds: List<Long> = emptyList(),
    val brotherIds: List<Long> = emptyList(),
    val sisterIds: List<Long> = emptyList(),
    val grandparentIds: List<Long> = emptyList(),
    val otherRelativeIds: List<Long> = emptyList()
)

@Singleton
class FamilyRepository @Inject constructor(
    private val database: FamilyHubDatabase,
    private val mediaStorageManager: MediaStorageManager
) {
    private val memberDao = database.familyMemberDao()
    private val relationshipDao = database.relationshipDao()
    private val customFieldDao = database.customFieldDao()
    private val memoryDao = database.memoryDao()
    private val galleryPhotoDao = database.galleryPhotoDao()
    private val eventDao = database.familyEventDao()

    val allMembers: Flow<List<FamilyMemberEntity>> = memberDao.observeAllMembers()
    val allRelationships: Flow<List<RelationshipEntity>> = relationshipDao.observeAllRelationships()
    val recentlyAddedMembers: Flow<List<FamilyMemberEntity>> = memberDao.observeRecentlyAdded()
    val recentlyUpdatedMembers: Flow<List<FamilyMemberEntity>> = memberDao.observeRecentlyUpdated()
    val recentlyViewedMembers: Flow<List<FamilyMemberEntity>> = memberDao.observeRecentlyViewed()
    val pinnedAndFavoriteMembers: Flow<List<FamilyMemberEntity>> = memberDao.observePinnedAndFavorites()
    val allGalleryPhotos: Flow<List<GalleryPhotoEntity>> = galleryPhotoDao.observeAll()
    val allEvents: Flow<List<FamilyEventEntity>> = eventDao.observeAll()
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.observeAll()

    fun observeMemberProfile(memberId: Long): Flow<MemberFullProfile?> {
        return combine(
            memberDao.observeMemberById(memberId),
            relationshipDao.observeRelationshipsForMember(memberId),
            customFieldDao.observeForMember(memberId),
            memoryDao.observeForMember(memberId),
            galleryPhotoDao.observeForMember(memberId)
        ) { member, relationships, fields, memories, photos ->
            member?.let {
                MemberFullProfile(
                    member = it,
                    outgoingRelationships = relationships.filter { r -> r.fromMemberId == memberId },
                    customFields = fields,
                    memories = memories,
                    photos = photos
                )
            }
        }
    }

    suspend fun getMemberById(memberId: Long): FamilyMemberEntity? = memberDao.getMemberById(memberId)

    suspend fun getMemberDraftContext(memberId: Long): Triple<FamilyMemberEntity?, RelationshipDraft, List<CustomFieldEntity>> {
        val member = memberDao.getMemberById(memberId) ?: return Triple(null, RelationshipDraft(), emptyList())
        val rels = relationshipDao.getOutgoingRelationships(memberId)
        val draft = RelationshipDraft(
            fatherId = rels.firstOrNull { it.type == RelationshipType.FATHER }?.toMemberId,
            motherId = rels.firstOrNull { it.type == RelationshipType.MOTHER }?.toMemberId,
            spouseIds = rels.filter { it.type == RelationshipType.SPOUSE }.map { it.toMemberId }.distinct(),
            childIds = rels.filter { it.type == RelationshipType.CHILD }.map { it.toMemberId }.distinct(),
            brotherIds = rels.filter { it.type == RelationshipType.BROTHER }.map { it.toMemberId }.distinct(),
            sisterIds = rels.filter { it.type == RelationshipType.SISTER }.map { it.toMemberId }.distinct(),
            grandparentIds = rels.filter { it.type == RelationshipType.GRANDPARENT }.map { it.toMemberId }.distinct(),
            otherRelativeIds = rels.filter { it.type == RelationshipType.OTHER_RELATIVE }.map { it.toMemberId }.distinct()
        )
        val fields = customFieldDao.getForMemberOnce(memberId)
        return Triple(member, draft, fields)
    }

    suspend fun findPotentialDuplicates(fullName: String, excludeId: Long? = null): List<FamilyMemberEntity> {
        val normalized = fullName.trim().lowercase()
        if (normalized.isEmpty()) return emptyList()
        return memberDao.getAllMembersOnce().filter { candidate ->
            candidate.id != excludeId &&
                candidate.fullName.trim().lowercase() == normalized
        }
    }

    /**
     * Saves or updates a family member along with their relationships and custom fields
     * inside a single Room transaction, keeping reciprocal relationship edges consistent.
     */
    suspend fun saveMemberWithDetails(
        member: FamilyMemberEntity,
        relationships: RelationshipDraft,
        customFields: List<CustomFieldEntity>
    ): Long {
        val now = System.currentTimeMillis()
        val isNew = member.id == 0L
        val sanitized = member.copy(
            fullName = member.fullName.trim(),
            createdAt = if (isNew) now else member.createdAt,
            updatedAt = now
        )

        return database.withTransaction {
            val savedId = if (isNew) {
                memberDao.insertMember(sanitized)
            } else {
                memberDao.updateMember(sanitized)
                sanitized.id
            }

            // Replace outgoing relationships for this member and synchronize reciprocals
            relationshipDao.deleteOutgoingForMember(savedId)
            val toInsert = mutableListOf<RelationshipEntity>()

            fun addEdge(targetId: Long, type: RelationshipType) {
                if (targetId != savedId && targetId > 0L) {
                    toInsert.add(RelationshipEntity(fromMemberId = savedId, toMemberId = targetId, type = type))
                }
            }

            relationships.fatherId?.let { fatherId ->
                addEdge(fatherId, RelationshipType.FATHER)
                if (fatherId != savedId) {
                    relationshipDao.insertRelationship(
                        RelationshipEntity(fromMemberId = fatherId, toMemberId = savedId, type = RelationshipType.CHILD)
                    )
                }
            }
            relationships.motherId?.let { motherId ->
                addEdge(motherId, RelationshipType.MOTHER)
                if (motherId != savedId) {
                    relationshipDao.insertRelationship(
                        RelationshipEntity(fromMemberId = motherId, toMemberId = savedId, type = RelationshipType.CHILD)
                    )
                }
            }
            relationships.spouseIds.distinct().forEach { spouseId ->
                addEdge(spouseId, RelationshipType.SPOUSE)
                if (spouseId != savedId) {
                    relationshipDao.insertRelationship(
                        RelationshipEntity(fromMemberId = spouseId, toMemberId = savedId, type = RelationshipType.SPOUSE)
                    )
                }
            }
            relationships.childIds.distinct().forEach { childId ->
                addEdge(childId, RelationshipType.CHILD)
                val parentType = when (sanitized.gender?.lowercase()) {
                    "male" -> RelationshipType.FATHER
                    "female" -> RelationshipType.MOTHER
                    else -> null
                }
                if (parentType != null && childId != savedId) {
                    relationshipDao.insertRelationship(
                        RelationshipEntity(fromMemberId = childId, toMemberId = savedId, type = parentType)
                    )
                }
            }
            relationships.brotherIds.distinct().forEach { broId ->
                addEdge(broId, RelationshipType.BROTHER)
                val reciprocal = when (sanitized.gender?.lowercase()) {
                    "female" -> RelationshipType.SISTER
                    else -> RelationshipType.BROTHER
                }
                if (broId != savedId) {
                    relationshipDao.insertRelationship(
                        RelationshipEntity(fromMemberId = broId, toMemberId = savedId, type = reciprocal)
                    )
                }
            }
            relationships.sisterIds.distinct().forEach { sisId ->
                addEdge(sisId, RelationshipType.SISTER)
                val reciprocal = when (sanitized.gender?.lowercase()) {
                    "male" -> RelationshipType.BROTHER
                    else -> RelationshipType.SISTER
                }
                if (sisId != savedId) {
                    relationshipDao.insertRelationship(
                        RelationshipEntity(fromMemberId = sisId, toMemberId = savedId, type = reciprocal)
                    )
                }
            }
            relationships.grandparentIds.distinct().forEach { gpId ->
                addEdge(gpId, RelationshipType.GRANDPARENT)
            }
            relationships.otherRelativeIds.distinct().forEach { relId ->
                addEdge(relId, RelationshipType.OTHER_RELATIVE)
                if (relId != savedId) {
                    relationshipDao.insertRelationship(
                        RelationshipEntity(fromMemberId = relId, toMemberId = savedId, type = RelationshipType.OTHER_RELATIVE)
                    )
                }
            }

            if (toInsert.isNotEmpty()) {
                relationshipDao.insertAll(toInsert)
            }

            // Replace custom fields for this member
            customFieldDao.deleteForMember(savedId)
            val validFields = customFields
                .filter { it.fieldName.isNotBlank() }
                .map { it.copy(id = 0, memberId = savedId, fieldName = it.fieldName.trim()) }
            if (validFields.isNotEmpty()) {
                customFieldDao.insertAll(validFields)
            }

            savedId
        }
    }

    suspend fun markMemberViewed(memberId: Long) {
        memberDao.markViewed(memberId, System.currentTimeMillis())
    }

    suspend fun togglePinned(memberId: Long, pinned: Boolean) {
        memberDao.setPinned(memberId, pinned)
    }

    suspend fun toggleFavorite(memberId: Long, favorite: Boolean) {
        memberDao.setFavorite(memberId, favorite)
    }

    suspend fun deleteMember(memberId: Long, deleteAssociatedMedia: Boolean) {
        val member = memberDao.getMemberById(memberId)
        val memories = memoryDao.getForMemberOnce(memberId)
        val photos = galleryPhotoDao.getForMemberOnce(memberId)

        if (deleteAssociatedMedia) {
            mediaStorageManager.deleteLocalFileIfOwned(member?.profilePhotoPath)
            memories.forEach { mediaStorageManager.deleteLocalFileIfOwned(it.attachmentPath) }
            photos.forEach {
                mediaStorageManager.deleteLocalFileIfOwned(it.filePath)
                galleryPhotoDao.deleteById(it.id)
            }
        }
        memberDao.deleteMemberById(memberId)
    }

    suspend fun saveCustomField(field: CustomFieldEntity): Long {
        val now = System.currentTimeMillis()
        val id = customFieldDao.insert(field.copy(fieldName = field.fieldName.trim()))
        memberDao.getMemberById(field.memberId)?.let {
            memberDao.updateMember(it.copy(updatedAt = now))
        }
        return id
    }

    suspend fun deleteCustomField(fieldId: Long) {
        customFieldDao.deleteById(fieldId)
    }

    suspend fun saveMemory(memory: MemoryEntity): Long {
        val id = memoryDao.insert(memory.copy(title = memory.title.trim()))
        memberDao.getMemberById(memory.memberId)?.let {
            memberDao.updateMember(it.copy(updatedAt = System.currentTimeMillis()))
        }
        return id
    }

    suspend fun deleteMemory(memory: MemoryEntity) {
        mediaStorageManager.deleteLocalFileIfOwned(memory.attachmentPath)
        memoryDao.deleteById(memory.id)
    }

    suspend fun saveGalleryPhoto(photo: GalleryPhotoEntity): Long {
        return galleryPhotoDao.insert(photo)
    }

    suspend fun deleteGalleryPhoto(photo: GalleryPhotoEntity) {
        mediaStorageManager.deleteLocalFileIfOwned(photo.filePath)
        galleryPhotoDao.deleteById(photo.id)
    }

    suspend fun saveEvent(event: FamilyEventEntity): Long {
        return eventDao.insert(event.copy(title = event.title.trim()))
    }

    suspend fun deleteEvent(eventId: Long) {
        eventDao.deleteById(eventId)
    }

    suspend fun clearAllData() {
        database.withTransaction {
            eventDao.deleteAll()
            galleryPhotoDao.deleteAll()
            memoryDao.deleteAll()
            customFieldDao.deleteAll()
            relationshipDao.deleteAllRelationships()
            memberDao.deleteAllMembers()
        }
        mediaStorageManager.clearAllManagedMedia()
    }
}
