package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CustomFieldEntity
import com.example.data.local.entity.CustomFieldType
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.media.MediaStorageManager
import com.example.data.repository.FamilyRepository
import com.example.data.repository.RelationshipDraft
import com.example.domain.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class CustomFieldDraft(
    val localKey: Long = System.nanoTime(),
    val fieldName: String = "",
    val fieldType: CustomFieldType = CustomFieldType.TEXT,
    val fieldValue: String = ""
)

data class AddEditMemberFormState(
    val memberId: Long = 0L,
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    // Basic Info
    val fullName: String = "",
    val profilePhotoPath: String? = null,
    val dateOfBirth: String = "",
    val gender: String = "",
    val nickname: String = "",
    val bloodGroup: String = "",
    val biography: String = "",
    // Relationships
    val relationshipToOwner: String = "",
    val generationOffset: Int = 0,
    val fatherId: Long? = null,
    val motherId: Long? = null,
    val spouseIds: List<Long> = emptyList(),
    val childIds: List<Long> = emptyList(),
    val brotherIds: List<Long> = emptyList(),
    val sisterIds: List<Long> = emptyList(),
    val grandparentIds: List<Long> = emptyList(),
    val otherRelativeIds: List<Long> = emptyList(),
    // Additional Info
    val birthplace: String = "",
    val currentAddress: String = "",
    val nationality: String = "",
    val occupation: String = "",
    val education: String = "",
    val phoneNumber: String = "",
    val emailAddress: String = "",
    val personalWebsite: String = "",
    val importantNotes: String = "",
    // Custom Fields
    val customFields: List<CustomFieldDraft> = emptyList(),
    // Validation & Duplicate Protection
    val nameError: String? = null,
    val emailError: String? = null,
    val websiteError: String? = null,
    val duplicateMatches: List<FamilyMemberEntity> = emptyList(),
    val allowDuplicateSave: Boolean = false,
    val originalCreatedAt: Long = System.currentTimeMillis()
)

data class AddEditScreenState(
    val form: AddEditMemberFormState = AddEditMemberFormState(),
    val candidateMembers: List<FamilyMemberEntity> = emptyList()
)

