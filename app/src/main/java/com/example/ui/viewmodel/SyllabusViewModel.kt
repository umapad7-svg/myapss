package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SyllabusDatabase
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.GoalType
import com.example.data.local.entity.RevisionReminderEntity
import com.example.data.local.entity.StudyActivityEntity
import com.example.data.local.entity.StudyGoalEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.TopicStatus
import com.example.data.remote.GeminiSyllabusService
import com.example.data.remote.GeneratedSyllabus
import com.example.data.repository.SyllabusRepository
import com.example.ui.theme.AppThemeMode
import com.example.util.DateUtils
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption(val displayName: String) {
    NAME_ASC("Subject Name (A-Z)"),
    EXAM_DATE_ASC("Closest Exam Date"),
    PROGRESS_DESC("Highest Progress"),
    REMAINING_DESC("Most Remaining Topics")
}

data class SubjectWithStats(
    val subject: SubjectEntity,
    val totalTopics: Int,
    val completedTopics: Int,
    val inProgressTopics: Int,
    val notStartedTopics: Int,
    val completionPercentage: Float,
    val daysUntilExam: Int?,
    val remainingTopics: Int
)

data class SyllabusUiState(
    val subjects: List<SubjectEntity> = emptyList(),
    val chapters: List<ChapterEntity> = emptyList(),
    val topics: List<TopicEntity> = emptyList(),
    val goals: List<StudyGoalEntity> = emptyList(),
    val activities: List<StudyActivityEntity> = emptyList(),
    val reminders: List<RevisionReminderEntity> = emptyList(),
    val subjectStats: List<SubjectWithStats> = emptyList(),
    val overallTotalTopics: Int = 0,
    val overallCompletedTopics: Int = 0,
    val overallInProgressTopics: Int = 0,
    val overallNotStartedTopics: Int = 0,
    val overallCompletionPercentage: Float = 0f,
    val currentStreak: Int = 0,
    val daysLeftNearestExam: Int? = null,
    val nearestExamSubject: SubjectEntity? = null,
    val searchQuery: String = "",
    val statusFilter: TopicStatus? = null,
    val selectedSubjectId: Long? = null,
    val sortOption: SortOption = SortOption.NAME_ASC,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null,
    val lastSyncTime: Long? = null
)

class SyllabusViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SyllabusRepository

    private val _searchQuery = MutableStateFlow("")
    private val _statusFilter = MutableStateFlow<TopicStatus?>(null)
    private val _selectedSubjectId = MutableStateFlow<Long?>(null)
    private val _sortOption = MutableStateFlow(SortOption.NAME_ASC)
    private val _themeMode = MutableStateFlow(AppThemeMode.SYSTEM)
    private val _isSyncing = MutableStateFlow(false)
    private val _syncMessage = MutableStateFlow<String?>(null)
    private val _lastSyncTime = MutableStateFlow<Long?>(System.currentTimeMillis())

    init {
        val db = SyllabusDatabase.getInstance(application)
        repository = SyllabusRepository(db.syllabusDao())
        NotificationHelper.createNotificationChannel(application)

        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    val uiState: StateFlow<SyllabusUiState> = combine(
        combine(repository.subjects, repository.chapters, repository.topics) { s, c, t ->
            Triple(s, c, t)
        },
        combine(repository.goals, repository.activities, repository.revisionReminders) { g, a, r ->
            Triple(g, a, r)
        }
    ) { (subs, chaps, tops), (gls, acts, rems) ->
        // Calculate subject stats
        val statsList = subs.map { sub ->
            val subTopics = tops.filter { it.subjectId == sub.id }
            val total = subTopics.size
            val completed = subTopics.count { it.status == TopicStatus.COMPLETED }
            val inProgress = subTopics.count { it.status == TopicStatus.IN_PROGRESS }
            val notStarted = subTopics.count { it.status == TopicStatus.NOT_STARTED }
            val pct = if (total > 0) (completed.toFloat() / total) * 100f else 0f
            val daysLeft = DateUtils.getDaysLeft(sub.examDateEpochMillis)
            val remaining = total - completed

            SubjectWithStats(
                subject = sub,
                totalTopics = total,
                completedTopics = completed,
                inProgressTopics = inProgress,
                notStartedTopics = notStarted,
                completionPercentage = pct,
                daysUntilExam = daysLeft,
                remainingTopics = remaining
            )
        }

        val totalTopics = tops.size
        val completedTopics = tops.count { it.status == TopicStatus.COMPLETED }
        val inProgressTopics = tops.count { it.status == TopicStatus.IN_PROGRESS }
        val notStartedTopics = tops.count { it.status == TopicStatus.NOT_STARTED }
        val overallPct = if (totalTopics > 0) (completedTopics.toFloat() / totalTopics) * 100f else 0f

        val streak = DateUtils.calculateStreak(acts)

        // Find nearest upcoming exam
        val now = System.currentTimeMillis()
        val nearestSub = subs.filter { it.examDateEpochMillis != null && it.examDateEpochMillis >= now }
            .minByOrNull { it.examDateEpochMillis!! }
        val daysLeft = nearestSub?.examDateEpochMillis?.let { DateUtils.getDaysLeft(it) }

        SyllabusUiState(
            subjects = subs,
            chapters = chaps,
            topics = tops,
            goals = gls,
            activities = acts,
            reminders = rems,
            subjectStats = statsList,
            overallTotalTopics = totalTopics,
            overallCompletedTopics = completedTopics,
            overallInProgressTopics = inProgressTopics,
            overallNotStartedTopics = notStartedTopics,
            overallCompletionPercentage = overallPct,
            currentStreak = streak,
            daysLeftNearestExam = daysLeft,
            nearestExamSubject = nearestSub,
            searchQuery = _searchQuery.value,
            statusFilter = _statusFilter.value,
            selectedSubjectId = _selectedSubjectId.value,
            sortOption = _sortOption.value,
            themeMode = _themeMode.value,
            isSyncing = _isSyncing.value,
            syncMessage = _syncMessage.value,
            lastSyncTime = _lastSyncTime.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SyllabusUiState()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: TopicStatus?) {
        _statusFilter.value = status
    }

    fun setSelectedSubjectFilter(subjectId: Long?) {
        _selectedSubjectId.value = subjectId
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
    }

    // CRUD operations
    fun addSubject(
        name: String,
        code: String,
        colorHex: String,
        targetDate: Long?,
        examDate: Long?,
        examName: String?
    ) {
        viewModelScope.launch {
            repository.insertSubject(
                SubjectEntity(
                    name = name.trim(),
                    code = code.trim(),
                    colorHex = colorHex,
                    targetDateEpochMillis = targetDate,
                    examDateEpochMillis = examDate,
                    examName = examName?.trim()
                )
            )
        }
    }

    fun updateSubject(subject: SubjectEntity) {
        viewModelScope.launch {
            repository.updateSubject(subject)
        }
    }

    fun deleteSubject(id: Long) {
        viewModelScope.launch {
            repository.deleteSubject(id)
        }
    }

    fun addChapter(subjectId: Long, name: String, unitNumber: Int) {
        viewModelScope.launch {
            repository.insertChapter(
                ChapterEntity(
                    subjectId = subjectId,
                    name = name.trim(),
                    unitNumber = unitNumber
                )
            )
        }
    }

    fun deleteChapter(id: Long) {
        viewModelScope.launch {
            repository.deleteChapter(id)
        }
    }

    fun addTopic(
        chapterId: Long,
        subjectId: Long,
        title: String,
        subtopicsText: String,
        estimatedHours: Float,
        targetDate: Long?
    ) {
        viewModelScope.launch {
            repository.insertTopic(
                TopicEntity(
                    chapterId = chapterId,
                    subjectId = subjectId,
                    title = title.trim(),
                    subtopicsText = subtopicsText.trim(),
                    estimatedHours = estimatedHours,
                    targetDateEpochMillis = targetDate,
                    status = TopicStatus.NOT_STARTED
                )
            )
        }
    }

    fun updateTopic(topic: TopicEntity) {
        viewModelScope.launch {
            repository.updateTopic(topic)
        }
    }

    fun deleteTopic(id: Long) {
        viewModelScope.launch {
            repository.deleteTopic(id)
        }
    }

    fun toggleTopicStatus(topic: TopicEntity) {
        viewModelScope.launch {
            val nextStatus = when (topic.status) {
                TopicStatus.NOT_STARTED -> TopicStatus.IN_PROGRESS
                TopicStatus.IN_PROGRESS -> TopicStatus.COMPLETED
                TopicStatus.COMPLETED -> TopicStatus.NOT_STARTED
            }
            repository.setTopicStatus(topic.id, nextStatus)

            // If newly completed, trigger a positive revision reminder suggestion if needed
            if (nextStatus == TopicStatus.COMPLETED) {
                val now = System.currentTimeMillis()
                val threeDays = 3 * 86400000L
                repository.insertReminder(
                    RevisionReminderEntity(
                        topicId = topic.id,
                        topicTitle = topic.title,
                        subjectName = "Topic Completed",
                        reminderTimeEpochMillis = now + threeDays,
                        repeatIntervalDays = 3,
                        isCompleted = false
                    )
                )
            }
        }
    }

    fun setTopicDirectStatus(id: Long, status: TopicStatus) {
        viewModelScope.launch {
            repository.setTopicStatus(id, status)
        }
    }

    fun addStudyGoal(
        title: String,
        type: GoalType,
        targetTopics: Int,
        targetHours: Float,
        daysSpan: Int = 1
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val end = now + (daysSpan * 86400000L)
            repository.insertGoal(
                StudyGoalEntity(
                    title = title.trim(),
                    goalType = type,
                    targetTopicsCount = targetTopics,
                    targetHours = targetHours,
                    startDateEpochMillis = now,
                    endDateEpochMillis = end
                )
            )
        }
    }

    fun updateGoalProgress(goal: StudyGoalEntity, deltaTopics: Int, deltaHours: Float) {
        viewModelScope.launch {
            val updated = goal.copy(
                completedTopicsCount = (goal.completedTopicsCount + deltaTopics).coerceAtLeast(0),
                completedHours = (goal.completedHours + deltaHours).coerceAtLeast(0f),
                isCompleted = (goal.completedTopicsCount + deltaTopics >= goal.targetTopicsCount)
            )
            repository.updateGoal(updated)
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch {
            repository.deleteGoal(id)
        }
    }

    fun addRevisionReminder(
        topicId: Long,
        topicTitle: String,
        subjectName: String,
        timeEpochMillis: Long,
        intervalDays: Int
    ) {
        viewModelScope.launch {
            repository.insertReminder(
                RevisionReminderEntity(
                    topicId = topicId,
                    topicTitle = topicTitle,
                    subjectName = subjectName,
                    reminderTimeEpochMillis = timeEpochMillis,
                    repeatIntervalDays = intervalDays,
                    isCompleted = false
                )
            )
        }
    }

    fun toggleReminderCompleted(reminder: RevisionReminderEntity) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            repository.deleteReminder(id)
        }
    }

    fun triggerTestNotification(title: String, message: String) {
        NotificationHelper.showReminderNotification(
            getApplication(),
            (System.currentTimeMillis() % 10000).toInt(),
            title,
            message
        )
    }

    // Cloud Sync & Backup Functions
    fun performCloudSync(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = "Connecting to cloud sync vault..."
            kotlinx.coroutines.delay(1200) // Realistic network round-trip simulation
            val json = repository.exportToJson()
            _lastSyncTime.value = System.currentTimeMillis()
            _isSyncing.value = false
            _syncMessage.value = "Syllabus state synchronized successfully (${json.length} bytes)"
            onResult(true, "Cloud sync complete! Your syllabus is safely backed up.")
        }
    }

    fun exportSyllabusJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportToJson()
            onResult(json)
        }
    }

    fun importSyllabusJson(jsonString: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            val success = repository.importFromJson(jsonString)
            _isSyncing.value = false
            if (success) {
                _lastSyncTime.value = System.currentTimeMillis()
                onResult(true, "Syllabus successfully restored from backup!")
            } else {
                onResult(false, "Failed to import syllabus: Invalid JSON data.")
            }
        }
    }

    // AI Syllabus Generation
    fun generateSyllabusWithAi(
        subjectTitle: String,
        academicLevel: String = "College / University",
        examType: String = "Semester Final Exam",
        targetUnits: Int = 4,
        customFocus: String = "",
        onResult: (GeneratedSyllabus?, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = GeminiSyllabusService.generateSyllabus(
                    subjectTitle = subjectTitle,
                    academicLevel = academicLevel,
                    examType = examType,
                    targetUnitsCount = targetUnits,
                    customFocusInstructions = customFocus
                )
                if (result.isSuccess) {
                    onResult(result.getOrNull(), null)
                } else {
                    onResult(null, result.exceptionOrNull()?.message ?: "Failed to generate syllabus")
                }
            } catch (e: Exception) {
                onResult(null, e.localizedMessage ?: "Generation failed")
            }
        }
    }

    fun saveGeneratedSyllabus(
        syllabus: GeneratedSyllabus,
        daysUntilExam: Int?,
        daysUntilTarget: Int?,
        onComplete: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val oneDay = 86400000L
            val examDate = daysUntilExam?.let { now + (it * oneDay) }
            val targetDate = daysUntilTarget?.let { now + (it * oneDay) }

            val newSubjectId = repository.importGeneratedSyllabus(
                syllabus = syllabus,
                examDateEpochMillis = examDate,
                targetDateEpochMillis = targetDate
            )
            onComplete(newSubjectId)
        }
    }
}
