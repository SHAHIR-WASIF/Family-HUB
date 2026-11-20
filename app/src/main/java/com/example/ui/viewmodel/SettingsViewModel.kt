package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.backup.BackupManager
import com.example.data.backup.RestoreMode
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FamilyRepository
import com.example.ui.theme.ThemeMode
import com.example.worker.BirthdayReminderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val members: List<FamilyMemberEntity> = emptyList(),
    val totalMembers: Int = 0,
    val totalPhotos: Int = 0,
    val totalEvents: Int = 0,
    val isBusy: Boolean = false,
    val statusMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: FamilyRepository,
    private val backupManager: BackupManager,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val isBusy = MutableStateFlow(false)
    private val statusMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferences,
        repository.allMembers,
        repository.allGalleryPhotos,
        repository.allEvents,
        isBusy,
        statusMessage
    ) { arr ->
        val prefs = arr[0] as UserPreferences
        @Suppress("UNCHECKED_CAST")
        val members = arr[1] as List<FamilyMemberEntity>
        @Suppress("UNCHECKED_CAST")
        val photos = arr[2] as List<com.example.data.local.entity.GalleryPhotoEntity>
        @Suppress("UNCHECKED_CAST")
        val events = arr[3] as List<com.example.data.local.entity.FamilyEventEntity>
        val busy = arr[4] as Boolean
        val msg = arr[5] as String?

        SettingsUiState(
            preferences = prefs,
            members = members,
            totalMembers = members.size,
            totalPhotos = photos.size,
            totalEvents = events.size,
            isBusy = busy,
            statusMessage = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setNotificationsEnabled(enabled)
            if (enabled) {
                BirthdayReminderWorker.schedule(context)
            } else {
                BirthdayReminderWorker.cancel(context)
            }
        }
    }

    fun setReminderDaysBefore(days: Int) {
        viewModelScope.launch { preferencesRepository.setReminderDaysBefore(days) }
    }

    fun setHideSensitiveOverview(hide: Boolean) {
        viewModelScope.launch { preferencesRepository.setHideSensitiveOverview(hide) }
    }

    fun setIncludePrivateNotesInBackup(include: Boolean) {
        viewModelScope.launch { preferencesRepository.setIncludePrivateNotesInBackup(include) }
    }

    fun setOwnerMemberId(id: Long?) {
        viewModelScope.launch { preferencesRepository.setOwnerMemberId(id) }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            isBusy.value = true
            val includeNotes = uiState.value.preferences.includePrivateNotesInBackup
            val result = backupManager.exportToUri(uri, includePrivateNotes = includeNotes)
            isBusy.value = false
            result.onSuccess { count ->
                statusMessage.value = "Exported backup with $count family member(s)."
            }.onFailure { err ->
                statusMessage.value = "Export failed: ${err.message}"
            }
        }
    }

    fun importBackup(uri: Uri, mode: RestoreMode) {
        viewModelScope.launch {
            isBusy.value = true
            val result = backupManager.importFromUri(uri, mode)
            isBusy.value = false
            result.onSuccess { summary ->
                statusMessage.value = "Restored ${summary.membersRestored} members, ${summary.relationshipsRestored} links, ${summary.photosRestored} photos, ${summary.eventsRestored} events."
            }.onFailure { err ->
                statusMessage.value = "Import failed: ${err.message}"
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            isBusy.value = true
            repository.clearAllData()
            preferencesRepository.setOwnerMemberId(null)
            isBusy.value = false
            statusMessage.value = "All local family data has been permanently erased."
        }
    }

    fun clearStatusMessage() {
        statusMessage.value = null
    }
}
