package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.StreakBanner
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StreakFire
import com.example.ui.theme.UrgentDeadlineRed
import com.example.ui.viewmodel.SyllabusUiState
import com.example.util.CalendarDay
import com.example.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    state: SyllabusUiState
) {
    val currentCalendar = remember { Calendar.getInstance() }
    var displayedMonth by remember { mutableIntStateOf(currentCalendar.get(Calendar.MONTH)) }
    var displayedYear by remember { mutableIntStateOf(currentCalendar.get(Calendar.YEAR)) }

    val monthCal = remember(displayedMonth, displayedYear) {
        val c = Calendar.getInstance()
        c.set(Calendar.YEAR, displayedYear)
        c.set(Calendar.MONTH, displayedMonth)
        c.set(Calendar.DAY_OF_MONTH, 1)
        c
    }

    val daysInMonth = remember(displayedMonth, displayedYear) {
        DateUtils.getDaysInMonth(displayedYear, displayedMonth)
    }

    var selectedDay by remember {
        mutableStateOf(daysInMonth.find { it.isToday } ?: daysInMonth.firstOrNull { it.isCurrentMonth })
    }

    // Active dates set from StudyActivityEntity
    val activityMap = remember(state.activities) {
        state.activities.associateBy { it.dateString }
    }

    // Exam dates set
    val isoSdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val examDatesMap = remember(state.subjects) {
        state.subjects.filter { it.examDateEpochMillis != null }
            .groupBy { isoSdf.format(Date(it.examDateEpochMillis!!)) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Streak Status Hero
        item {
            StreakBanner(streakDays = state.currentStreak)
        }

        // Month Navigation Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header: Month Year and navigation arrows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = DateUtils.formatMonthYear(monthCal),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    if (displayedMonth == 0) {
                                        displayedMonth = 11
                                        displayedYear--
                                    } else {
                                        displayedMonth--
                                    }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                            }
                            IconButton(
                                onClick = {
                                    if (displayedMonth == 11) {
                                        displayedMonth = 0
                                        displayedYear++
                                    } else {
                                        displayedMonth++
                                    }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Day of Week Headers
                    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        dayNames.forEach { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(36.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar Month Matrix (Custom Row-based for smooth scrolling inside LazyColumn)
                    val rows = daysInMonth.chunked(7)
                    rows.forEach { week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            week.forEach { day ->
                                if (!day.isCurrentMonth) {
                                    Box(modifier = Modifier.size(40.dp))
                                } else {
                                    val isSelected = selectedDay?.dateIso == day.dateIso
                                    val hasActivity = activityMap.containsKey(day.dateIso)
                                    val hasExam = examDatesMap.containsKey(day.dateIso)

                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .clickable { selectedDay = day }
                                            .then(
                                                when {
                                                    isSelected -> Modifier.background(MaterialTheme.colorScheme.primary)
                                                    day.isToday -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                                    hasActivity -> Modifier.background(StatusCompleted.copy(alpha = 0.15f))
                                                    else -> Modifier
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${day.dayNumber}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (isSelected || day.isToday) FontWeight.Bold else FontWeight.Normal
                                                ),
                                                color = when {
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    hasExam -> UrgentDeadlineRed
                                                    hasActivity -> StatusCompleted
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                            // Dot indicators
                                            if (hasExam || hasActivity) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else if (hasExam) UrgentDeadlineRed else StatusCompleted)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            // Fill remaining space if last week has < 7 items
                            if (week.size < 7) {
                                for (i in 0 until (7 - week.size)) {
                                    Box(modifier = Modifier.size(40.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(StatusCompleted))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Study Activity", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(UrgentDeadlineRed))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exam Day", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Today", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Details for Selected Date
        item {
            val dateIso = selectedDay?.dateIso ?: DateUtils.getTodayIsoString()
            val activity = activityMap[dateIso]
            val exams = examDatesMap[dateIso] ?: emptyList()

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Schedule on $dateIso",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (exams.isNotEmpty()) {
                        exams.forEach { sub ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.School, contentDescription = null, tint = UrgentDeadlineRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${sub.name} (${sub.examName ?: "Exam"})",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = UrgentDeadlineRed
                                )
                            }
                        }
                    }

                    if (activity != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = StreakFire)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${activity.topicsCompletedCount} topics completed • ${activity.studyMinutes} mins logged",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else if (exams.isEmpty()) {
                        Text(
                            text = "No study logs recorded for this day.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
