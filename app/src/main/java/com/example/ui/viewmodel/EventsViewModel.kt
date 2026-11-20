package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.FamilyEventEntity
import com.example.data.local.entity.FamilyEventType
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FamilyRepository
import com.example.domain.DateUtils
import com.example.worker.BirthdayReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

data class EventRowItem(
    val event: FamilyEventEntity,
    val relatedMemberName: String?,
    val daysUntil: Long?,
    val displayDate: String
)

data class EventsUiState(
    val birthdays: List<UpcomingBirthdayItem> = emptyList(),
    val events: List<EventRowItem> = emptyList(),
    val members: List<FamilyMemberEntity> = emptyList(),
    val selectedMonth: Int = LocalDate.now().monthValue, // 1..12
    val notificationsEnabled: Boolean = false
)

@HiltViewModel
class EventsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: FamilyRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(LocalDate.now().monthValue)

    val uiState: StateFlow<EventsUiState> = combine(
        repository.allMembers,
        repository.allEvents,
        preferencesRepository.preferences,
        selectedMonth
    ) { members, events, prefs, month ->
        val memberMap = members.associateBy { it.id }

        val birthdays = members.mapNotNull { m ->
            val days = DateUtils.daysUntilNextOccurrence(m.dateOfBirth) ?: return@mapNotNull null
            UpcomingBirthdayItem(
                member = m,
                daysUntil = days,
                turningAge = DateUtils.nextTurningAge(m.dateOfBirth),
                displayDate = DateUtils.formatMonthDay(m.dateOfBirth)
            )
        }.sortedBy { it.daysUntil }

        val today = LocalDate.now()
        val eventRows = events.map { ev ->
            val days = if (ev.isRecurringYearly) {
                DateUtils.daysUntilNextOccurrence(ev.eventDateIso, today)
            } else {
                val parsed = DateUtils.parseIsoDate(ev.eventDateIso)
                if (parsed != null && !parsed.isBefore(today)) {
                    ChronoUnit.DAYS.between(today, parsed)
                } else null
            }
            EventRowItem(
                event = ev,
                relatedMemberName = ev.relatedMemberId?.let { memberMap[it]?.fullName },
                daysUntil = days,
                displayDate = DateUtils.formatDisplayDate(ev.eventDateIso)
            )
        }.sortedWith(compareBy<EventRowItem> { it.daysUntil ?: Long.MAX_VALUE }.thenBy { it.event.eventDateIso })

        EventsUiState(
            birthdays = birthdays,
            events = eventRows,
            members = members,
            selectedMonth = month,
            notificationsEnabled = prefs.notificationsEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EventsUiState()
    )

    fun selectMonth(month: Int) {
        selectedMonth.value = month.coerceIn(1, 12)
    }

    fun addEvent(
        title: String,
        description: String,
        dateIso: String,
        type: FamilyEventType,
        relatedMemberId: Long?,
        isRecurringYearly: Boolean,
        reminderEnabled: Boolean
    ) {
        if (title.isBlank() || dateIso.isBlank()) return
        viewModelScope.launch {
            repository.saveEvent(
                FamilyEventEntity(
                    title = title.trim(),
                    description = description.trim(),
                    eventDateIso = dateIso,
                    eventType = type,
                    relatedMemberId = relatedMemberId,
                    isRecurringYearly = isRecurringYearly,
                    reminderEnabled = reminderEnabled
                )
            )
        }
    }

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch { repository.deleteEvent(eventId) }
    }

    fun enableNotifications() {
        viewModelScope.launch {
            preferencesRepository.setNotificationsEnabled(true)
            BirthdayReminderWorker.schedule(context)
        }
    }
}
