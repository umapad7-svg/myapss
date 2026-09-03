package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.SubjectEntity
import com.example.ui.components.ExamCountdownCard
import com.example.ui.components.MetricTile
import com.example.ui.components.RadialProgressGauge
import com.example.ui.components.StreakBanner
import com.example.ui.components.SubjectCard
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusInProgress
import com.example.ui.theme.StatusNotStarted
import com.example.ui.viewmodel.SyllabusUiState
import com.example.ui.viewmodel.SyllabusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: SyllabusUiState,
    viewModel: SyllabusViewModel,
    onNavigateToSyllabus: (Long?) -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onOpenAddSubject: () -> Unit,
    onOpenCloudSync: () -> Unit,
    onOpenAiGenerator: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Progress Card: Animated Radial Gauge
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("overall_progress_hero_card"),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Syllabus Overview",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Real-time track of your academic milestones",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = onOpenCloudSync,
                            modifier = Modifier.testTag("cloud_sync_header_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Cloud Sync",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    RadialProgressGauge(
                        percentage = state.overallCompletionPercentage,
                        size = 170.dp,
                        title = "Completed"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3-column quick overview breakdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.overallCompletedTopics}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StatusCompleted
                                )
                            )
                            Text(
                                text = "Completed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.overallInProgressTopics}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StatusInProgress
                                )
                            )
                            Text(
                                text = "In Progress",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${state.overallNotStartedTopics}",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = StatusNotStarted
                                )
                            )
                            Text(
                                text = "Remaining",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Streak Banner
        item {
            StreakBanner(
                streakDays = state.currentStreak,
                onViewCalendar = onNavigateToCalendar
            )
        }

        // Nearest Exam Alert Countdown if available
        if (state.nearestExamSubject != null && state.daysLeftNearestExam != null) {
            item {
                val stats = state.subjectStats.find { it.subject.id == state.nearestExamSubject.id }
                ExamCountdownCard(
                    subject = state.nearestExamSubject,
                    remainingTopics = stats?.remainingTopics ?: 0
                )
            }
        }

        // Quick Metrics Row (Total Subjects, Study Goals, Revision Reminders)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    title = "Subjects",
                    value = "${state.subjects.size}",
                    icon = Icons.Default.School,
                    iconColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    title = "Total Topics",
                    value = "${state.overallTotalTopics}",
                    icon = Icons.Default.MenuBook,
                    iconColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Subjects Progress Header with AI Quick Action
        item {
            Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_ai_syllabus_banner"),
                onClick = onOpenAiGenerator
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add Any Subject with AI",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Auto-build complete exam syllabus with Gemini",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = onOpenAiGenerator,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Try AI", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Subjects Progress Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subjects & Progress",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = { onNavigateToSyllabus(null) },
                    modifier = Modifier.testTag("view_all_subjects_btn")
                ) {
                    Text("View Full Syllabus")
                }
            }
        }

        // Subject list cards
        if (state.subjectStats.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "No subjects added yet",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Add manually or generate any exam syllabus using Gemini AI",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(onClick = onOpenAddSubject) {
                                Text("Add Manually")
                            }
                            Button(onClick = onOpenAiGenerator) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add with AI")
                            }
                        }
                    }
                }
            }
        } else {
            items(state.subjectStats, key = { it.subject.id }) { stats ->
                SubjectCard(
                    stats = stats,
                    onClick = { onNavigateToSyllabus(stats.subject.id) },
                    onAddChapter = { onNavigateToSyllabus(stats.subject.id) },
                    onAddTopic = { onNavigateToSyllabus(stats.subject.id) },
                    onEdit = { /* Handled in full syllabus */ },
                    onDelete = { viewModel.deleteSubject(stats.subject.id) }
                )
            }
        }
    }
}
