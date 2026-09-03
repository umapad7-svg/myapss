package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.GoalType
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TopicEntity
import com.example.util.DateUtils
import java.util.Calendar

val SubjectColorPresets = listOf(
    "#4F46E5", // Indigo
    "#0D9488", // Teal
    "#F59E0B", // Amber
    "#EC4899", // Pink
    "#10B981", // Emerald
    "#8B5CF6", // Violet
    "#3B82F6", // Blue
    "#F97316"  // Orange
)

@Composable
fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, code: String, colorHex: String, targetDate: Long?, examDate: Long?, examName: String?) -> Unit,
    onSwitchToAi: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(SubjectColorPresets[0]) }
    var examName by remember { mutableStateOf("") }
    var daysUntilExamStr by remember { mutableStateOf("30") }
    var daysUntilTargetStr by remember { mutableStateOf("25") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Subject", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (onSwitchToAi != null) {
                    androidx.compose.material3.Card(
                        onClick = onSwitchToAi,
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("switch_to_ai_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Generate with AI?",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Creates chapters, topics & hours automatically",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Use AI →",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name (e.g. Physics)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_name_input")
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Course Code (optional, e.g. PHY-101)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_code_input")
                )

                Text("Theme Color", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SubjectColorPresets.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = hex == selectedColor
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .then(
                                    if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                        )
                    }
                }

                OutlinedTextField(
                    value = examName,
                    onValueChange = { examName = it },
                    label = { Text("Exam Name (e.g. Final Semester Exam)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = daysUntilExamStr,
                        onValueChange = { daysUntilExamStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Exam in (days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = daysUntilTargetStr,
                        onValueChange = { daysUntilTargetStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Target in (days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val now = System.currentTimeMillis()
                        val examDays = daysUntilExamStr.toIntOrNull()
                        val examDate = examDays?.let { now + (it * 86400000L) }
                        val targetDays = daysUntilTargetStr.toIntOrNull()
                        val targetDate = targetDays?.let { now + (it * 86400000L) }
                        onConfirm(name, code, selectedColor, targetDate, examDate, examName.ifBlank { null })
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_add_subject_btn")
            ) {
                Text("Add Subject")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChapterDialog(
    subjects: List<SubjectEntity>,
    preselectedSubjectId: Long?,
    onDismiss: () -> Unit,
    onConfirm: (subjectId: Long, name: String, unitNumber: Int) -> Unit
) {
    var selectedSubId by remember { mutableLongStateOf(preselectedSubjectId ?: (subjects.firstOrNull()?.id ?: 0L)) }
    var chapterName by remember { mutableStateOf("") }
    var unitNumberStr by remember { mutableStateOf("1") }
    var subjectMenuExpanded by remember { mutableStateOf(false) }

    val currentSubject = subjects.find { it.id == selectedSubId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Chapter / Unit", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = subjectMenuExpanded,
                    onExpandedChange = { subjectMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currentSubject?.name ?: "Select Subject",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = subjectMenuExpanded,
                        onDismissRequest = { subjectMenuExpanded = false }
                    ) {
                        subjects.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = {
                                    selectedSubId = s.id
                                    subjectMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = chapterName,
                    onValueChange = { chapterName = it },
                    label = { Text("Chapter / Unit Title (e.g. Unit 2: Kinematics)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("chapter_name_input")
                )

                OutlinedTextField(
                    value = unitNumberStr,
                    onValueChange = { unitNumberStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Unit / Chapter Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (chapterName.isNotBlank() && selectedSubId > 0) {
                        val unitNum = unitNumberStr.toIntOrNull() ?: 1
                        onConfirm(selectedSubId, chapterName, unitNum)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_add_chapter_btn")
            ) {
                Text("Add Chapter")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTopicDialog(
    subjects: List<SubjectEntity>,
    chapters: List<ChapterEntity>,
    preselectedSubjectId: Long?,
    preselectedChapterId: Long?,
    onDismiss: () -> Unit,
    onConfirm: (chapterId: Long, subjectId: Long, title: String, subtopics: String, hours: Float, targetDays: Int?) -> Unit
) {
    var selectedSubId by remember { mutableLongStateOf(preselectedSubjectId ?: (subjects.firstOrNull()?.id ?: 0L)) }
    val availableChapters = chapters.filter { it.subjectId == selectedSubId }
    var selectedChapId by remember {
        mutableLongStateOf(
            preselectedChapterId ?: (availableChapters.firstOrNull()?.id ?: 0L)
        )
    }

    var topicTitle by remember { mutableStateOf("") }
    var subtopicsText by remember { mutableStateOf("") }
    var estHoursStr by remember { mutableStateOf("2.0") }
    var targetDaysStr by remember { mutableStateOf("7") }

    var subjectExpanded by remember { mutableStateOf(false) }
    var chapterExpanded by remember { mutableStateOf(false) }

    val currentSubject = subjects.find { it.id == selectedSubId }
    val currentChapter = availableChapters.find { it.id == selectedChapId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Topic", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Subject Dropdown
                ExposedDropdownMenuBox(
                    expanded = subjectExpanded,
                    onExpandedChange = { subjectExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currentSubject?.name ?: "Select Subject",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Subject") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = subjectExpanded,
                        onDismissRequest = { subjectExpanded = false }
                    ) {
                        subjects.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = {
                                    selectedSubId = s.id
                                    val newChaps = chapters.filter { it.subjectId == s.id }
                                    selectedChapId = newChaps.firstOrNull()?.id ?: 0L
                                    subjectExpanded = false
                                }
                            )
                        }
                    }
                }

                // Chapter Dropdown
                ExposedDropdownMenuBox(
                    expanded = chapterExpanded,
                    onExpandedChange = { chapterExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currentChapter?.name ?: "Select Chapter",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Chapter / Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = chapterExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = chapterExpanded,
                        onDismissRequest = { chapterExpanded = false }
                    ) {
                        availableChapters.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = {
                                    selectedChapId = c.id
                                    chapterExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = topicTitle,
                    onValueChange = { topicTitle = it },
                    label = { Text("Topic Title (e.g. Newton's Laws of Motion)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("topic_title_input")
                )

                OutlinedTextField(
                    value = subtopicsText,
                    onValueChange = { subtopicsText = it },
                    label = { Text("Subtopics (comma-separated)") },
                    placeholder = { Text("Inertia, F=ma, Action-Reaction, Friction") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = estHoursStr,
                        onValueChange = { estHoursStr = it },
                        label = { Text("Est. Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = targetDaysStr,
                        onValueChange = { targetDaysStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Target in (days)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (topicTitle.isNotBlank() && selectedChapId > 0 && selectedSubId > 0) {
                        val hours = estHoursStr.toFloatOrNull() ?: 1.5f
                        val targetDays = targetDaysStr.toIntOrNull()
                        onConfirm(selectedChapId, selectedSubId, topicTitle, subtopicsText, hours, targetDays)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_add_topic_btn")
            ) {
                Text("Add Topic")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, type: GoalType, targetTopics: Int, targetHours: Float, daysSpan: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(GoalType.DAILY) }
    var targetTopicsStr by remember { mutableStateOf("3") }
    var targetHoursStr by remember { mutableStateOf("2.5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Study Goal", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GoalType.values().forEach { type ->
                        FilterChip(
                            selected = (selectedType == type),
                            onClick = {
                                selectedType = type
                                when (type) {
                                    GoalType.DAILY -> {
                                        targetTopicsStr = "3"
                                        targetHoursStr = "2.5"
                                    }
                                    GoalType.WEEKLY -> {
                                        targetTopicsStr = "12"
                                        targetHoursStr = "15.0"
                                    }
                                    GoalType.MONTHLY -> {
                                        targetTopicsStr = "35"
                                        targetHoursStr = "50.0"
                                    }
                                }
                            },
                            label = { Text(type.name) }
                        )
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Description (e.g. Master Calculus Units)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_title_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = targetTopicsStr,
                        onValueChange = { targetTopicsStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Target Topics") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = targetHoursStr,
                        onValueChange = { targetHoursStr = it },
                        label = { Text("Target Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val topics = targetTopicsStr.toIntOrNull() ?: 3
                        val hours = targetHoursStr.toFloatOrNull() ?: 2.5f
                        val days = when (selectedType) {
                            GoalType.DAILY -> 1
                            GoalType.WEEKLY -> 7
                            GoalType.MONTHLY -> 30
                        }
                        onConfirm(title, selectedType, topics, hours, days)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_add_goal_btn")
            ) {
                Text("Set Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CloudSyncDialog(
    isSyncing: Boolean,
    lastSyncTime: Long?,
    onDismiss: () -> Unit,
    onSyncNow: () -> Unit,
    onExportJson: ((String) -> Unit) -> Unit,
    onImportJson: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var jsonInputText by remember { mutableStateOf("") }
    var showImportField by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Cloud Sync & Device Transfer")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Keep your syllabus progress synchronized across devices, or backup and restore your complete study journey anytime.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Sync status card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Last Synced",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = DateUtils.formatDate(lastSyncTime),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = "Synced",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Button(
                    onClick = onSyncNow,
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth().testTag("sync_vault_btn")
                ) {
                    Icon(imageVector = Icons.Default.CloudSync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSyncing) "Syncing..." else "Sync to Cloud Now")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onExportJson { json ->
                                clipboardManager.setText(AnnotatedString(json))
                                Toast.makeText(context, "Syllabus JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("export_backup_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export JSON")
                    }

                    OutlinedButton(
                        onClick = { showImportField = !showImportField },
                        modifier = Modifier.weight(1f).testTag("import_backup_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import JSON")
                    }
                }

                if (showImportField) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = jsonInputText,
                            onValueChange = { jsonInputText = it },
                            label = { Text("Paste Syllabus JSON") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4
                        )
                        Button(
                            onClick = {
                                if (jsonInputText.isNotBlank()) {
                                    onImportJson(jsonInputText)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Restore Syllabus Data")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderDialog(
    topics: List<TopicEntity>,
    preselectedTopic: TopicEntity?,
    subjects: List<SubjectEntity>,
    onDismiss: () -> Unit,
    onConfirm: (topicId: Long, topicTitle: String, subjectName: String, timeEpochMillis: Long, intervalDays: Int) -> Unit
) {
    var selectedTopicId by remember {
        mutableLongStateOf(preselectedTopic?.id ?: (topics.firstOrNull()?.id ?: 0L))
    }
    var intervalDays by remember { mutableIntStateOf(3) }
    var topicMenuExpanded by remember { mutableStateOf(false) }

    val currentTopic = topics.find { it.id == selectedTopicId }
    val currentSubject = subjects.find { it.id == currentTopic?.subjectId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schedule Revision Reminder", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = topicMenuExpanded,
                    onExpandedChange = { topicMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currentTopic?.title ?: "Select Topic",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Topic to Revise") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = topicMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = topicMenuExpanded,
                        onDismissRequest = { topicMenuExpanded = false }
                    ) {
                        topics.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.title) },
                                onClick = {
                                    selectedTopicId = t.id
                                    topicMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Text("Spaced Repetition Interval", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 3, 7, 14).forEach { days ->
                        FilterChip(
                            selected = (intervalDays == days),
                            onClick = { intervalDays = days },
                            label = { Text("$days Days") }
                        )
                    }
                }

                val targetTime = System.currentTimeMillis() + (intervalDays * 86400000L)
                Text(
                    text = "Reminder set for: ${DateUtils.formatDate(targetTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentTopic != null) {
                        val reminderTime = System.currentTimeMillis() + (intervalDays * 86400000L)
                        onConfirm(
                            currentTopic.id,
                            currentTopic.title,
                            currentSubject?.name ?: "Syllabus Topic",
                            reminderTime,
                            intervalDays
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_add_reminder_btn")
            ) {
                Text("Schedule Reminder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

