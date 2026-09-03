package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.TopicEntity
import com.example.ui.components.AddChapterDialog
import com.example.ui.components.AddGoalDialog
import com.example.ui.components.AddReminderDialog
import com.example.ui.components.AddSubjectDialog
import com.example.ui.components.AddTopicDialog
import com.example.ui.components.AiSyllabusDialog
import com.example.ui.components.CloudSyncDialog
import com.example.ui.theme.AppThemeMode
import com.example.ui.viewmodel.SyllabusViewModel

enum class MainDestination(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, "nav_dashboard"),
    SYLLABUS("Syllabus", Icons.Default.MenuBook, "nav_syllabus"),
    GOALS("Goals", Icons.Default.TrackChanges, "nav_goals"),
    CALENDAR("Calendar", Icons.Default.CalendarMonth, "nav_calendar"),
    ANALYTICS("Analytics", Icons.Default.BarChart, "nav_analytics"),
    REVISION("Revision", Icons.Default.Psychology, "nav_revision")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: SyllabusViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var currentDestination by remember { mutableStateOf(MainDestination.DASHBOARD) }

    // Dialog flags
    var showAddSubjectDialog by remember { mutableStateOf(false) }
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var showCloudSyncDialog by remember { mutableStateOf(false) }
    var showAiSyllabusDialog by remember { mutableStateOf(false) }

    // Preselections for dialogs
    var preselectedSubjectIdForChapter by remember { mutableStateOf<Long?>(null) }
    var preselectedSubjectIdForTopic by remember { mutableStateOf<Long?>(null) }
    var preselectedChapterIdForTopic by remember { mutableStateOf<Long?>(null) }
    var preselectedTopicForReminder by remember { mutableStateOf<TopicEntity?>(null) }

    // Fab speed-dial menu
    var fabMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Syllabus Tracker",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                actions = {
                    // Theme toggle button
                    IconButton(
                        onClick = {
                            val nextMode = when (state.themeMode) {
                                AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
                                AppThemeMode.LIGHT -> AppThemeMode.DARK
                                AppThemeMode.DARK -> AppThemeMode.SYSTEM
                            }
                            viewModel.setThemeMode(nextMode)
                        },
                        modifier = Modifier.testTag("theme_toggle_btn")
                    ) {
                        Icon(
                            imageVector = when (state.themeMode) {
                                AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                AppThemeMode.LIGHT -> Icons.Default.LightMode
                                AppThemeMode.DARK -> Icons.Default.DarkMode
                            },
                            contentDescription = "Toggle Theme"
                        )
                    }

                    // AI Generator icon button
                    IconButton(
                        onClick = { showAiSyllabusDialog = true },
                        modifier = Modifier.testTag("open_ai_generator_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Add Syllabus with AI",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Cloud Sync icon
                    IconButton(
                        onClick = { showCloudSyncDialog = true },
                        modifier = Modifier.testTag("open_cloud_sync_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Cloud Sync & Backup",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                MainDestination.values().forEach { destination ->
                    val isSelected = currentDestination == destination
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = destination },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.title
                            )
                        },
                        label = {
                            Text(
                                text = destination.title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.testTag(destination.tag)
                    )
                }
            }
        },
        floatingActionButton = {
            Box {
                when (currentDestination) {
                    MainDestination.GOALS -> {
                        ExtendedFloatingActionButton(
                            onClick = { showAddGoalDialog = true },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("New Goal") },
                            modifier = Modifier.testTag("fab_add_goal")
                        )
                    }
                    MainDestination.REVISION -> {
                        ExtendedFloatingActionButton(
                            onClick = {
                                preselectedTopicForReminder = null
                                showAddReminderDialog = true
                            },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("New Reminder") },
                            modifier = Modifier.testTag("fab_add_reminder")
                        )
                    }
                    else -> {
                        // Dashboard, Syllabus, Calendar, Analytics: Speed dial for Subject / Chapter / Topic
                        FloatingActionButton(
                            onClick = { fabMenuExpanded = true },
                            modifier = Modifier.testTag("main_fab")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add options")
                        }

                        DropdownMenu(
                            expanded = fabMenuExpanded,
                            onDismissRequest = { fabMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Add Syllabus with AI",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    fabMenuExpanded = false
                                    showAiSyllabusDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Add Subject (Manual)") },
                                onClick = {
                                    fabMenuExpanded = false
                                    showAddSubjectDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.School, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Add Chapter / Unit") },
                                onClick = {
                                    fabMenuExpanded = false
                                    preselectedSubjectIdForChapter = state.subjects.firstOrNull()?.id
                                    showAddChapterDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Add Topic") },
                                onClick = {
                                    fabMenuExpanded = false
                                    preselectedSubjectIdForTopic = state.subjects.firstOrNull()?.id
                                    preselectedChapterIdForTopic = state.chapters.firstOrNull()?.id
                                    showAddTopicDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen_transition"
            ) { target ->
                when (target) {
                    MainDestination.DASHBOARD -> DashboardScreen(
                        state = state,
                        viewModel = viewModel,
                        onNavigateToSyllabus = { subjectId ->
                            viewModel.setSelectedSubjectFilter(subjectId)
                            currentDestination = MainDestination.SYLLABUS
                        },
                        onNavigateToGoals = { currentDestination = MainDestination.GOALS },
                        onNavigateToCalendar = { currentDestination = MainDestination.CALENDAR },
                        onOpenAddSubject = { showAddSubjectDialog = true },
                        onOpenCloudSync = { showCloudSyncDialog = true },
                        onOpenAiGenerator = { showAiSyllabusDialog = true }
                    )
                    MainDestination.SYLLABUS -> SyllabusScreen(
                        state = state,
                        viewModel = viewModel,
                        onOpenAddSubject = { showAddSubjectDialog = true },
                        onOpenAddChapter = { subjectId ->
                            preselectedSubjectIdForChapter = subjectId
                            showAddChapterDialog = true
                        },
                        onOpenAddTopic = { subjectId, chapterId ->
                            preselectedSubjectIdForTopic = subjectId
                            preselectedChapterIdForTopic = chapterId
                            showAddTopicDialog = true
                        },
                        onOpenAddReminder = { topic ->
                            preselectedTopicForReminder = topic
                            showAddReminderDialog = true
                        },
                        onOpenAiGenerator = { showAiSyllabusDialog = true }
                    )
                    MainDestination.GOALS -> GoalsScreen(
                        state = state,
                        viewModel = viewModel,
                        onOpenAddGoal = { showAddGoalDialog = true }
                    )
                    MainDestination.CALENDAR -> CalendarScreen(
                        state = state
                    )
                    MainDestination.ANALYTICS -> AnalyticsScreen(
                        state = state
                    )
                    MainDestination.REVISION -> RevisionScreen(
                        state = state,
                        viewModel = viewModel,
                        onOpenAddReminder = {
                            preselectedTopicForReminder = null
                            showAddReminderDialog = true
                        }
                    )
                }
            }
        }
    }

    // Modal Dialogs
    if (showAddSubjectDialog) {
        AddSubjectDialog(
            onDismiss = { showAddSubjectDialog = false },
            onConfirm = { name, code, colorHex, targetDate, examDate, examName ->
                viewModel.addSubject(name, code, colorHex, targetDate, examDate, examName)
                Toast.makeText(context, "Subject added!", Toast.LENGTH_SHORT).show()
            },
            onSwitchToAi = {
                showAddSubjectDialog = false
                showAiSyllabusDialog = true
            }
        )
    }

    if (showAddChapterDialog) {
        AddChapterDialog(
            subjects = state.subjects,
            preselectedSubjectId = preselectedSubjectIdForChapter,
            onDismiss = { showAddChapterDialog = false },
            onConfirm = { subjectId, name, unitNumber ->
                viewModel.addChapter(subjectId, name, unitNumber)
                Toast.makeText(context, "Chapter added!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddTopicDialog) {
        AddTopicDialog(
            subjects = state.subjects,
            chapters = state.chapters,
            preselectedSubjectId = preselectedSubjectIdForTopic,
            preselectedChapterId = preselectedChapterIdForTopic,
            onDismiss = { showAddTopicDialog = false },
            onConfirm = { chapterId, subjectId, title, subtopics, hours, targetDays ->
                val targetDate = targetDays?.let { System.currentTimeMillis() + (it * 86400000L) }
                viewModel.addTopic(chapterId, subjectId, title, subtopics, hours, targetDate)
                Toast.makeText(context, "Topic added!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { title, type, targetTopics, targetHours, daysSpan ->
                viewModel.addStudyGoal(title, type, targetTopics, targetHours, daysSpan)
                Toast.makeText(context, "Goal created!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showAddReminderDialog) {
        AddReminderDialog(
            topics = state.topics,
            preselectedTopic = preselectedTopicForReminder,
            subjects = state.subjects,
            onDismiss = { showAddReminderDialog = false },
            onConfirm = { topicId, topicTitle, subjectName, timeEpochMillis, intervalDays ->
                viewModel.addRevisionReminder(topicId, topicTitle, subjectName, timeEpochMillis, intervalDays)
                Toast.makeText(context, "Revision reminder scheduled!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showCloudSyncDialog) {
        CloudSyncDialog(
            isSyncing = state.isSyncing,
            lastSyncTime = state.lastSyncTime,
            onDismiss = { showCloudSyncDialog = false },
            onSyncNow = {
                viewModel.performCloudSync { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            },
            onExportJson = { callback ->
                viewModel.exportSyllabusJson { json ->
                    callback(json)
                }
            },
            onImportJson = { json ->
                viewModel.importSyllabusJson(json) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (showAiSyllabusDialog) {
        AiSyllabusDialog(
            onDismiss = { showAiSyllabusDialog = false },
            onGenerate = { subject, academicLevel, examType, targetUnits, customFocus, onResult ->
                viewModel.generateSyllabusWithAi(
                    subjectTitle = subject,
                    academicLevel = academicLevel,
                    examType = examType,
                    targetUnits = targetUnits,
                    customFocus = customFocus,
                    onResult = onResult
                )
            },
            onSaveToSyllabus = { syllabus, daysUntilExam, daysUntilTarget ->
                viewModel.saveGeneratedSyllabus(
                    syllabus = syllabus,
                    daysUntilExam = daysUntilExam,
                    daysUntilTarget = daysUntilTarget
                ) { newSubjectId ->
                    viewModel.setSelectedSubjectFilter(newSubjectId)
                    currentDestination = MainDestination.SYLLABUS
                    Toast.makeText(
                        context,
                        "Syllabus for '${syllabus.subjectName}' generated & added!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
    }
}
