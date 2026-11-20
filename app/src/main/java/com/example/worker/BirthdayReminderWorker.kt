package com.example.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.local.dao.FamilyEventDao
import com.example.data.local.dao.FamilyMemberDao
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.DateUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@HiltWorker
class BirthdayReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val memberDao: FamilyMemberDao,
    private val eventDao: FamilyEventDao,
    private val preferencesRepository: UserPreferencesRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = preferencesRepository.preferences.first()
        if (!prefs.notificationsEnabled) return Result.success()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        val today = LocalDate.now()
        val leadDays = prefs.reminderDaysBefore.toLong()
        val alerts = mutableListOf<Pair<String, String>>()

        memberDao.getAllMembersOnce().forEach { member ->
            val days = DateUtils.daysUntilNextOccurrence(member.dateOfBirth, today)
            if (days != null && days <= leadDays) {
                val turning = DateUtils.nextTurningAge(member.dateOfBirth, today)
                val whenText = when (days) {
                    0L -> "today"
                    1L -> "tomorrow"
                    else -> "in $days days"
                }
                val ageText = if (turning != null && turning > 0) " (turning $turning)" else ""
                alerts.add("${member.fullName}'s Birthday" to "${member.fullName}'s birthday is $whenText$ageText.")
            }
        }

        eventDao.getAllOnce().filter { it.reminderEnabled }.forEach { event ->
            val days = if (event.isRecurringYearly) {
                DateUtils.daysUntilNextOccurrence(event.eventDateIso, today)
            } else {
                val eventDate = DateUtils.parseIsoDate(event.eventDateIso)
                if (eventDate != null && !eventDate.isBefore(today)) {
                    java.time.temporal.ChronoUnit.DAYS.between(today, eventDate)
                } else null
            }
            if (days != null && days <= leadDays) {
                val whenText = when (days) {
                    0L -> "today"
                    1L -> "tomorrow"
                    else -> "in $days days"
                }
                alerts.add(event.title to "${event.eventType.displayName} is $whenText.")
            }
        }

        if (alerts.isNotEmpty()) {
            val intent = Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                appContext,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val summaryTitle = if (alerts.size == 1) alerts.first().first else "${alerts.size} Upcoming Family Celebrations"
            val summaryBody = if (alerts.size == 1) alerts.first().second else alerts.take(3).joinToString(" • ") { it.first }

            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(summaryTitle)
                .setContentText(summaryBody)
                .setStyle(NotificationCompat.BigTextStyle().bigText(summaryBody))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
        }

        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "family_hub_reminders"
        const val WORK_NAME = "family_hub_daily_reminders"
        private const val NOTIFICATION_ID = 2042

        fun createChannel(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Birthdays & Family Events",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Local reminders for upcoming family birthdays, anniversaries, and milestones."
            }
            manager.createNotificationChannel(channel)
        }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BirthdayReminderWorker>(24, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
