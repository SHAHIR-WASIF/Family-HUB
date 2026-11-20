package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CustomFieldEntity
import com.example.data.local.entity.CustomFieldType
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.GalleryPhotoEntity
import com.example.data.local.entity.MemoryCategory
import com.example.data.local.entity.MemoryEntity
import com.example.data.local.entity.RelationshipType
import com.example.data.media.MediaStorageManager
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FamilyRepository
import com.example.data.repository.MemberFullProfile
import com.example.domain.DateUtils
import com.example.domain.RelationshipLabelResolver
import com.example.util.PdfExportHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ResolvedRelationshipGroup(
    val type: RelationshipType,
    val label: String,
    val members: List<FamilyMemberEntity>
)

data class MemberDetailUiState(
    val isLoading: Boolean = true,
    val profile: MemberFullProfile? = null,
    val relationshipGroups: List<ResolvedRelationshipGroup> = emptyList(),
    val ownerLabel: String? = null,
    val hideSensitiveDetails: Boolean = false,
    val isSensitiveRevealed: Boolean = false,
    val exportMessage: String? = null
)

@HiltViewModel
class MemberDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val repository: FamilyRepository,
    private val mediaStorageManager: MediaStorageManager,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val memberId: Long = savedStateHandle.get<Long>("memberId") ?: 0L
    private val sensitiveRevealed = MutableStateFlow(false)
    private val exportMessage = MutableStateFlow<String?>(null)

    init {
        if (memberId > 0L) {
            viewModelScope.launch {
                repository.markMemberViewed(memberId)
            }
        }
    }

    val uiState: StateFlow<MemberDetailUiState> = combine(
        repository.observeMemberProfile(memberId),
        repository.allMembers,
        repository.allRelationships,
        preferencesRepository.preferences,
        sensitiveRevealed,
        exportMessage
    ) { arr ->
        val profile = arr[0] as MemberFullProfile?
        @Suppress("UNCHECKED_CAST")
        val allMembers = arr[1] as List<FamilyMemberEntity>
        @Suppress("UNCHECKED_CAST")
        val allRelationships = arr[2] as List<com.example.data.local.entity.RelationshipEntity>
        val prefs = arr[3] as com.example.data.preferences.UserPreferences
        val revealed = arr[4] as Boolean
        val msg = arr[5] as String?

        val memberMap = allMembers.associateBy { it.id }
        val groups = if (profile != null) {
            val orderedTypes = listOf(
                RelationshipType.FATHER,
                RelationshipType.MOTHER,
                RelationshipType.SPOUSE,
                RelationshipType.CHILD,
                RelationshipType.BROTHER,
                RelationshipType.SISTER,
                RelationshipType.GRANDPARENT,
                RelationshipType.OTHER_RELATIVE
            )
            orderedTypes.mapNotNull { type ->
                val related = profile.outgoingRelationships
                    .filter { it.type == type }
                    .mapNotNull { memberMap[it.toMemberId] }
                    .distinctBy { it.id }
                if (related.isEmpty()) null
                else ResolvedRelationshipGroup(
                    type = type,
                    label = when (type) {
                        RelationshipType.CHILD -> if (related.size > 1) "Children" else "Child"
                        RelationshipType.BROTHER -> if (related.size > 1) "Brothers" else "Brother"
                        RelationshipType.SISTER -> if (related.size > 1) "Sisters" else "Sister"
                        RelationshipType.GRANDPARENT -> if (related.size > 1) "Grandparents" else "Grandparent"
                        RelationshipType.OTHER_RELATIVE -> "Other Relatives"
                        else -> type.displayName
                    },
                    members = related
                )
            }
        } else emptyList()

        val ownerLabel = profile?.member?.let {
            RelationshipLabelResolver.resolveLabelToOwner(it, prefs.ownerMemberId, allRelationships)
        }

        MemberDetailUiState(
            isLoading = false,
            profile = profile,
            relationshipGroups = groups,
            ownerLabel = ownerLabel,
            hideSensitiveDetails = prefs.hideSensitiveOverview && !revealed,
            isSensitiveRevealed = revealed,
            exportMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MemberDetailUiState()
    )

    fun toggleRevealSensitive() {
        sensitiveRevealed.value = !sensitiveRevealed.value
    }

    fun togglePinned() {
        val current = uiState.value.profile?.member ?: return
        viewModelScope.launch {
            repository.togglePinned(current.id, !current.isPinned)
        }
    }

    fun toggleFavorite() {
        val current = uiState.value.profile?.member ?: return
        viewModelScope.launch {
            repository.toggleFavorite(current.id, !current.isFavorite)
        }
    }

    fun setAsFamilyOwner() {
        val current = uiState.value.profile?.member ?: return
        viewModelScope.launch {
            preferencesRepository.setOwnerMemberId(current.id)
            exportMessage.value = "${current.fullName} set as Family Owner"
        }
    }

    fun saveCustomField(existingId: Long, name: String, type: CustomFieldType, value: String) {
        if (name.isBlank() || memberId <= 0L) return
        viewModelScope.launch {
            repository.saveCustomField(
                CustomFieldEntity(
                    id = existingId,
                    memberId = memberId,
                    fieldName = name.trim(),
                    fieldType = type,
                    fieldValue = value.trim()
                )
            )
        }
    }

    fun deleteCustomField(fieldId: Long) {
        viewModelScope.launch { repository.deleteCustomField(fieldId) }
    }

    fun saveMemory(
        existingId: Long,
        category: MemoryCategory,
        title: String,
        description: String,
        dateIso: String?,
        attachmentUri: Uri?,
        existingAttachmentPath: String?
    ) {
        if (title.isBlank() || memberId <= 0L) return
        viewModelScope.launch {
            val savedPath = if (attachmentUri != null) {
                mediaStorageManager.saveDocumentFromUri(attachmentUri, prefix = "mem")
            } else {
                existingAttachmentPath
            }
            repository.saveMemory(
                MemoryEntity(
                    id = existingId,
                    memberId = memberId,
                    category = category,
                    title = title.trim(),
                    description = description.trim(),
                    dateIso = dateIso?.takeIf { it.isNotBlank() },
                    attachmentPath = savedPath
                )
            )
        }
    }

    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch { repository.deleteMemory(memory) }
    }

    fun addMemberGalleryPhoto(uri: Uri, caption: String, albumName: String) {
        if (memberId <= 0L) return
        viewModelScope.launch {
            val savedPath = mediaStorageManager.saveImageFromUri(uri) ?: return@launch
            repository.saveGalleryPhoto(
                GalleryPhotoEntity(
                    memberId = memberId,
                    albumName = albumName.ifBlank { "Personal Photos" },
                    filePath = savedPath,
                    caption = caption.trim(),
                    takenDateIso = DateUtils.toIsoDate(System.currentTimeMillis())
                )
            )
        }
    }

    fun deleteGalleryPhoto(photo: GalleryPhotoEntity) {
        viewModelScope.launch { repository.deleteGalleryPhoto(photo) }
    }

    fun deleteMember(deleteMedia: Boolean, onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteMember(memberId, deleteMedia)
            onDeleted()
        }
    }

    fun exportProfilePdf(onFileReady: (Uri, String) -> Unit) {
        val state = uiState.value
        val profile = state.profile ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val data = buildPdfData(profile, state.relationshipGroups, state.ownerLabel)
                    val file = PdfExportHelper.generateMemberProfilePdf(context, data)
                    mediaStorageManager.getUriForFile(file) to "application/pdf"
                }
            }
            result.onSuccess { (uri, mime) -> onFileReady(uri, mime) }
                .onFailure { exportMessage.value = "Failed to export PDF: ${it.message}" }
        }
    }

    fun exportProfileImage(onFileReady: (Uri, String) -> Unit) {
        val state = uiState.value
        val profile = state.profile ?: return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val data = buildPdfData(profile, state.relationshipGroups, state.ownerLabel)
                    val file = PdfExportHelper.generateMemberShareImage(context, data)
                    mediaStorageManager.getUriForFile(file) to "image/png"
                }
            }
            result.onSuccess { (uri, mime) -> onFileReady(uri, mime) }
                .onFailure { exportMessage.value = "Failed to export image: ${it.message}" }
        }
    }

    fun clearMessage() {
        exportMessage.value = null
    }

    private fun buildPdfData(
        profile: MemberFullProfile,
        groups: List<ResolvedRelationshipGroup>,
        ownerLabel: String?
    ): PdfExportHelper.MemberPdfData {
        val m = profile.member
        val dobText = m.dateOfBirth?.let { iso ->
            val formatted = DateUtils.formatDisplayDate(iso)
            val age = DateUtils.formatAgeLabel(iso)
            if (age != null) "$formatted ($age)" else formatted
        }
        val relLines = groups.map { grp ->
            "${grp.label}: ${grp.members.joinToString(", ") { it.fullName }}"
        }
        val infoLines = buildList {
            m.birthplace?.takeIf { it.isNotBlank() }?.let { add("Birthplace: $it") }
            m.currentAddress?.takeIf { it.isNotBlank() }?.let { add("Address: $it") }
            m.nationality?.takeIf { it.isNotBlank() }?.let { add("Nationality: $it") }
            m.occupation?.takeIf { it.isNotBlank() }?.let { add("Occupation: $it") }
            m.education?.takeIf { it.isNotBlank() }?.let { add("Education: $it") }
            m.phoneNumber?.takeIf { it.isNotBlank() }?.let { add("Phone: $it") }
            m.emailAddress?.takeIf { it.isNotBlank() }?.let { add("Email: $it") }
            m.personalWebsite?.takeIf { it.isNotBlank() }?.let { add("Website: $it") }
            m.importantNotes?.takeIf { it.isNotBlank() }?.let { add("Notes: $it") }
        }
        val cfLines = profile.customFields.map { cf ->
            val v = cf.fieldValue.ifBlank { "—" }
            "${cf.fieldName} (${cf.fieldType.displayName}): $v"
        }
        val memLines = profile.memories.map { mem ->
            val d = mem.dateIso?.let { " [${DateUtils.formatDisplayDate(it)}]" } ?: ""
            "${mem.category.displayName}: ${mem.title}$d" + if (mem.description.isNotBlank()) " — ${mem.description}" else ""
        }
        return PdfExportHelper.MemberPdfData(
            m.fullName,
            m.nickname,
            dobText,
            m.gender,
            m.bloodGroup,
            ownerLabel,
            m.biography,
            m.profilePhotoPath,
            relLines,
            infoLines,
            memLines,
            cfLines
        )
    }
}
