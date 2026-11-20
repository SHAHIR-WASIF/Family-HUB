package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.RelationshipEntity
import com.example.data.local.entity.RelationshipType
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FamilyRepository
import com.example.domain.FamilyTreeLayoutEngine
import com.example.domain.RelationshipLabelResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class MemberSortOrder(val label: String) {
    NAME_ASC("Name (A–Z)"),
    NAME_DESC("Name (Z–A)"),
    DOB_YOUNGEST("Age (Youngest)"),
    DOB_OLDEST("Age (Oldest)"),
    RECENTLY_UPDATED("Recently Updated"),
    RECENTLY_ADDED("Recently Added")
}

enum class RelationshipFilter(val label: String) {
    ALL("All"),
    PINNED("Pinned / Favorites"),
    HAS_PARENTS("Has Parents"),
    HAS_CHILDREN("Parents"),
    MARRIED("Married / Spouse"),
    SIBLINGS("Has Siblings")
}

data class MemberListEntry(
    val member: FamilyMemberEntity,
    val generation: Int,
    val relationshipBadge: String?
)

data class MembersUiState(
    val members: List<MemberListEntry> = emptyList(),
    val totalCount: Int = 0,
    val searchQuery: String = "",
    val selectedGender: String? = null,
    val selectedGeneration: Int? = null,
    val availableGenerations: List<Int> = emptyList(),
    val relationshipFilter: RelationshipFilter = RelationshipFilter.ALL,
    val sortOrder: MemberSortOrder = MemberSortOrder.NAME_ASC,
    val isGridView: Boolean = true
)

@HiltViewModel
class MembersViewModel @Inject constructor(
    repository: FamilyRepository,
    preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val selectedGender = MutableStateFlow<String?>(null)
    private val selectedGeneration = MutableStateFlow<Int?>(null)
    private val relationshipFilter = MutableStateFlow(RelationshipFilter.ALL)
    private val sortOrder = MutableStateFlow(MemberSortOrder.NAME_ASC)
    private val isGridView = MutableStateFlow(true)

    private val filterControls = combine<Any?, FilterParams>(
        searchQuery,
        selectedGender,
        selectedGeneration,
        relationshipFilter,
        sortOrder,
        isGridView
    ) { arr ->
        FilterParams(
            query = arr[0] as String,
            gender = arr[1] as String?,
            generation = arr[2] as Int?,
            relFilter = arr[3] as RelationshipFilter,
            sort = arr[4] as MemberSortOrder,
            grid = arr[5] as Boolean
        )
    }

    private data class FilterParams(
        val query: String,
        val gender: String?,
        val generation: Int?,
        val relFilter: RelationshipFilter,
        val sort: MemberSortOrder,
        val grid: Boolean
    )

    val uiState: StateFlow<MembersUiState> = combine(
        repository.allMembers,
        repository.allRelationships,
        preferencesRepository.preferences,
        filterControls
    ) { allMembers, relationships, prefs, filters ->
        val layout = FamilyTreeLayoutEngine.buildLayout(allMembers, relationships, prefs.ownerMemberId)
        val genById = layout.nodes.associate { it.member.id to it.generation }
        val relsByMember = relationships.groupBy { it.fromMemberId }

        val entries = allMembers.map { m ->
            MemberListEntry(
                member = m,
                generation = genById[m.id] ?: 0,
                relationshipBadge = RelationshipLabelResolver.resolveLabelToOwner(m, prefs.ownerMemberId, relationships)
            )
        }

        val filtered = entries.filter { entry ->
            val m = entry.member
            val matchesQuery = filters.query.isBlank() || listOfNotNull(
                m.fullName,
                m.nickname,
                m.occupation,
                m.birthplace,
                m.relationshipToOwner
            ).any { it.contains(filters.query.trim(), ignoreCase = true) }

            val matchesGender = filters.gender == null ||
                (filters.gender == "Unspecified" && m.gender.isNullOrBlank()) ||
                m.gender.equals(filters.gender, ignoreCase = true)

            val matchesGen = filters.generation == null || entry.generation == filters.generation

            val outgoing = relsByMember[m.id].orEmpty()
            val matchesRel = when (filters.relFilter) {
                RelationshipFilter.ALL -> true
                RelationshipFilter.PINNED -> m.isPinned || m.isFavorite
                RelationshipFilter.HAS_PARENTS -> outgoing.any { it.type == RelationshipType.FATHER || it.type == RelationshipType.MOTHER }
                RelationshipFilter.HAS_CHILDREN -> outgoing.any { it.type == RelationshipType.CHILD }
                RelationshipFilter.MARRIED -> outgoing.any { it.type == RelationshipType.SPOUSE }
                RelationshipFilter.SIBLINGS -> outgoing.any { it.type == RelationshipType.BROTHER || it.type == RelationshipType.SISTER }
            }

            matchesQuery && matchesGender && matchesGen && matchesRel
        }

        val sorted = when (filters.sort) {
            MemberSortOrder.NAME_ASC -> filtered.sortedBy { it.member.fullName.lowercase() }
            MemberSortOrder.NAME_DESC -> filtered.sortedByDescending { it.member.fullName.lowercase() }
            MemberSortOrder.DOB_YOUNGEST -> filtered.sortedByDescending { it.member.dateOfBirth ?: "0000-00-00" }
            MemberSortOrder.DOB_OLDEST -> filtered.sortedBy { it.member.dateOfBirth ?: "9999-12-31" }
            MemberSortOrder.RECENTLY_UPDATED -> filtered.sortedByDescending { it.member.updatedAt }
            MemberSortOrder.RECENTLY_ADDED -> filtered.sortedByDescending { it.member.createdAt }
        }

        MembersUiState(
            members = sorted,
            totalCount = allMembers.size,
            searchQuery = filters.query,
            selectedGender = filters.gender,
            selectedGeneration = filters.generation,
            availableGenerations = layout.generations,
            relationshipFilter = filters.relFilter,
            sortOrder = filters.sort,
            isGridView = filters.grid
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MembersUiState()
    )

    fun onSearchQueryChange(value: String) {
        searchQuery.value = value
    }

    fun onGenderFilterChange(gender: String?) {
        selectedGender.value = gender
    }

    fun onGenerationFilterChange(gen: Int?) {
        selectedGeneration.value = gen
    }

    fun onRelationshipFilterChange(filter: RelationshipFilter) {
        relationshipFilter.value = filter
    }

    fun onSortOrderChange(order: MemberSortOrder) {
        sortOrder.value = order
    }

    fun toggleLayoutMode() {
        isGridView.value = !isGridView.value
    }
}
