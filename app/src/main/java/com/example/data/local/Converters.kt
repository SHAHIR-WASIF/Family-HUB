package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.local.entity.CustomFieldType
import com.example.data.local.entity.FamilyEventType
import com.example.data.local.entity.MemoryCategory
import com.example.data.local.entity.RelationshipType

class FamilyHubConverters {
    @TypeConverter
    fun fromRelationshipType(value: RelationshipType): String = value.name

    @TypeConverter
    fun toRelationshipType(value: String): RelationshipType =
        runCatching { RelationshipType.valueOf(value) }.getOrDefault(RelationshipType.OTHER_RELATIVE)

    @TypeConverter
    fun fromCustomFieldType(value: CustomFieldType): String = value.name

    @TypeConverter
    fun toCustomFieldType(value: String): CustomFieldType =
        runCatching { CustomFieldType.valueOf(value) }.getOrDefault(CustomFieldType.TEXT)

    @TypeConverter
    fun fromMemoryCategory(value: MemoryCategory): String = value.name

    @TypeConverter
    fun toMemoryCategory(value: String): MemoryCategory =
        runCatching { MemoryCategory.valueOf(value) }.getOrDefault(MemoryCategory.CUSTOM_NOTE)

    @TypeConverter
    fun fromFamilyEventType(value: FamilyEventType): String = value.name

    @TypeConverter
    fun toFamilyEventType(value: String): FamilyEventType =
        runCatching { FamilyEventType.valueOf(value) }.getOrDefault(FamilyEventType.CUSTOM)
}
