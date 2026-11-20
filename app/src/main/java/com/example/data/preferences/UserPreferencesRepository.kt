package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "family_hub_prefs")

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = false,
    val reminderDaysBefore: Int = 1,
    val hideSensitiveOverview: Boolean = false,
    val includePrivateNotesInBackup: Boolean = true,
    val ownerMemberId: Long? = null
)

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val REMINDER_DAYS_BEFORE = intPreferencesKey("reminder_days_before")
        val HIDE_SENSITIVE_OVERVIEW = booleanPreferencesKey("hide_sensitive_overview")
        val INCLUDE_PRIVATE_NOTES_BACKUP = booleanPreferencesKey("include_private_notes_backup")
        val OWNER_MEMBER_ID = longPreferencesKey("owner_member_id")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = prefs[Keys.THEME_MODE]?.let { name ->
                runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM)
            } ?: ThemeMode.SYSTEM,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: false,
            reminderDaysBefore = prefs[Keys.REMINDER_DAYS_BEFORE] ?: 1,
            hideSensitiveOverview = prefs[Keys.HIDE_SENSITIVE_OVERVIEW] ?: false,
            includePrivateNotesInBackup = prefs[Keys.INCLUDE_PRIVATE_NOTES_BACKUP] ?: true,
            ownerMemberId = prefs[Keys.OWNER_MEMBER_ID]?.takeIf { it > 0L }
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setReminderDaysBefore(days: Int) {
        context.dataStore.edit { it[Keys.REMINDER_DAYS_BEFORE] = days.coerceIn(0, 14) }
    }

    suspend fun setHideSensitiveOverview(hide: Boolean) {
        context.dataStore.edit { it[Keys.HIDE_SENSITIVE_OVERVIEW] = hide }
    }

    suspend fun setIncludePrivateNotesInBackup(include: Boolean) {
        context.dataStore.edit { it[Keys.INCLUDE_PRIVATE_NOTES_BACKUP] = include }
    }

    suspend fun setOwnerMemberId(memberId: Long?) {
        context.dataStore.edit {
            if (memberId == null || memberId <= 0L) {
                it.remove(Keys.OWNER_MEMBER_ID)
            } else {
                it[Keys.OWNER_MEMBER_ID] = memberId
            }
        }
    }
}
