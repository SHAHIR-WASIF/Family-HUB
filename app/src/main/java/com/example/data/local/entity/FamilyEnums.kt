package com.example.data.local.entity

import kotlinx.serialization.Serializable

@Serializable
enum class RelationshipType(val displayName: String) {
    FATHER("Father"),
    MOTHER("Mother"),
    SPOUSE("Spouse"),
    CHILD("Child"),
    BROTHER("Brother"),
    SISTER("Sister"),
    GRANDPARENT("Grandparent"),
    OTHER_RELATIVE("Other Relative")
}

@Serializable
enum class CustomFieldType(val displayName: String) {
    TEXT("Text"),
    NUMBER("Number"),
    DATE("Date"),
    BOOLEAN("Yes / No")
}

@Serializable
enum class MemoryCategory(val displayName: String) {
    LIFE_EVENT("Life Event"),
    ACHIEVEMENT("Achievement"),
    STORY("Memory & Story"),
    CONTRIBUTION("Family Contribution"),
    DOCUMENT("Historical Document"),
    CUSTOM_NOTE("Custom Note")
}

@Serializable
enum class FamilyEventType(val displayName: String) {
    ANNIVERSARY("Anniversary"),
    REUNION("Family Reunion"),
    MILESTONE("Milestone"),
    MEMORIAL("Memorial"),
    CUSTOM("Custom Event")
}
