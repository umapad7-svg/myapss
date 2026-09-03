package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.GeneratedSyllabus
import com.example.data.remote.GeneratedTopic
import com.example.data.remote.GeneratedUnit
import kotlin.math.roundToInt

val SampleSubjectSuggestions = listOf(
    "Data Structures & Algorithms",
    "Operating Systems",
    "Organic Chemistry",
    "Calculus II",
    "UPSC Indian Polity",
    "Machine Learning",
    "Microeconomics",
    "Molecular Biology",
    "World History"
)

val AcademicLevels = listOf(
    "College / University",
    "Competitive Exam",
    "High School / AP",
    "Certification"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AiSyllabusDialog(
    onDismiss: () -> Unit,
    onGenerate: (
        subject: String,
        academicLevel: String,
        examType: String,
        targetUnits: Int,
        customFocus: String,
        onResult: (GeneratedSyllabus?, String?) -> Unit
    ) -> Unit,
    onSaveToSyllabus: (
        syllabus: GeneratedSyllabus,
        daysUntilExam: Int?,
        daysUntilTarget: Int?
    ) -> Unit
) {
    val context = LocalContext.current
    var subjectTitle by remember { mutableStateOf("") }
    var academicLevel by remember { mutableStateOf(AcademicLevels[0]) }
    var examType by remember { mutableStateOf("Semester Final Exam") }
    var daysUntilExamStr by remember { mutableStateOf("30") }
    var targetUnits by remember { mutableFloatStateOf(4f) }
    var customFocus by remember { mutableStateOf("") }

    var isGenerating by remember { mutableStateOf(false) }
    var generationError by remember { mutableStateOf<String?>(null) }
    var previewSyllabus by remember { mutableStateOf<GeneratedSyllabus?>(null) }

    // Track excluded topic keys (unitIdx_topicIdx)
    val excludedTopics = remember { mutableStateListOf<String>() }

    Dialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .testTag("ai_syllabus_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (previewSyllabus == null) "AI Syllabus Generator" else "Review AI Syllabus",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (previewSyllabus == null) "Powered by Gemini AI" else "Check topics before saving",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isGenerating,
                        modifier = Modifier.testTag("close_ai_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Body content: Form or Preview
                if (previewSyllabus == null) {
                    // FORM MODE
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Subject Title Input
                        OutlinedTextField(
                            value = subjectTitle,
                            onValueChange = { subjectTitle = it },
                            label = { Text("Subject or Exam Name *") },
                            placeholder = { Text("e.g., Computer Networks, AP Chemistry") },
                            leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_subject_input"),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Preset Suggestions
                        Text(
                            text = "Quick Suggestions:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SampleSubjectSuggestions.forEach { suggestion ->
                                FilterChip(
                                    selected = subjectTitle.equals(suggestion, ignoreCase = true),
                                    onClick = { subjectTitle = suggestion },
                                    label = { Text(suggestion, fontSize = 12.sp) },
                                    modifier = Modifier.testTag("suggestion_${suggestion.take(8)}")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Academic Level
                        Text(
                            text = "Academic Level:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            AcademicLevels.forEach { level ->
                                FilterChip(
                                    selected = academicLevel == level,
                                    onClick = { academicLevel = level },
                                    label = { Text(level, fontSize = 12.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Exam Name & Days Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = examType,
                                onValueChange = { examType = it },
                                label = { Text("Exam Name") },
                                modifier = Modifier.weight(1.3f),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = daysUntilExamStr,
                                onValueChange = { if (it.all { ch -> ch.isDigit() }) daysUntilExamStr = it },
                                label = { Text("Days Left") },
                                leadingIcon = { Icon(Icons.Default.Event, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Number of Chapters/Units Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Units / Chapters to Generate:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${targetUnits.roundToInt()} Units",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Slider(
                            value = targetUnits,
                            onValueChange = { targetUnits = it },
                            valueRange = 2f..6f,
                            steps = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Optional custom focus
                        OutlinedTextField(
                            value = customFocus,
                            onValueChange = { customFocus = it },
                            label = { Text("Custom Focus / Syllabus Notes (Optional)") },
                            placeholder = { Text("e.g., Focus heavily on Dynamic Programming, Graph Theory, and Memory Pointers") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        if (generationError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = generationError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (isGenerating) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                                    Column {
                                        Text(
                                            text = "Consulting Gemini AI...",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                        Text(
                                            text = "Designing structured syllabus, chapters & topics...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            enabled = !isGenerating
                        ) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (subjectTitle.isBlank()) {
                                    Toast.makeText(context, "Please enter a subject name", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isGenerating = true
                                generationError = null
                                onGenerate(
                                    subjectTitle.trim(),
                                    academicLevel,
                                    examType.trim().ifBlank { "Final Exam" },
                                    targetUnits.roundToInt(),
                                    customFocus.trim()
                                ) { generated, err ->
                                    isGenerating = false
                                    if (generated != null) {
                                        previewSyllabus = generated
                                        excludedTopics.clear()
                                    } else {
                                        generationError = err ?: "Failed to generate syllabus"
                                    }
                                }
                            },
                            enabled = !isGenerating && subjectTitle.isNotBlank(),
                            modifier = Modifier.testTag("btn_generate_ai_syllabus")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate Syllabus")
                        }
                    }

                } else {
                    // PREVIEW MODE
                    val syllabus = previewSyllabus!!
                    val totalTopics = syllabus.units.sumOf { it.topics.size }
                    val includedTopicsCount = syllabus.units.mapIndexed { uIdx, unit ->
                        unit.topics.filterIndexed { tIdx, _ -> !excludedTopics.contains("${uIdx}_$tIdx") }.size
                    }.sum()
                    val totalHours = syllabus.units.mapIndexed { uIdx, unit ->
                        unit.topics.filterIndexed { tIdx, _ -> !excludedTopics.contains("${uIdx}_$tIdx") }
                            .sumOf { it.estimatedHours.toDouble() }
                    }.sum()

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Summary Banner
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = runCatching { Color(android.graphics.Color.parseColor(syllabus.suggestedColorHex)) }.getOrDefault(MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text(
                                                text = syllabus.subjectCode,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = syllabus.subjectName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${syllabus.examName} • ${syllabus.units.size} Units",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$includedTopicsCount topics",
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "%.1fh study".format(totalHours),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Generated Units & Topics (select topics to include):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Units List
                        syllabus.units.forEachIndexed { unitIdx, unit ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "${unit.unitNumber}",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = unit.name,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    unit.topics.forEachIndexed { topicIdx, topic ->
                                        val key = "${unitIdx}_$topicIdx"
                                        val isChecked = !excludedTopics.contains(key)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isChecked) excludedTopics.add(key) else excludedTopics.remove(key)
                                                }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    if (checked) excludedTopics.remove(key) else excludedTopics.add(key)
                                                },
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = topic.title,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = if (isChecked) FontWeight.Medium else FontWeight.Normal
                                                        ),
                                                        color = if (isChecked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                    )
                                                    Text(
                                                        text = "${topic.estimatedHours}h",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                if (topic.subtopics.isNotEmpty()) {
                                                    Text(
                                                        text = topic.subtopics.joinToString(" • "),
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Review Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { previewSyllabus = null },
                            modifier = Modifier.testTag("btn_back_to_prompt")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Prompt")
                        }

                        Button(
                            onClick = {
                                // Filter out excluded topics
                                val filteredUnits = syllabus.units.mapIndexedNotNull { uIdx, unit ->
                                    val keptTopics = unit.topics.filterIndexed { tIdx, _ ->
                                        !excludedTopics.contains("${uIdx}_$tIdx")
                                    }
                                    if (keptTopics.isNotEmpty()) {
                                        unit.copy(topics = keptTopics)
                                    } else null
                                }

                                if (filteredUnits.isEmpty()) {
                                    Toast.makeText(context, "Please include at least one topic", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val finalSyllabus = syllabus.copy(units = filteredUnits)
                                val daysLeft = daysUntilExamStr.toIntOrNull()
                                val daysTarget = daysLeft?.let { (it * 0.85).toInt().coerceAtLeast(1) }
                                onSaveToSyllabus(finalSyllabus, daysLeft, daysTarget)
                                onDismiss()
                            },
                            modifier = Modifier.testTag("btn_save_ai_syllabus")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to My Syllabus")
                        }
                    }
                }
            }
        }
    }
}