@HiltViewModel
class AddEditMemberViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FamilyRepository,
    private val mediaStorageManager: MediaStorageManager
) : ViewModel() {

    private val editingMemberId: Long = savedStateHandle.get<Long>("memberId") ?: -1L
    private val formState = MutableStateFlow(
        AddEditMemberFormState(
            memberId = if (editingMemberId > 0L) editingMemberId else 0L,
            isEditMode = editingMemberId > 0L,
            isLoading = editingMemberId > 0L
        )
    )

    val uiState: StateFlow<AddEditScreenState> = combine(
        formState,
        repository.allMembers
    ) { form, members ->
        AddEditScreenState(
            form = form,
            candidateMembers = members.filter { it.id != form.memberId }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AddEditScreenState()
    )

    init {
        if (editingMemberId > 0L) {
            loadMemberForEdit(editingMemberId)
        }
    }

    private fun loadMemberForEdit(id: Long) {
        viewModelScope.launch {
            val (member, draft, fields) = repository.getMemberDraftContext(id)
            if (member != null) {
                formState.update {
                    it.copy(
                        memberId = member.id,
                        isEditMode = true,
                        isLoading = false,
                        fullName = member.fullName,
                        profilePhotoPath = member.profilePhotoPath,
                        dateOfBirth = member.dateOfBirth.orEmpty(),
                        gender = member.gender.orEmpty(),
                        nickname = member.nickname.orEmpty(),
                        bloodGroup = member.bloodGroup.orEmpty(),
                        biography = member.biography.orEmpty(),
                        relationshipToOwner = member.relationshipToOwner.orEmpty(),
                        generationOffset = member.generationOffset,
                        fatherId = draft.fatherId,
                        motherId = draft.motherId,
                        spouseIds = draft.spouseIds,
                        childIds = draft.childIds,
                        brotherIds = draft.brotherIds,
                        sisterIds = draft.sisterIds,
                        grandparentIds = draft.grandparentIds,
                        otherRelativeIds = draft.otherRelativeIds,
                        birthplace = member.birthplace.orEmpty(),
                        currentAddress = member.currentAddress.orEmpty(),
                        nationality = member.nationality.orEmpty(),
                        occupation = member.occupation.orEmpty(),
                        education = member.education.orEmpty(),
                        phoneNumber = member.phoneNumber.orEmpty(),
                        emailAddress = member.emailAddress.orEmpty(),
                        personalWebsite = member.personalWebsite.orEmpty(),
                        importantNotes = member.importantNotes.orEmpty(),
                        customFields = fields.map { cf ->
                            CustomFieldDraft(
                                localKey = cf.id,
                                fieldName = cf.fieldName,
                                fieldType = cf.fieldType,
                                fieldValue = cf.fieldValue
                            )
                        },
                        originalCreatedAt = member.createdAt
                    )
                }
            } else {
                formState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun updateFullName(value: String) {
        formState.update {
            it.copy(
                fullName = value,
                nameError = null,
                allowDuplicateSave = false
            )
        }
        viewModelScope.launch {
            val exclude = formState.value.memberId.takeIf { it > 0L }
            val matches = repository.findPotentialDuplicates(value, exclude)
            formState.update { it.copy(duplicateMatches = matches) }
        }
    }

    fun updateNickname(v: String) = formState.update { it.copy(nickname = v) }
    fun updateDateOfBirth(v: String) = formState.update { it.copy(dateOfBirth = v) }
    fun updateGender(v: String) = formState.update { it.copy(gender = v) }
    fun updateBloodGroup(v: String) = formState.update { it.copy(bloodGroup = v) }
    fun updateBiography(v: String) = formState.update { it.copy(biography = v) }
    fun updateRelationshipToOwner(v: String) = formState.update { it.copy(relationshipToOwner = v) }
    fun updateGenerationOffset(v: Int) = formState.update { it.copy(generationOffset = v) }
    fun updateBirthplace(v: String) = formState.update { it.copy(birthplace = v) }
    fun updateCurrentAddress(v: String) = formState.update { it.copy(currentAddress = v) }
    fun updateNationality(v: String) = formState.update { it.copy(nationality = v) }
    fun updateOccupation(v: String) = formState.update { it.copy(occupation = v) }
    fun updateEducation(v: String) = formState.update { it.copy(education = v) }
    fun updatePhoneNumber(v: String) = formState.update { it.copy(phoneNumber = v) }
    fun updateEmailAddress(v: String) = formState.update { it.copy(emailAddress = v, emailError = null) }
    fun updatePersonalWebsite(v: String) = formState.update { it.copy(personalWebsite = v, websiteError = null) }
    fun updateImportantNotes(v: String) = formState.update { it.copy(importantNotes = v) }

    fun setFather(id: Long?) = formState.update { it.copy(fatherId = id) }
    fun setMother(id: Long?) = formState.update { it.copy(motherId = id) }
    fun setSpouses(ids: List<Long>) = formState.update { it.copy(spouseIds = ids) }
    fun setChildren(ids: List<Long>) = formState.update { it.copy(childIds = ids) }
    fun setBrothers(ids: List<Long>) = formState.update { it.copy(brotherIds = ids) }
    fun setSisters(ids: List<Long>) = formState.update { it.copy(sisterIds = ids) }
    fun setGrandparents(ids: List<Long>) = formState.update { it.copy(grandparentIds = ids) }
    fun setOtherRelatives(ids: List<Long>) = formState.update { it.copy(otherRelativeIds = ids) }

    fun addCustomField() {
        formState.update {
            it.copy(customFields = it.customFields + CustomFieldDraft())
        }
    }

    fun updateCustomField(localKey: Long, name: String, type: CustomFieldType, value: String) {
        formState.update { state ->
            state.copy(
                customFields = state.customFields.map {
                    if (it.localKey == localKey) it.copy(fieldName = name, fieldType = type, fieldValue = value)
                    else it
                }
            )
        }
    }

    fun removeCustomField(localKey: Long) {
        formState.update { state ->
            state.copy(customFields = state.customFields.filterNot { it.localKey == localKey })
        }
    }

    fun createTempCameraUri(): Pair<Uri, File> = mediaStorageManager.createTempCameraUri()

    fun setProfilePhotoFromUri(uri: Uri) {
        viewModelScope.launch {
            val savedPath = mediaStorageManager.saveImageFromUri(uri)
            if (savedPath != null) {
                formState.update { it.copy(profilePhotoPath = savedPath) }
            }
        }
    }

    fun clearProfilePhoto() {
        formState.update { it.copy(profilePhotoPath = null) }
    }

    fun confirmDuplicateAndSave(onSaved: (Long) -> Unit) {
        formState.update { it.copy(allowDuplicateSave = true) }
        saveMember(onSaved)
    }

    fun saveMember(onSaved: (Long) -> Unit) {
        val current = formState.value
        if (current.isSaving) return

        val trimmedName = current.fullName.trim()
        if (trimmedName.isEmpty()) {
            formState.update { it.copy(nameError = "Full Name is required") }
            return
        }
        if (!DateUtils.isValidEmail(current.emailAddress)) {
            formState.update { it.copy(emailError = "Please enter a valid email address") }
            return
        }
        if (!DateUtils.isValidWebsite(current.personalWebsite)) {
            formState.update { it.copy(websiteError = "Please enter a valid website URL") }
            return
        }

        viewModelScope.launch {
            // Check duplicate protection on save if not already acknowledged
            if (!current.allowDuplicateSave) {
                val exclude = current.memberId.takeIf { it > 0L }
                val duplicates = repository.findPotentialDuplicates(trimmedName, exclude)
                if (duplicates.isNotEmpty()) {
                    formState.update { it.copy(duplicateMatches = duplicates) }
                    return@launch
                }
            }

            formState.update { it.copy(isSaving = true) }
            val existing = if (current.memberId > 0L) repository.getMemberById(current.memberId) else null
            val member = FamilyMemberEntity(
                id = current.memberId,
                fullName = trimmedName,
                profilePhotoPath = current.profilePhotoPath,
                dateOfBirth = current.dateOfBirth.trim().takeIf { it.isNotEmpty() },
                gender = current.gender.trim().takeIf { it.isNotEmpty() },
                nickname = current.nickname.trim().takeIf { it.isNotEmpty() },
                bloodGroup = current.bloodGroup.trim().takeIf { it.isNotEmpty() },
                biography = current.biography.trim().takeIf { it.isNotEmpty() },
                relationshipToOwner = current.relationshipToOwner.trim().takeIf { it.isNotEmpty() },
                generationOffset = current.generationOffset,
                birthplace = current.birthplace.trim().takeIf { it.isNotEmpty() },
                currentAddress = current.currentAddress.trim().takeIf { it.isNotEmpty() },
                nationality = current.nationality.trim().takeIf { it.isNotEmpty() },
                occupation = current.occupation.trim().takeIf { it.isNotEmpty() },
                education = current.education.trim().takeIf { it.isNotEmpty() },
                phoneNumber = current.phoneNumber.trim().takeIf { it.isNotEmpty() },
                emailAddress = current.emailAddress.trim().takeIf { it.isNotEmpty() },
                personalWebsite = current.personalWebsite.trim().takeIf { it.isNotEmpty() },
                importantNotes = current.importantNotes.trim().takeIf { it.isNotEmpty() },
                isPinned = existing?.isPinned ?: false,
                isFavorite = existing?.isFavorite ?: false,
                lastViewedAt = existing?.lastViewedAt,
                createdAt = existing?.createdAt ?: current.originalCreatedAt,
                updatedAt = System.currentTimeMillis()
            )

            val relDraft = RelationshipDraft(
                fatherId = current.fatherId,
                motherId = current.motherId,
                spouseIds = current.spouseIds,
                childIds = current.childIds,
                brotherIds = current.brotherIds,
                sisterIds = current.sisterIds,
                grandparentIds = current.grandparentIds,
                otherRelativeIds = current.otherRelativeIds
            )

            val fields = current.customFields
                .filter { it.fieldName.isNotBlank() }
                .map {
                    CustomFieldEntity(
                        id = 0L,
                        memberId = current.memberId,
                        fieldName = it.fieldName.trim(),
                        fieldType = it.fieldType,
                        fieldValue = it.fieldValue.trim()
                    )
                }

            val savedId = repository.saveMemberWithDetails(member, relDraft, fields)
            formState.update { it.copy(isSaving = false) }
            onSaved(savedId)
        }
    }
}
