package com.example.domain

import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object DateUtils {
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val displayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())
    private val shortMonthDayFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

    fun parseIsoDate(iso: String?): LocalDate? {
        if (iso.isNullOrBlank()) return null
        return runCatching { LocalDate.parse(iso.trim(), isoFormatter) }.getOrNull()
    }

    fun toIsoDate(millis: Long): String {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.of("UTC"))
            .toLocalDate()
            .format(isoFormatter)
    }

    fun isoToUtcMillis(iso: String?): Long? {
        val date = parseIsoDate(iso) ?: return null
        return date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    }

    fun formatDisplayDate(iso: String?): String {
        val date = parseIsoDate(iso) ?: return ""
        return date.format(displayFormatter)
    }

    fun formatMonthDay(iso: String?): String {
        val date = parseIsoDate(iso) ?: return ""
        return date.format(shortMonthDayFormatter)
    }

    fun calculateAgeYears(dobIso: String?, today: LocalDate = LocalDate.now()): Int? {
        val dob = parseIsoDate(dobIso) ?: return null
        if (dob.isAfter(today)) return null
        return Period.between(dob, today).years
    }

    fun formatAgeLabel(dobIso: String?, today: LocalDate = LocalDate.now()): String? {
        val dob = parseIsoDate(dobIso) ?: return null
        if (dob.isAfter(today)) return null
        val period = Period.between(dob, today)
        return when {
            period.years >= 1 -> "${period.years} yrs"
            period.months >= 1 -> "${period.months} mos"
            else -> "${period.days} days"
        }
    }

    fun extractBirthYear(dobIso: String?): String? {
        return parseIsoDate(dobIso)?.year?.toString()
    }

    fun daysUntilNextOccurrence(isoDate: String?, today: LocalDate = LocalDate.now()): Long? {
        val date = parseIsoDate(isoDate) ?: return null
        val thisYearOccurrence = runCatching {
            LocalDate.of(today.year, date.month, date.dayOfMonth)
        }.getOrElse {
            // Leap day fallback on non-leap years -> Feb 28
            LocalDate.of(today.year, date.month, 28)
        }
        val next = if (!thisYearOccurrence.isBefore(today)) {
            thisYearOccurrence
        } else {
            runCatching {
                LocalDate.of(today.year + 1, date.month, date.dayOfMonth)
            }.getOrElse {
                LocalDate.of(today.year + 1, date.month, 28)
            }
        }
        return ChronoUnit.DAYS.between(today, next)
    }

    fun nextTurningAge(dobIso: String?, today: LocalDate = LocalDate.now()): Int? {
        val dob = parseIsoDate(dobIso) ?: return null
        val days = daysUntilNextOccurrence(dobIso, today) ?: return null
        val nextOccurrenceDate = today.plusDays(days)
        val age = nextOccurrenceDate.year - dob.year
        return age.takeIf { it >= 0 }
    }

    fun formatTimestamp(millis: Long): String {
        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        val diff = ChronoUnit.DAYS.between(date, today)
        return when {
            diff == 0L -> "Today"
            diff == 1L -> "Yesterday"
            diff in 2L..6L -> "$diff days ago"
            else -> date.format(displayFormatter)
        }
    }

    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return true
        return Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$").matches(email.trim())
    }

    fun isValidWebsite(url: String): Boolean {
        if (url.isBlank()) return true
        val trimmed = url.trim()
        return trimmed.contains(".") && !trimmed.contains(" ")
    }
}
