package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.RoundedProgressBar
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusInProgress
import com.example.ui.theme.StatusNotStarted
import com.example.ui.viewmodel.SyllabusUiState

@Composable
fun AnalyticsScreen(
    state: SyllabusUiState
) {
    val total = state.overallTotalTopics
    val completed = state.overallCompletedTopics
    val inProgress = state.overallInProgressTopics
    val notStarted = state.overallNotStartedTopics

    val completedAngle = if (total > 0) (completed.toFloat() / total) * 360f else 0f
    val inProgressAngle = if (total > 0) (inProgress.toFloat() / total) * 360f else 0f
    val notStartedAngle = if (total > 0) (notStarted.toFloat() / total) * 360f else 0f

    // Projected syllabus completion date calculation
    // Calculate 7-day average pace from activities
    val recentActivities = state.activities.take(7)
    val avgTopicsPerDay = if (recentActivities.isNotEmpty()) {
        recentActivities.sumOf { it.topicsCompletedCount }.toFloat() / recentActivities.size
    } else 1.2f
    val remainingTopics = state.overallNotStartedTopics + (state.overallInProgressTopics / 2)
    val estimatedDaysNeeded = if (avgTopicsPerDay > 0) (remainingTopics / avgTopicsPerDay).toInt() else 30

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Study Velocity Hero
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "STUDY VELOCITY & PACE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = String.format("%.1f topics/day average pace", avgTopicsPerDay),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "At your current pace, the remaining syllabus ($remainingTopics topics) will be completed in approximately $estimatedDaysNeeded days.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Status Breakdown Donut Chart
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DonutLarge,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Status Distribution",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Custom Canvas Donut Chart
                        Box(
                            modifier = Modifier.size(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(130.dp)) {
                                val strokeWidth = 24.dp.toPx()
                                val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                                val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)

                                var currentStart = -90f
                                if (total == 0) {
                                    drawArc(
                                        color = Color.LightGray.copy(alpha = 0.3f),
                                        startAngle = 0f,
                                        sweepAngle = 360f,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(width = strokeWidth)
                                    )
                                } else {
                                    if (completedAngle > 0) {
                                        drawArc(
                                            color = StatusCompleted,
                                            startAngle = currentStart,
                                            sweepAngle = completedAngle,
                                            useCenter = false,
                                            topLeft = topLeft,
                                            size = arcSize,
                                            style = Stroke(width = strokeWidth)
                                        )
                                        currentStart += completedAngle
                                    }
                                    if (inProgressAngle > 0) {
                                        drawArc(
                                            color = StatusInProgress,
                                            startAngle = currentStart,
                                            sweepAngle = inProgressAngle,
                                            useCenter = false,
                                            topLeft = topLeft,
                                            size = arcSize,
                                            style = Stroke(width = strokeWidth)
                                        )
                                        currentStart += inProgressAngle
                                    }
                                    if (notStartedAngle > 0) {
                                        drawArc(
                                            color = StatusNotStarted,
                                            startAngle = currentStart,
                                            sweepAngle = notStartedAngle,
                                            useCenter = false,
                                            topLeft = topLeft,
                                            size = arcSize,
                                            style = Stroke(width = strokeWidth)
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$total",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Topics",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Legend with Counts & Percentages
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            LegendRow(
                                label = "Completed",
                                count = completed,
                                percentage = if (total > 0) (completed * 100 / total) else 0,
                                color = StatusCompleted
                            )
                            LegendRow(
                                label = "In Progress",
                                count = inProgress,
                                percentage = if (total > 0) (inProgress * 100 / total) else 0,
                                color = StatusInProgress
                            )
                            LegendRow(
                                label = "Not Started",
                                count = notStarted,
                                percentage = if (total > 0) (notStarted * 100 / total) else 0,
                                color = StatusNotStarted
                            )
                        }
                    }
                }
            }
        }

        // Subject Completion Comparison Bar Chart
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Subject Completion Comparison",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (state.subjectStats.isEmpty()) {
                        Text(
                            text = "No subjects available for comparison.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.subjectStats.forEach { stats ->
                            val parsedColor = try {
                                Color(android.graphics.Color.parseColor(stats.subject.colorHex))
                            } catch (_: Exception) {
                                MaterialTheme.colorScheme.primary
                            }

                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stats.subject.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${stats.completionPercentage.toInt()}% (${stats.completedTopics}/${stats.totalTopics})",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = parsedColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                RoundedProgressBar(
                                    progress = stats.completionPercentage / 100f,
                                    barColor = parsedColor,
                                    height = 10.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRow(
    label: String,
    count: Int,
    percentage: Int,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = "$label ($count)",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$percentage% of syllabus",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
