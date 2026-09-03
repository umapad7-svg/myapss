package com.example.data.repository

import android.content.Context
import com.example.data.local.SyllabusDatabase
import com.example.data.local.dao.SyllabusDao
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.GoalType
import com.example.data.local.entity.RevisionReminderEntity
import com.example.data.local.entity.StudyActivityEntity
import com.example.data.local.entity.StudyGoalEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TopicEntity
import com.example.data.local.entity.TopicStatus
import com.example.data.remote.GeneratedSyllabus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SyllabusRepository(private val dao: SyllabusDao) {

    val subjects: Flow<List<SubjectEntity>> = dao.getAllSubjects()
    val chapters: Flow<List<ChapterEntity>> = dao.getAllChapters()
    val topics: Flow<List<TopicEntity>> = dao.getAllTopics()
    val goals: Flow<List<StudyGoalEntity>> = dao.getAllGoals()
    val activities: Flow<List<StudyActivityEntity>> = dao.getAllActivities()
    val revisionReminders: Flow<List<RevisionReminderEntity>> = dao.getAllRevisionReminders()

    suspend fun insertSubject(subject: SubjectEntity): Long = dao.insertSubject(subject)
    suspend fun updateSubject(subject: SubjectEntity) = dao.updateSubject(subject)
    suspend fun deleteSubject(id: Long) {
        dao.deleteTopicsBySubjectId(id)
        dao.deleteChaptersBySubjectId(id)
        dao.deleteSubjectById(id)
    }

    suspend fun insertChapter(chapter: ChapterEntity): Long = dao.insertChapter(chapter)
    suspend fun updateChapter(chapter: ChapterEntity) = dao.updateChapter(chapter)
    suspend fun deleteChapter(id: Long) {
        dao.deleteTopicsByChapterId(id)
        dao.deleteChapterById(id)
    }

    suspend fun insertTopic(topic: TopicEntity): Long = dao.insertTopic(topic)
    suspend fun updateTopic(topic: TopicEntity) = dao.updateTopic(topic)
    suspend fun deleteTopic(id: Long) = dao.deleteTopicById(id)

    suspend fun setTopicStatus(id: Long, status: TopicStatus) {
        val now = if (status == TopicStatus.COMPLETED) System.currentTimeMillis() else null
        dao.updateTopicStatus(id, status, now)
        if (status == TopicStatus.COMPLETED) {
            recordTodayActivity(topicsCount = 1, minutes = 25)
        }
    }

    suspend fun recordTodayActivity(topicsCount: Int = 1, minutes: Int = 25) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val existing = dao.getActivityByDate(todayStr)
        if (existing != null) {
            dao.insertActivity(
                existing.copy(
                    topicsCompletedCount = existing.topicsCompletedCount + topicsCount,
                    studyMinutes = existing.studyMinutes + minutes,
                    timestamp = System.currentTimeMillis()
                )
            )
        } else {
            dao.insertActivity(
                StudyActivityEntity(
                    dateString = todayStr,
                    topicsCompletedCount = topicsCount,
                    studyMinutes = minutes,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun insertGoal(goal: StudyGoalEntity): Long = dao.insertGoal(goal)
    suspend fun updateGoal(goal: StudyGoalEntity) = dao.updateGoal(goal)
    suspend fun deleteGoal(id: Long) = dao.deleteGoalById(id)

    suspend fun insertReminder(reminder: RevisionReminderEntity): Long = dao.insertReminder(reminder)
    suspend fun updateReminder(reminder: RevisionReminderEntity) = dao.updateReminder(reminder)
    suspend fun deleteReminder(id: Long) = dao.deleteReminderById(id)

    suspend fun importGeneratedSyllabus(
        syllabus: GeneratedSyllabus,
        examDateEpochMillis: Long?,
        targetDateEpochMillis: Long?
    ): Long = withContext(Dispatchers.IO) {
        val subjectId = dao.insertSubject(
            SubjectEntity(
                name = syllabus.subjectName,
                code = syllabus.subjectCode,
                colorHex = syllabus.suggestedColorHex,
                iconName = "menu_book",
                targetDateEpochMillis = targetDateEpochMillis,
                examDateEpochMillis = examDateEpochMillis,
                examName = syllabus.examName
            )
        )

        for (unit in syllabus.units) {
            val chapterId = dao.insertChapter(
                ChapterEntity(
                    subjectId = subjectId,
                    name = unit.name,
                    unitNumber = unit.unitNumber,
                    orderIndex = unit.unitNumber
                )
            )

            for ((idx, topic) in unit.topics.withIndex()) {
                dao.insertTopic(
                    TopicEntity(
                        chapterId = chapterId,
                        subjectId = subjectId,
                        title = topic.title,
                        subtopicsText = topic.subtopics.joinToString(", "),
                        estimatedHours = topic.estimatedHours,
                        targetDateEpochMillis = targetDateEpochMillis,
                        status = TopicStatus.NOT_STARTED,
                        orderIndex = idx
                    )
                )
            }
        }

        // Kickoff goal
        val firstUnitTopicCount = syllabus.units.firstOrNull()?.topics?.size ?: 3
        dao.insertGoal(
            StudyGoalEntity(
                title = "Complete Unit 1 of ${syllabus.subjectName}",
                goalType = GoalType.WEEKLY,
                targetTopicsCount = firstUnitTopicCount.coerceAtLeast(1),
                targetHours = 6.0f,
                startDateEpochMillis = System.currentTimeMillis(),
                endDateEpochMillis = System.currentTimeMillis() + (7 * 86400000L)
            )
        )

        subjectId
    }

    suspend fun seedSampleDataIfEmpty() {
        withContext(Dispatchers.IO) {
            val existing = dao.getAllSubjects().first()
            if (existing.isNotEmpty()) return@withContext

            val now = System.currentTimeMillis()
            val oneDayMillis = 86400000L

            // 1. Data Structures & Algorithms (Exam in 24 days)
            val dsaSubject = SubjectEntity(
                name = "Data Structures & Algorithms",
                code = "CS-201",
                colorHex = "#4F46E5",
                iconName = "code",
                targetDateEpochMillis = now + (20 * oneDayMillis),
                examDateEpochMillis = now + (24 * oneDayMillis),
                examName = "End-Semester Theory Exam"
            )
            val dsaId = dao.insertSubject(dsaSubject)

            val dsaChap1 = dao.insertChapter(ChapterEntity(subjectId = dsaId, name = "Unit 1: Linear Data Structures", unitNumber = 1, orderIndex = 1))
            val dsaChap2 = dao.insertChapter(ChapterEntity(subjectId = dsaId, name = "Unit 2: Trees & Balanced BSTs", unitNumber = 2, orderIndex = 2))
            val dsaChap3 = dao.insertChapter(ChapterEntity(subjectId = dsaId, name = "Unit 3: Graph Algorithms", unitNumber = 3, orderIndex = 3))
            val dsaChap4 = dao.insertChapter(ChapterEntity(subjectId = dsaId, name = "Unit 4: Dynamic Programming", unitNumber = 4, orderIndex = 4))

            dao.insertTopic(TopicEntity(chapterId = dsaChap1, subjectId = dsaId, title = "Arrays & Two-Pointer Techniques", subtopicsText = "Prefix Sum, Sliding Window, Dutch National Flag", status = TopicStatus.COMPLETED, completionEpochMillis = now - (3 * oneDayMillis), estimatedHours = 3f, orderIndex = 1))
            dao.insertTopic(TopicEntity(chapterId = dsaChap1, subjectId = dsaId, title = "Linked Lists Operations", subtopicsText = "Singly, Doubly, Floyd's Cycle Detection", status = TopicStatus.COMPLETED, completionEpochMillis = now - (2 * oneDayMillis), estimatedHours = 2.5f, orderIndex = 2))
            dao.insertTopic(TopicEntity(chapterId = dsaChap1, subjectId = dsaId, title = "Stacks & Monotonic Queues", subtopicsText = "Next Greater Element, Min Stack, Infix to Postfix", status = TopicStatus.IN_PROGRESS, estimatedHours = 3f, orderIndex = 3))

            dao.insertTopic(TopicEntity(chapterId = dsaChap2, subjectId = dsaId, title = "Binary Trees & Traversals", subtopicsText = "Inorder, Preorder, Postorder, BFS Level-Order", status = TopicStatus.COMPLETED, completionEpochMillis = now - (1 * oneDayMillis), estimatedHours = 4f, orderIndex = 1))
            dao.insertTopic(TopicEntity(chapterId = dsaChap2, subjectId = dsaId, title = "Binary Search Trees & AVL", subtopicsText = "BST Search, Insert, AVL Tree Rotations", status = TopicStatus.NOT_STARTED, estimatedHours = 3.5f, orderIndex = 2))

            dao.insertTopic(TopicEntity(chapterId = dsaChap3, subjectId = dsaId, title = "Graph Traversals (BFS & DFS)", subtopicsText = "Connected Components, Topological Sort", status = TopicStatus.NOT_STARTED, estimatedHours = 4f, orderIndex = 1))
            dao.insertTopic(TopicEntity(chapterId = dsaChap3, subjectId = dsaId, title = "Shortest Paths (Dijkstra, Bellman-Ford)", subtopicsText = "PriorityQueue Dijkstra, Negative Cycles", status = TopicStatus.NOT_STARTED, estimatedHours = 3.5f, orderIndex = 2))

            dao.insertTopic(TopicEntity(chapterId = dsaChap4, subjectId = dsaId, title = "0/1 Knapsack & Subset Sum", subtopicsText = "Memoization vs Tabulation, Space Optimization", status = TopicStatus.NOT_STARTED, estimatedHours = 4.5f, orderIndex = 1))
            dao.insertTopic(TopicEntity(chapterId = dsaChap4, subjectId = dsaId, title = "Longest Common Subsequence (LCS)", subtopicsText = "Print LCS, Shortest Common Supersequence", status = TopicStatus.NOT_STARTED, estimatedHours = 3f, orderIndex = 2))

            // 2. Database Management Systems (Exam in 38 days)
            val dbSubject = SubjectEntity(
                name = "Database Management Systems",
                code = "CS-204",
                colorHex = "#0D9488",
                iconName = "storage",
                targetDateEpochMillis = now + (35 * oneDayMillis),
                examDateEpochMillis = now + (38 * oneDayMillis),
                examName = "GATE / University Exam"
            )
            val dbId = dao.insertSubject(dbSubject)

            val dbChap1 = dao.insertChapter(ChapterEntity(subjectId = dbId, name = "Unit 1: ER Model & Relational Algebra", unitNumber = 1, orderIndex = 1))
            val dbChap2 = dao.insertChapter(ChapterEntity(subjectId = dbId, name = "Unit 2: SQL & Normalization", unitNumber = 2, orderIndex = 2))
            val dbChap3 = dao.insertChapter(ChapterEntity(subjectId = dbId, name = "Unit 3: Transactions & Concurrency", unitNumber = 3, orderIndex = 3))

            dao.insertTopic(TopicEntity(chapterId = dbChap1, subjectId = dbId, title = "Entity Relationship Diagrams", subtopicsText = "Entities, Cardinality, Strong vs Weak Entity", status = TopicStatus.COMPLETED, completionEpochMillis = now - (4 * oneDayMillis), estimatedHours = 2f, orderIndex = 1))
            dao.insertTopic(TopicEntity(chapterId = dbChap1, subjectId = dbId, title = "Relational Algebra Operations", subtopicsText = "Selection, Projection, Cartesian Product, Joins", status = TopicStatus.COMPLETED, completionEpochMillis = now - (1 * oneDayMillis), estimatedHours = 3f, orderIndex = 2))

            dao.insertTopic(TopicEntity(chapterId = dbChap2, subjectId = dbId, title = "Functional Dependencies & 1NF/2NF/3NF", subtopicsText = "Attribute Closure, Candidate Keys, Normal Forms", status = TopicStatus.IN_PROGRESS, estimatedHours = 4f, orderIndex = 1))
            dao.insertTopic(TopicEntity(chapterId = dbChap2, subjectId = dbId, title = "Boyce-Codd Normal Form (BCNF)", subtopicsText = "Dependency Preservation, Lossless Decomposition", status = TopicStatus.NOT_STARTED, estimatedHours = 2.5f, orderIndex = 2))

            dao.insertTopic(TopicEntity(chapterId = dbChap3, subjectId = dbId, title = "ACID Properties & Serializability", subtopicsText = "Conflict Serializability, Precedence Graph", status = TopicStatus.NOT_STARTED, estimatedHours = 3.5f, orderIndex = 1))
            dao.insertTopic(TopicEntity(chapterId = dbChap3, subjectId = dbId, title = "Two-Phase Locking Protocol (2PL)", subtopicsText = "Strict 2PL, Deadlock Handling", status = TopicStatus.NOT_STARTED, estimatedHours = 3f, orderIndex = 2))

            // 3. Operating Systems (Exam in 16 days)
            val osSubject = SubjectEntity(
                name = "Operating Systems",
                code = "CS-202",
                colorHex = "#F59E0B",
                iconName = "settings_suggest",
                targetDateEpochMillis = now + (14 * oneDayMillis),
                examDateEpochMillis = now + (16 * oneDayMillis),
                examName = "Midterm Major Exam"
            )
            val osId = dao.insertSubject(osSubject)

            val osChap1 = dao.insertChapter(ChapterEntity(subjectId = osId, name = "Unit 1: Process Management & CPU Scheduling", unitNumber = 1, orderIndex = 1))
            val osChap2 = dao.insertChapter(ChapterEntity(subjectId = osId, name = "Unit 2: Process Synchronization & Deadlocks", unitNumber = 2, orderIndex = 2))
            val osChap3 = dao.insertChapter(ChapterEntity(subjectId = osId, name = "Unit 3: Memory Management & Paging", unitNumber = 3, orderIndex = 3))

            dao.insertTopic(TopicEntity(chapterId = osChap1, subjectId = osId, title = "Process States & PCB", subtopicsText = "Context Switching, Fork System Call", status = TopicStatus.COMPLETED, completionEpochMillis = now - (5 * oneDayMillis), estimatedHours = 2f, orderIndex = 1))
            dao.insertTopic(TopicEntity(chapterId = osChap1, subjectId = osId, title = "CPU Scheduling Algorithms", subtopicsText = "FCFS, SJF, Round Robin, Priority Scheduling", status = TopicStatus.COMPLETED, completionEpochMillis = now - (2 * oneDayMillis), estimatedHours = 3.5f, orderIndex = 2))
            dao.insertTopic(TopicEntity(chapterId = osChap2, subjectId = osId, title = "Critical Section & Semaphores", subtopicsText = "Peterson's Solution, Counting Semaphores, Mutex", status = TopicStatus.IN_PROGRESS, estimatedHours = 4f, orderIndex = 1))
            dao.insertTopic(TopicEntity(chapterId = osChap2, subjectId = osId, title = "Deadlock Banker's Algorithm", subtopicsText = "Resource Allocation Graph, Deadlock Detection", status = TopicStatus.NOT_STARTED, estimatedHours = 3f, orderIndex = 2))
            dao.insertTopic(TopicEntity(chapterId = osChap3, subjectId = osId, title = "Virtual Memory & Page Replacement", subtopicsText = "LRU, FIFO, Optimal, Belady's Anomaly, Thrashing", status = TopicStatus.NOT_STARTED, estimatedHours = 4f, orderIndex = 1))

            // Seed Study Goals: Daily, Weekly, Monthly
            dao.insertGoal(
                StudyGoalEntity(
                    title = "Complete 3 syllabus topics today",
                    goalType = GoalType.DAILY,
                    targetTopicsCount = 3,
                    completedTopicsCount = 1,
                    targetHours = 3.0f,
                    completedHours = 1.5f,
                    startDateEpochMillis = now,
                    endDateEpochMillis = now + oneDayMillis
                )
            )
            dao.insertGoal(
                StudyGoalEntity(
                    title = "Finish Trees & Dynamic Programming Units",
                    goalType = GoalType.WEEKLY,
                    targetTopicsCount = 8,
                    completedTopicsCount = 4,
                    targetHours = 16.0f,
                    completedHours = 9.0f,
                    startDateEpochMillis = now,
                    endDateEpochMillis = now + (7 * oneDayMillis)
                )
            )
            dao.insertGoal(
                StudyGoalEntity(
                    title = "Cover 75% of Operating Systems & DSA",
                    goalType = GoalType.MONTHLY,
                    targetTopicsCount = 20,
                    completedTopicsCount = 9,
                    targetHours = 45.0f,
                    completedHours = 26.0f,
                    startDateEpochMillis = now,
                    endDateEpochMillis = now + (30 * oneDayMillis)
                )
            )

            // Seed Activity History for study streak (last 5 days)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance()
            for (i in 4 downTo 0) {
                cal.timeInMillis = now - (i * oneDayMillis)
                val dateStr = sdf.format(cal.time)
                dao.insertActivity(
                    StudyActivityEntity(
                        dateString = dateStr,
                        topicsCompletedCount = if (i == 0) 1 else (2 + (i % 2)),
                        studyMinutes = 45 + (i * 15),
                        timestamp = cal.timeInMillis
                    )
                )
            }

            // Seed Revision Reminders
            dao.insertReminder(
                RevisionReminderEntity(
                    topicId = 1,
                    topicTitle = "Binary Trees & Traversals",
                    subjectName = "Data Structures & Algorithms",
                    reminderTimeEpochMillis = now + (1 * oneDayMillis),
                    repeatIntervalDays = 3,
                    isCompleted = false
                )
            )
            dao.insertReminder(
                RevisionReminderEntity(
                    topicId = 2,
                    topicTitle = "CPU Scheduling Algorithms",
                    subjectName = "Operating Systems",
                    reminderTimeEpochMillis = now + (2 * oneDayMillis),
                    repeatIntervalDays = 7,
                    isCompleted = false
                )
            )
        }
    }

    // Export entire syllabus state to formatted JSON for Cloud Sync / Backup
    suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val allSubs = dao.getAllSubjects().first()
        val allChaps = dao.getAllChapters().first()
        val allTops = dao.getAllTopics().first()
        val allGls = dao.getAllGoals().first()
        val allActs = dao.getAllActivities().first()

        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val subArray = JSONArray()
        allSubs.forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("code", s.code)
            obj.put("colorHex", s.colorHex)
            obj.put("iconName", s.iconName)
            obj.put("targetDateEpochMillis", s.targetDateEpochMillis ?: -1L)
            obj.put("examDateEpochMillis", s.examDateEpochMillis ?: -1L)
            obj.put("examName", s.examName ?: "")
            subArray.put(obj)
        }
        root.put("subjects", subArray)

        val chapArray = JSONArray()
        allChaps.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("subjectId", c.subjectId)
            obj.put("name", c.name)
            obj.put("unitNumber", c.unitNumber)
            obj.put("orderIndex", c.orderIndex)
            chapArray.put(obj)
        }
        root.put("chapters", chapArray)

        val topArray = JSONArray()
        allTops.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("chapterId", t.chapterId)
            obj.put("subjectId", t.subjectId)
            obj.put("title", t.title)
            obj.put("subtopicsText", t.subtopicsText)
            obj.put("status", t.status.name)
            obj.put("completionEpochMillis", t.completionEpochMillis ?: -1L)
            obj.put("targetDateEpochMillis", t.targetDateEpochMillis ?: -1L)
            obj.put("isRevisionNeeded", t.isRevisionNeeded)
            obj.put("revisionCount", t.revisionCount)
            obj.put("estimatedHours", t.estimatedHours.toDouble())
            topArray.put(obj)
        }
        root.put("topics", topArray)

        root.toString(2)
    }

    // Import from JSON to restore syllabus on new device / from cloud
    suspend fun importFromJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            dao.clearTopics()
            dao.clearChapters()
            dao.clearSubjects()

            val subArray = root.optJSONArray("subjects") ?: JSONArray()
            for (i in 0 until subArray.length()) {
                val obj = subArray.getJSONObject(i)
                val targetDate = obj.optLong("targetDateEpochMillis", -1L).takeIf { it > 0 }
                val examDate = obj.optLong("examDateEpochMillis", -1L).takeIf { it > 0 }
                dao.insertSubject(
                    SubjectEntity(
                        id = obj.optLong("id", 0),
                        name = obj.getString("name"),
                        code = obj.optString("code", ""),
                        colorHex = obj.optString("colorHex", "#4F46E5"),
                        iconName = obj.optString("iconName", "menu_book"),
                        targetDateEpochMillis = targetDate,
                        examDateEpochMillis = examDate,
                        examName = obj.optString("examName", "").takeIf { it.isNotBlank() }
                    )
                )
            }

            val chapArray = root.optJSONArray("chapters") ?: JSONArray()
            for (i in 0 until chapArray.length()) {
                val obj = chapArray.getJSONObject(i)
                dao.insertChapter(
                    ChapterEntity(
                        id = obj.optLong("id", 0),
                        subjectId = obj.getLong("subjectId"),
                        name = obj.getString("name"),
                        unitNumber = obj.optInt("unitNumber", 1),
                        orderIndex = obj.optInt("orderIndex", 0)
                    )
                )
            }

            val topArray = root.optJSONArray("topics") ?: JSONArray()
            for (i in 0 until topArray.length()) {
                val obj = topArray.getJSONObject(i)
                val statusStr = obj.optString("status", "NOT_STARTED")
                val status = try { TopicStatus.valueOf(statusStr) } catch (_: Exception) { TopicStatus.NOT_STARTED }
                val compDate = obj.optLong("completionEpochMillis", -1L).takeIf { it > 0 }
                val targetDate = obj.optLong("targetDateEpochMillis", -1L).takeIf { it > 0 }
                dao.insertTopic(
                    TopicEntity(
                        id = obj.optLong("id", 0),
                        chapterId = obj.getLong("chapterId"),
                        subjectId = obj.getLong("subjectId"),
                        title = obj.getString("title"),
                        subtopicsText = obj.optString("subtopicsText", ""),
                        status = status,
                        completionEpochMillis = compDate,
                        targetDateEpochMillis = targetDate,
                        isRevisionNeeded = obj.optBoolean("isRevisionNeeded", false),
                        revisionCount = obj.optInt("revisionCount", 0),
                        estimatedHours = obj.optDouble("estimatedHours", 1.0).toFloat()
                    )
                )
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
