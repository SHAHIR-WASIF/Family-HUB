package com.example.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.data.local.FamilyHubDatabase
import com.example.data.local.entity.CustomFieldEntity
import com.example.data.local.entity.FamilyEventEntity
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.GalleryPhotoEntity
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.RelationshipEntity
import com.example.data.media.MediaStorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class FamilyBackupPayload(
    val schemaVersion: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val members: List<FamilyMemberEntity> = emptyList(),
    val relationships: List<RelationshipEntity> = emptyList(),
    val customFields: List<CustomFieldEntity> = emptyList(),
    val memories: List<MemoryEntity> = emptyList(),
    val galleryPhotos: List<GalleryPhotoEntity> = emptyList(),
    val events: List<FamilyEventEntity> = emptyList(),
    val mediaBase64: Map<String, String> = emptyMap()
)

data class RestoreSummary(
    val membersRestored: Int,
    val relationshipsRestored: Int,
    val customFieldsRestored: Int,
    val memoriesRestored: Int,
    val photosRestored: Int,
    val eventsRestored: Int
)

enum class RestoreMode {
    REPLACE_EXISTING,
    MERGE_WITH_EXISTING
}

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: FamilyHubDatabase,
    private val mediaStorageManager: MediaStorageManager,
    private val json: Json
) {
    suspend fun createBackupPayload(includePrivateNotes: Boolean = true): FamilyBackupPayload =
        withContext(Dispatchers.IO) {
            val rawMembers = database.familyMemberDao().getAllMembersOnce()
            val members = if (includePrivateNotes) {
                rawMembers
            } else {
                rawMembers.map { it.copy(importantNotes = null) }
            }
            val relationships = database.relationshipDao().getAllRelationshipsOnce()
            val customFields = database.customFieldDao().getAllOnce()
            val memories = database.memoryDao().getAllOnce()
            val photos = database.galleryPhotoDao().getAllOnce()
            val events = database.familyEventDao().getAllOnce()

            val mediaMap = mutableMapOf<String, String>()
            members.mapNotNull { it.profilePhotoPath }.distinct().forEach { path ->
                mediaStorageManager.encodeFileToBase64(path)?.let { encoded ->
                    mediaMap[path] = encoded
                }
            }
            memories.mapNotNull { it.attachmentPath }.distinct().forEach { path ->
                if (!mediaMap.containsKey(path)) {
                    mediaStorageManager.encodeFileToBase64(path)?.let { encoded ->
                        mediaMap[path] = encoded
                    }
                }
            }
            photos.map { it.filePath }.distinct().forEach { path ->
                if (!mediaMap.containsKey(path)) {
                    mediaStorageManager.encodeFileToBase64(path)?.let { encoded ->
                        mediaMap[path] = encoded
                    }
                }
            }

            FamilyBackupPayload(
                schemaVersion = 1,
                exportedAt = System.currentTimeMillis(),
                members = members,
                relationships = relationships,
                customFields = customFields,
                memories = memories,
                galleryPhotos = photos,
                events = events,
                mediaBase64 = mediaMap
            )
        }

    suspend fun exportToJsonString(includePrivateNotes: Boolean = true): String {
        val payload = createBackupPayload(includePrivateNotes)
        return json.encodeToString(payload)
    }

    suspend fun exportToUri(uri: Uri, includePrivateNotes: Boolean = true): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = createBackupPayload(includePrivateNotes)
                val encoded = json.encodeToString(payload)
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    OutputStreamWriter(os, Charsets.UTF_8).use { writer ->
                        writer.write(encoded)
                    }
                } ?: error("Unable to open output stream for backup file")
                payload.members.size
            }
        }

    suspend fun importFromUri(uri: Uri, mode: RestoreMode): Result<RestoreSummary> =
        withContext(Dispatchers.IO) {
            runCatching {
                val rawJson = context.contentResolver.openInputStream(uri)?.use { input ->
                    BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
                } ?: error("Unable to read backup file")
                importFromJsonString(rawJson, mode).getOrThrow()
            }
        }

    suspend fun importFromJsonString(rawJson: String, mode: RestoreMode): Result<RestoreSummary> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = json.decodeFromString<FamilyBackupPayload>(rawJson)
                restorePayload(payload, mode)
            }
        }

    suspend fun restorePayload(payload: FamilyBackupPayload, mode: RestoreMode): RestoreSummary =
        withContext(Dispatchers.IO) {
            // Decode embedded media files first
            val restoredPathMap = mutableMapOf<String, String>()
            payload.mediaBase64.forEach { (originalPath, base64) ->
                val isDoc = originalPath.endsWith(".pdf", ignoreCase = true)
                mediaStorageManager.decodeBase64ToInternalFile(base64, isDocument = isDoc)?.let { newPath ->
                    restoredPathMap[originalPath] = newPath
                }
            }

            database.withTransaction {
                if (mode == RestoreMode.REPLACE_EXISTING) {
                    database.familyEventDao().deleteAll()
                    database.galleryPhotoDao().deleteAll()
                    database.memoryDao().deleteAll()
                    database.customFieldDao().deleteAll()
                    database.relationshipDao().deleteAllRelationships()
                    database.familyMemberDao().deleteAllMembers()
                }

                val memberIdMap = mutableMapOf<Long, Long>()
                val memberDao = database.familyMemberDao()
                payload.members.forEach { member ->
                    val newPhoto = member.profilePhotoPath?.let { restoredPathMap[it] ?: it }
                    val newId = memberDao.insertMember(
                        member.copy(
                            id = 0,
                            profilePhotoPath = newPhoto
                        )
                    )
                    memberIdMap[member.id] = newId
                }

                val relationshipDao = database.relationshipDao()
                var relCount = 0
                payload.relationships.forEach { rel ->
                    val fromId = memberIdMap[rel.fromMemberId]
                    val toId = memberIdMap[rel.toMemberId]
                    if (fromId != null && toId != null && fromId != toId) {
                        val inserted = relationshipDao.insertRelationship(
                            rel.copy(id = 0, fromMemberId = fromId, toMemberId = toId)
                        )
                        if (inserted > 0) relCount++
                    }
                }

                val customFieldDao = database.customFieldDao()
                var cfCount = 0
                payload.customFields.forEach { cf ->
                    val newMemberId = memberIdMap[cf.memberId]
                    if (newMemberId != null) {
                        customFieldDao.insert(cf.copy(id = 0, memberId = newMemberId))
                        cfCount++
                    }
                }

                val memoryDao = database.memoryDao()
                var memCount = 0
                payload.memories.forEach { mem ->
                    val newMemberId = memberIdMap[mem.memberId]
                    if (newMemberId != null) {
                        val newAtt = mem.attachmentPath?.let { restoredPathMap[it] ?: it }
                        memoryDao.insert(mem.copy(id = 0, memberId = newMemberId, attachmentPath = newAtt))
                        memCount++
                    }
                }

                val galleryDao = database.galleryPhotoDao()
                var photoCount = 0
                payload.galleryPhotos.forEach { photo ->
                    val newPath = restoredPathMap[photo.filePath] ?: photo.filePath
                    val newMemberId = photo.memberId?.let { memberIdMap[it] }
                    galleryDao.insert(photo.copy(id = 0, memberId = newMemberId, filePath = newPath))
                    photoCount++
                }

                val eventDao = database.familyEventDao()
                var eventCount = 0
                payload.events.forEach { event ->
                    val newMemberId = event.relatedMemberId?.let { memberIdMap[it] }
                    eventDao.insert(event.copy(id = 0, relatedMemberId = newMemberId))
                    eventCount++
                }

                RestoreSummary(
                    membersRestored = payload.members.size,
                    relationshipsRestored = relCount,
                    customFieldsRestored = cfCount,
                    memoriesRestored = memCount,
                    photosRestored = photoCount,
                    eventsRestored = eventCount
                )
            }
        }
}
