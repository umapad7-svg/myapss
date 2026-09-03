package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.TopicStatus
import com.example.ui.components.RoundedProgressBar
import com.example.ui.components.TopicCard
import com.example.ui.viewmodel.SortOption
import com.example.ui.viewmodel.SubjectWithStats
import com.example.ui.viewmodel.SyllabusUiState
import com.example.ui.viewmodel.SyllabusViewModel
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusScreen(
    state: SyllabusUiState,
    viewModel: SyllabusViewModel,
    onOpenAddSubject: () -> Unit,
    onOpenAddChapter: (Long) -> Unit,
    onOpenAddTopic: (Long, Long?) -> Unit,
    onOpenAddReminder: (TopicEntity) -> Unit,
    onOpenAiGenerator: () -> Unit = {}
) {
    // Keep track of collapsed chapters
    val collapsedChapters = remember { mutableStateMapOf<Long, Boolean>() }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Filter and sort subjects
    val filteredSubjects = remember(
        state.subjectStats,
        state.searchQuery,
        state.statusFilter,
        state.selectedSubjectId,
        state.sortOption
    ) {
        var list = state.subjectStats

        // Subject filter
        if (state.selectedSubjectId != null) {
            list = list.filter { it.subject.id == state.selectedSubjectId }
        }

        // Search query
        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.trim().lowercase()
            list = list.filter { stats ->
                val subMatch = stats.subject.name.lowercase().contains(query) ||
                        stats.subject.code.lowercase().contains(query)
                val matchingTopics = state.topics.filter { it.subjectId == stats.subject.id }
                    .any {
                        it.title.lowercase().contains(query) ||
                                it.subtopicsText.lowercase().contains(query)
                    }
                val matchingChapters = state.chapters.filter { it.subjectId == stats.subject.id }
                    .any { it.name.lowercase().contains(query) }
                subMatch || matchingTopics || matchingChapters
            }
        }

        // Sorting
        when (state.sortOption) {
            SortOption.NAME_ASC -> list.sortedBy { it.subject.name.lowercase() }
            SortOption.EXAM_DATE_ASC -> list.sortedWith(
                compareBy(
                    { it.subject.examDateEpochMillis == null },
                    { it.subject.examDateEpochMillis }
                )
            )
            SortOption.PROGRESS_DESC -> list.sortedByDescending { it.completionPercentage }
            SortOption.REMAINING_DESC -> list.sortedByDescending { it.remainingTopics }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("syllabus_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search bar
        item {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("syllabus_search_input"),
                placeholder = { Text("Search subjects, chapters, topics, subtopics...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search"
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }

        // AI Syllabus Banner Quick Access
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("syllabus_ai_banner"),
                onClick = onOpenAiGenerator
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Add Any Subject Syllabus with AI",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Auto-generates units, topics & study targets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = onOpenAiGenerator,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Use AI", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Filter chips and Sort row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status Filters",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Sort menu button
                    Box {
                        OutlinedButton(
                            onClick = { sortMenuExpanded = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("sort_menu_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(state.sortOption.displayName, maxLines = 1, style = MaterialTheme.typography.labelSmall)
                        }

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            SortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.displayName) },
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Status Filter Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = (state.statusFilter == null),
                        onClick = { viewModel.setStatusFilter(null) },
                        label = { Text("All Statuses") }
                    )
                    FilterChip(
                        selected = (state.statusFilter == TopicStatus.NOT_STARTED),
                        onClick = { viewModel.setStatusFilter(TopicStatus.NOT_STARTED) },
                        label = { Text("Not Started") }
                    )
                    FilterChip(
                        selected = (state.statusFilter == TopicStatus.IN_PROGRESS),
                        onClick = { viewModel.setStatusFilter(TopicStatus.IN_PROGRESS) },
                        label = { Text("In Progress") }
                    )
                    FilterChip(
                        selected = (state.statusFilter == TopicStatus.COMPLETED),
                        onClick = { viewModel.setStatusFilter(TopicStatus.COMPLETED) },
                        label = { Text("Completed") }
                    )
                }

                // Subject filter horizontal pills
                if (state.subjects.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = (state.selectedSubjectId == null),
                                onClick = { viewModel.setSelectedSubjectFilter(null) },
                                label = { Text("All Subjects (${state.subjects.size})") }
                            )
                        }
                        items(state.subjects) { sub ->
                            FilterChip(
                                selected = (state.selectedSubjectId == sub.id),
                                onClick = { viewModel.setSelectedSubjectFilter(sub.id) },
                                label = { Text(if (sub.code.isNotBlank()) sub.code else sub.name) }
                            )
                        }
                    }
                }
            }
        }

        // Subjects with Chapters & Topics Hierarchy
        if (filteredSubjects.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No subjects match your filters",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try clearing the search query or status filter",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredSubjects, key = { it.subject.id }) { stats ->
                val subject = stats.subject
                val parsedColor = try {
                    Color(android.graphics.Color.parseColor(subject.colorHex))
                } catch (_: Exception) {
                    MaterialTheme.colorScheme.primary
                }

                val subjectChapters = state.chapters.filter { it.subjectId == subject.id }
                val subjectTopics = state.topics.filter { it.subjectId == subject.id }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_section_${subject.id}"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Subject Header Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(parsedColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = subject.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (subject.examName != null && subject.examDateEpochMillis != null) {
                                        Text(
                                            text = "${subject.examName} • ${DateUtils.getDaysRemainingLabel(subject.examDateEpochMillis)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = parsedColor
                                        )
                                    }
                                }
                            }

                            // Add Chapter / Topic quick buttons
                            Row {
                                IconButton(
                                    onClick = { onOpenAddChapter(subject.id) },
                                    modifier = Modifier.testTag("add_chapter_btn_${subject.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Chapter",
                                        tint = parsedColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subject Progress Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${stats.completedTopics}/${stats.totalTopics} topics completed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${stats.completionPercentage.toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = parsedColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        RoundedProgressBar(
                            progress = stats.completionPercentage / 100f,
                            barColor = parsedColor,
                            height = 6.dp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Chapters & Topics Listing
                        if (subjectChapters.isEmpty()) {
                            // If no chapters defined, show unassigned topics or prompt to add chapter
                            val unassignedTopics = subjectTopics.filter { it.chapterId == 0L }
                            if (unassignedTopics.isEmpty()) {
                                OutlinedButton(
                                    onClick = { onOpenAddChapter(subject.id) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add First Chapter / Unit")
                                }
                            } else {
                                unassignedTopics.forEach { topic ->
                                    TopicCard(
                                        topic = topic,
                                        chapter = null,
                                        subject = subject,
                                        onToggleStatus = { viewModel.toggleTopicStatus(topic) },
                                        onAddReminder = { onOpenAddReminder(topic) },
                                        onDelete = { viewModel.deleteTopic(topic.id) }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        } else {
                            subjectChapters.forEach { chapter ->
                                val isCollapsed = collapsedChapters[chapter.id] == true
                                val chapterTopics = subjectTopics.filter { it.chapterId == chapter.id }
                                val filteredChapterTopics = if (state.statusFilter != null) {
                                    chapterTopics.filter { it.status == state.statusFilter }
                                } else {
                                    chapterTopics
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        // Chapter Accordion Header
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    collapsedChapters[chapter.id] = !isCollapsed
                                                },
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(
                                                    imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = chapter.name,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${chapterTopics.count { it.status == TopicStatus.COMPLETED }}/${chapterTopics.size}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                IconButton(
                                                    onClick = { onOpenAddTopic(subject.id, chapter.id) },
                                                    modifier = Modifier.size(32.dp).testTag("add_topic_to_chap_${chapter.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Add Topic",
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Animated topics container
                                        AnimatedVisibility(
                                            visible = !isCollapsed,
                                            enter = expandVertically(),
                                            exit = shrinkVertically()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(top = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (filteredChapterTopics.isEmpty()) {
                                                    Text(
                                                        text = if (chapterTopics.isEmpty()) "No topics yet. Tap + to add topics." else "No topics match status filter.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.padding(start = 24.dp, top = 4.dp, bottom = 4.dp)
                                                    )
                                                } else {
                                                    filteredChapterTopics.forEach { topic ->
                                                        TopicCard(
                                                            topic = topic,
                                                            chapter = chapter,
                                                            subject = subject,
                                                            onToggleStatus = { viewModel.toggleTopicStatus(topic) },
                                                            onAddReminder = { onOpenAddReminder(topic) },
                                                            onDelete = { viewModel.deleteTopic(topic.id) }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
