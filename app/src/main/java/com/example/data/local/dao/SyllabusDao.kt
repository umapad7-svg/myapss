package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.RevisionReminderEntity
import com.example.data.local.entity.StudyActivityEntity
import com.example.data.local.entity.StudyGoalEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.TopicStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface SyllabusDao {

    // Subjects
    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getSubjectById(id: Long): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>): List<Long>

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: Long)

    // Chapters
    @Query("SELECT * FROM chapters ORDER BY unitNumber ASC, orderIndex ASC, id ASC")
    fun getAllChapters(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE subjectId = :subjectId ORDER BY unitNumber ASC, orderIndex ASC")
    fun getChaptersBySubject(subjectId: Long): Flow<List<ChapterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>): List<Long>

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)

    @Query("DELETE FROM chapters WHERE id = :id")
    suspend fun deleteChapterById(id: Long)

    @Query("DELETE FROM chapters WHERE subjectId = :subjectId")
    suspend fun deleteChaptersBySubjectId(subjectId: Long)

    // Topics
    @Query("SELECT * FROM topics ORDER BY orderIndex ASC, id ASC")
    fun getAllTopics(): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE subjectId = :subjectId ORDER BY orderIndex ASC, id ASC")
    fun getTopicsBySubject(subjectId: Long): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE chapterId = :chapterId ORDER BY orderIndex ASC, id ASC")
    fun getTopicsByChapter(chapterId: Long): Flow<List<TopicEntity>>

    @Query("SELECT * FROM topics WHERE id = :id")
    suspend fun getTopicById(id: Long): TopicEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: TopicEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopics(topics: List<TopicEntity>): List<Long>

    @Update
    suspend fun updateTopic(topic: TopicEntity)

    @Query("UPDATE topics SET status = :status, completionEpochMillis = :completedAt WHERE id = :id")
    suspend fun updateTopicStatus(id: Long, status: TopicStatus, completedAt: Long?)

    @Delete
    suspend fun deleteTopic(topic: TopicEntity)

    @Query("DELETE FROM topics WHERE id = :id")
    suspend fun deleteTopicById(id: Long)

    @Query("DELETE FROM topics WHERE chapterId = :chapterId")
    suspend fun deleteTopicsByChapterId(chapterId: Long)

    @Query("DELETE FROM topics WHERE subjectId = :subjectId")
    suspend fun deleteTopicsBySubjectId(subjectId: Long)

    // Goals
    @Query("SELECT * FROM study_goals ORDER BY endDateEpochMillis ASC")
    fun getAllGoals(): Flow<List<StudyGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: StudyGoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<StudyGoalEntity>): List<Long>

    @Update
    suspend fun updateGoal(goal: StudyGoalEntity)

    @Query("DELETE FROM study_goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)

    // Activities & Streaks
    @Query("SELECT * FROM study_activities ORDER BY timestamp DESC")
    fun getAllActivities(): Flow<List<StudyActivityEntity>>

    @Query("SELECT * FROM study_activities WHERE dateString = :dateString LIMIT 1")
    suspend fun getActivityByDate(dateString: String): StudyActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: StudyActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<StudyActivityEntity>): List<Long>

    // Revision Reminders
    @Query("SELECT * FROM revision_reminders ORDER BY reminderTimeEpochMillis ASC")
    fun getAllRevisionReminders(): Flow<List<RevisionReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: RevisionReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: RevisionReminderEntity)

    @Query("DELETE FROM revision_reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    // Clear all tables for cloud restore or reset
    @Query("DELETE FROM subjects")
    suspend fun clearSubjects()

    @Query("DELETE FROM chapters")
    suspend fun clearChapters()

    @Query("DELETE FROM topics")
    suspend fun clearTopics()

    @Query("DELETE FROM study_goals")
    suspend fun clearGoals()

    @Query("DELETE FROM study_activities")
    suspend fun clearActivities()

    @Query("DELETE FROM revision_reminders")
    suspend fun clearReminders()
}
