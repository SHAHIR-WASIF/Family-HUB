package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "family_members")
data class FamilyMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val profilePhotoPath: String? = null,
    val dateOfBirth: String? = null, // ISO YYYY-MM-DD
    val gender: String? = null,
    val nickname: String? = null,
    val bloodGroup: String? = null,
    val biography: String? = null,
    val relationshipToOwner: String? = null,
    val generationOffset: Int = 0,
    val birthplace: String? = null,
    val currentAddress: String? = null,
    val nationality: String? = null,
    val occupation: String? = null,
    val education: String? = null,
    val phoneNumber: String? = null,
    val emailAddress: String? = null,
    val personalWebsite: String? = null,
    val importantNotes: String? = null,
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val lastViewedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "relationships",
    foreignKeys = [
        ForeignKey(
            entity = FamilyMemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["fromMemberId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FamilyMemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["toMemberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["fromMemberId"]),
        Index(value = ["toMemberId"]),
        Index(value = ["fromMemberId", "toMemberId", "type"], unique = true)
    ]
)
data class RelationshipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromMemberId: Long,
    val toMemberId: Long,
    val type: RelationshipType,
    val customLabel: String? = null
)

@Serializable
@Entity(
    tableName = "custom_fields",
    foreignKeys = [
        ForeignKey(
            entity = FamilyMemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["memberId"])]
)
data class CustomFieldEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long,
    val fieldName: String,
    val fieldType: CustomFieldType,
    val fieldValue: String = ""
)

@Serializable
@Entity(
    tableName = "memories",
    foreignKeys = [
        ForeignKey(
            entity = FamilyMemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["memberId"])]
)
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long,
    val category: MemoryCategory,
    val title: String,
    val description: String = "",
    val dateIso: String? = null,
    val attachmentPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "gallery_photos",
    foreignKeys = [
        ForeignKey(
            entity = FamilyMemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["memberId"]), Index(value = ["albumName"])]
)
data class GalleryPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memberId: Long? = null,
    val albumName: String = "Family Moments",
    val filePath: String,
    val caption: String = "",
    val takenDateIso: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
@Entity(
    tableName = "family_events",
    foreignKeys = [
        ForeignKey(
            entity = FamilyMemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["relatedMemberId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["relatedMemberId"])]
)
data class FamilyEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val eventDateIso: String, // ISO YYYY-MM-DD
    val eventType: FamilyEventType = FamilyEventType.CUSTOM,
    val relatedMemberId: Long? = null,
    val isRecurringYearly: Boolean = true,
    val reminderEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
