package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TopicStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}

enum class GoalType {
    DAILY,
    WEEKLY,
    MONTHLY
}

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String = "",
    val colorHex: String = "#4F46E5",
    val iconName: String = "menu_book",
    val targetDateEpochMillis: Long? = null,
    val examDateEpochMillis: Long? = null,
    val examName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chapters")
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val name: String,
    val unitNumber: Int = 1,
    val orderIndex: Int = 0
)

@Entity(tableName = "topics")
data class TopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Long,
    val subjectId: Long,
    val title: String,
    val subtopicsText: String = "",
    val status: TopicStatus = TopicStatus.NOT_STARTED,
    val completionEpochMillis: Long? = null,
    val targetDateEpochMillis: Long? = null,
    val isRevisionNeeded: Boolean = false,
    val revisionCount: Int = 0,
    val lastRevisionEpochMillis: Long? = null,
    val nextRevisionEpochMillis: Long? = null,
    val notes: String = "",
    val estimatedHours: Float = 1.0f,
    val orderIndex: Int = 0
)

@Entity(tableName = "study_goals")
data class StudyGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val goalType: GoalType = GoalType.DAILY,
    val targetTopicsCount: Int = 3,
    val completedTopicsCount: Int = 0,
    val targetHours: Float = 2.0f,
    val completedHours: Float = 0.0f,
    val startDateEpochMillis: Long = System.currentTimeMillis(),
    val endDateEpochMillis: Long = System.currentTimeMillis() + 86400000L,
    val isCompleted: Boolean = false
)

@Entity(tableName = "study_activities")
data class StudyActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String, // e.g. "2026-09-02"
    val topicsCompletedCount: Int = 1,
    val studyMinutes: Int = 30,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "revision_reminders")
data class RevisionReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val topicTitle: String,
    val subjectName: String,
    val reminderTimeEpochMillis: Long,
    val repeatIntervalDays: Int = 3,
    val isCompleted: Boolean = false
)
