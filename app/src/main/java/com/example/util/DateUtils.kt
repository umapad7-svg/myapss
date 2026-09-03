package com.example.util

import com.example.data.local.entity.StudyActivityEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val standardDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    fun formatDate(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis <= 0) return "Not set"
        return standardDateFormat.format(Date(epochMillis))
    }

    fun formatShortDate(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis <= 0) return "N/A"
        return shortDateFormat.format(Date(epochMillis))
    }

    fun formatMonthYear(calendar: Calendar): String {
        return monthYearFormat.format(calendar.time)
    }

    fun getTodayIsoString(): String {
        return isoDateFormat.format(Date())
    }

    fun getDaysLeft(targetEpochMillis: Long?): Int? {
        if (targetEpochMillis == null || targetEpochMillis <= 0) return null
        val now = System.currentTimeMillis()
        val diffMillis = targetEpochMillis - now
        val diffDays = (diffMillis / (1000 * 60 * 60 * 24)).toInt()
        return if (diffDays < 0) diffDays else (diffDays + 1)
    }

    fun getDaysRemainingLabel(targetEpochMillis: Long?): String {
        val days = getDaysLeft(targetEpochMillis) ?: return "No target date"
        return when {
            days < 0 -> "${-days}d overdue"
            days == 0 -> "Due Today"
            days == 1 -> "1 day left"
            else -> "$days days left"
        }
    }

    fun calculateStreak(activities: List<StudyActivityEntity>): Int {
        if (activities.isEmpty()) return 0
        val activeDates = activities.map { it.dateString }.toSet()
        val cal = Calendar.getInstance()

        var streak = 0
        val todayStr = isoDateFormat.format(cal.time)

        // Check today
        if (activeDates.contains(todayStr)) {
            streak++
            cal.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            // If not today, check if yesterday was active
            cal.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = isoDateFormat.format(cal.time)
            if (!activeDates.contains(yesterdayStr)) {
                return 0
            }
        }

        // Count backwards consecutive days
        while (true) {
            val dateStr = isoDateFormat.format(cal.time)
            if (activeDates.contains(dateStr)) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return streak
    }

    fun getDaysInMonth(year: Int, month: Int): List<CalendarDay> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 is Sunday, 2 is Monday
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val days = mutableListOf<CalendarDay>()

        // Leading blanks (assuming week starts on Sunday: 1 -> 0 blanks, 2 -> 1 blank)
        val leadingBlanks = firstDayOfWeek - 1
        for (i in 0 until leadingBlanks) {
            days.add(CalendarDay(dayNumber = 0, isCurrentMonth = false, dateIso = ""))
        }

        val todayStr = getTodayIsoString()
        for (day in 1..maxDays) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val iso = isoDateFormat.format(cal.time)
            days.add(
                CalendarDay(
                    dayNumber = day,
                    isCurrentMonth = true,
                    dateIso = iso,
                    isToday = (iso == todayStr),
                    epochMillis = cal.timeInMillis
                )
            )
        }

        return days
    }
}

data class CalendarDay(
    val dayNumber: Int,
    val isCurrentMonth: Boolean,
    val dateIso: String,
    val isToday: Boolean = false,
    val epochMillis: Long = 0L
)
