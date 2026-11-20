package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.FamilyEventEntity
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FamilyRepository
import com.example.domain.DateUtils
import com.example.domain.FamilyTreeLayoutEngine
import com.example.domain.RelationshipLabelResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class UpcomingBirthdayItem(
    val member: FamilyMemberEntity,
    val daysUntil: Long,
    val turningAge: Int?,
    val displayDate: String
)

data class FamilyStatistics(
    val totalMembers: Int = 0,
    val generationCount: Int = 0,
    val totalPhotos: Int = 0,
    val totalEvents: Int = 0,
    val genderBreakdown: Map<String, Int> = emptyMap(),
    val averageAgeYears: Int? = null
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val totalMembers: Int = 0,
    val pinnedAndFavorites: List<FamilyMemberEntity> = emptyList(),
    val recentlyAdded: List<FamilyMemberEntity> = emptyList(),
    val recentlyUpdated: List<FamilyMemberEntity> = emptyList(),
    val recentlyViewed: List<FamilyMemberEntity> = emptyList(),
    val upcomingBirthdays: List<UpcomingBirthdayItem> = emptyList(),
    val upcomingEvents: List<Pair<FamilyEventEntity, Long>> = emptyList(),
    val statistics: FamilyStatistics = FamilyStatistics(),
    val ownerLabels: Map<Long, String> = emptyMap(),
    val preferences: UserPreferences = UserPreferences()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: FamilyRepository,
    preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.allMembers,
        repository.allRelationships,
        repository.recentlyAddedMembers,
        repository.recentlyUpdatedMembers,
        repository.recentlyViewedMembers,
        repository.pinnedAndFavoriteMembers,
        repository.allEvents,
        repository.allGalleryPhotos,
        preferencesRepository.preferences
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allMembers = values[0] as List<FamilyMemberEntity>
        @Suppress("UNCHECKED_CAST")
        val allRelationships = values[1] as List<com.example.data.local.entity.RelationshipEntity>
        @Suppress("UNCHECKED_CAST")
        val recentAdded = values[2] as List<FamilyMemberEntity>
        @Suppress("UNCHECKED_CAST")
        val recentUpdated = values[3] as List<FamilyMemberEntity>
        @Suppress("UNCHECKED_CAST")
        val recentViewed = values[4] as List<FamilyMemberEntity>
        @Suppress("UNCHECKED_CAST")
        val pinned = values[5] as List<FamilyMemberEntity>
        @Suppress("UNCHECKED_CAST")
        val events = values[6] as List<FamilyEventEntity>
        @Suppress("UNCHECKED_CAST")
        val photos = values[7] as List<com.example.data.local.entity.GalleryPhotoEntity>
        val prefs = values[8] as UserPreferences

        val birthdays = allMembers.mapNotNull { m ->
            val days = DateUtils.daysUntilNextOccurrence(m.dateOfBirth) ?: return@mapNotNull null
            UpcomingBirthdayItem(
                member = m,
                daysUntil = days,
                turningAge = DateUtils.nextTurningAge(m.dateOfBirth),
                displayDate = DateUtils.formatMonthDay(m.dateOfBirth)
            )
        }.sortedBy { it.daysUntil }.take(6)

        val upcomingEvents = events.mapNotNull { ev ->
            val days = if (ev.isRecurringYearly) {
                DateUtils.daysUntilNextOccurrence(ev.eventDateIso)
            } else {
                val parsed = DateUtils.parseIsoDate(ev.eventDateIso)
                val today = java.time.LocalDate.now()
                if (parsed != null && !parsed.isBefore(today)) {
                    java.time.temporal.ChronoUnit.DAYS.between(today, parsed)
                } else null
            } ?: return@mapNotNull null
            ev to days
        }.sortedBy { it.second }.take(5)

        val treeLayout = FamilyTreeLayoutEngine.buildLayout(allMembers, allRelationships, prefs.ownerMemberId)
        val ages = allMembers.mapNotNull { DateUtils.calculateAgeYears(it.dateOfBirth) }
        val genderCounts = allMembers
            .groupBy { it.gender?.takeIf { g -> g.isNotBlank() } ?: "Unspecified" }
            .mapValues { it.value.size }

        val labels = allMembers.mapNotNull { m ->
            RelationshipLabelResolver.resolveLabelToOwner(m, prefs.ownerMemberId, allRelationships)?.let { m.id to it }
        }.toMap()

        HomeUiState(
            isLoading = false,
            totalMembers = allMembers.size,
            pinnedAndFavorites = pinned,
            recentlyAdded = recentAdded.take(6),
            recentlyUpdated = recentUpdated.take(6),
            recentlyViewed = recentViewed.take(6),
            upcomingBirthdays = birthdays,
            upcomingEvents = upcomingEvents,
            statistics = FamilyStatistics(
                totalMembers = allMembers.size,
                generationCount = if (allMembers.isEmpty()) 0 else treeLayout.generations.size,
                totalPhotos = photos.size,
                totalEvents = events.size,
                genderBreakdown = genderCounts,
                averageAgeYears = if (ages.isNotEmpty()) ages.average().toInt() else null
            ),
            ownerLabels = labels,
            preferences = prefs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )
}
