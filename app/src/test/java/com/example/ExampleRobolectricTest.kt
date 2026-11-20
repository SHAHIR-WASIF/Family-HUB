package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.BackupManager
import com.example.data.backup.RestoreMode
import com.example.data.local.FamilyHubDatabase
import com.example.data.local.entity.CustomFieldEntity
import com.example.data.local.entity.CustomFieldType
import com.example.data.local.entity.FamilyEventEntity
import com.example.data.local.entity.FamilyEventType
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.MemoryCategory
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.RelationshipType
import com.example.data.media.MediaStorageManager
import com.example.data.repository.FamilyRepository
import com.example.data.repository.RelationshipDraft
import com.example.util.PdfExportHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: FamilyHubDatabase
    private lateinit var mediaStorageManager: MediaStorageManager
    private lateinit var repository: FamilyRepository
    private lateinit var backupManager: BackupManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FamilyHubDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        mediaStorageManager = MediaStorageManager(context)
        repository = FamilyRepository(db, mediaStorageManager)
        val json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        backupManager = BackupManager(context, db, mediaStorageManager, json)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `app name matches Family Hub`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Family Hub", appName)
    }

    @Test
    fun `save member with only fullName and all optional fields empty`() = runBlocking {
        val savedId = repository.saveMemberWithDetails(
            member = FamilyMemberEntity(fullName = "Elena Vance"),
            relationships = RelationshipDraft(),
            customFields = emptyList()
        )
        assertTrue(savedId > 0L)

        val loaded = repository.getMemberById(savedId)
        assertNotNull(loaded)
        assertEquals("Elena Vance", loaded?.fullName)
        assertNull(loaded?.dateOfBirth)
        assertNull(loaded?.phoneNumber)
        assertNull(loaded?.biography)
    }

    @Test
    fun `reciprocal relationships and custom fields persist accurately`() = runBlocking {
        val fatherId = repository.saveMemberWithDetails(
            member = FamilyMemberEntity(fullName = "Arthur Vance", gender = "Male"),
            relationships = RelationshipDraft(),
            customFields = emptyList()
        )
        val childId = repository.saveMemberWithDetails(
            member = FamilyMemberEntity(fullName = "Maya Vance", gender = "Female"),
            relationships = RelationshipDraft(fatherId = fatherId),
            customFields = listOf(
                CustomFieldEntity(memberId = 0L, fieldName = "Favorite Food", fieldType = CustomFieldType.TEXT, fieldValue = ""),
                CustomFieldEntity(memberId = 0L, fieldName = "Plays Piano", fieldType = CustomFieldType.BOOLEAN, fieldValue = "Yes")
            )
        )

        val allRels = repository.allRelationships.first()
        assertTrue(allRels.any { it.fromMemberId == childId && it.toMemberId == fatherId && it.type == RelationshipType.FATHER })
        assertTrue(allRels.any { it.fromMemberId == fatherId && it.toMemberId == childId && it.type == RelationshipType.CHILD })

        val profile = repository.observeMemberProfile(childId).first()
        assertNotNull(profile)
        assertEquals(2, profile?.customFields?.size)
        assertEquals("", profile?.customFields?.first()?.fieldValue)
    }

    @Test
    fun `json backup and restore preserves members relationships memories and events`() = runBlocking {
        val parentId = repository.saveMemberWithDetails(
            member = FamilyMemberEntity(fullName = "Clara Reed", dateOfBirth = "1972-05-14", gender = "Female"),
            relationships = RelationshipDraft(),
            customFields = listOf(CustomFieldEntity(memberId = 0L, fieldName = "Hobbies", fieldType = CustomFieldType.TEXT, fieldValue = "Gardening"))
        )
        val spouseId = repository.saveMemberWithDetails(
            member = FamilyMemberEntity(fullName = "David Reed", gender = "Male"),
            relationships = RelationshipDraft(spouseIds = listOf(parentId)),
            customFields = emptyList()
        )
        repository.saveMemory(
            MemoryEntity(memberId = parentId, category = MemoryCategory.ACHIEVEMENT, title = "Published Novel", description = "First book")
        )
        repository.saveEvent(
            FamilyEventEntity(title = "Anniversary", eventDateIso = "2000-06-15", eventType = FamilyEventType.ANNIVERSARY, relatedMemberId = parentId)
        )

        val backupJson = backupManager.exportToJsonString(includePrivateNotes = true)
        repository.clearAllData()
        assertEquals(0, repository.allMembers.first().size)

        val restoreResult = backupManager.importFromJsonString(backupJson, RestoreMode.REPLACE_EXISTING)
        assertTrue(restoreResult.isSuccess)
        val summary = restoreResult.getOrThrow()
        assertEquals(2, summary.membersRestored)
        assertTrue(summary.relationshipsRestored >= 2)
        assertEquals(1, summary.customFieldsRestored)
        assertEquals(1, summary.memoriesRestored)
        assertEquals(1, summary.eventsRestored)

        val restoredMembers = repository.allMembers.first()
        assertEquals(2, restoredMembers.size)
        assertTrue(restoredMembers.any { it.fullName == "Clara Reed" })
        assertTrue(restoredMembers.any { it.fullName == "David Reed" })
    }

    @Test
    fun `java PdfExportHelper generates valid member profile PDF`() {
        val data = PdfExportHelper.MemberPdfData(
            "Clara Reed",
            "Grammy",
            "May 14, 1972",
            "Female",
            "O+",
            "Mother",
            "Loves gardening and baking.",
            null,
            listOf("Spouse: David Reed"),
            listOf("Birthplace: Portland"),
            listOf("Achievement: Published Novel"),
            listOf("Hobbies: Gardening")
        )
        val file = PdfExportHelper.generateMemberShareImage(context, data)
        assertTrue(file.exists())
        assertTrue(file.length() > 500L)
    }
}
