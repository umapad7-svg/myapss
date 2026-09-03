package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SyllabusDatabase
import com.example.data.local.entity.StudyActivityEntity
import com.example.data.remote.GeminiSyllabusService
import com.example.data.remote.GeneratedSyllabus
import com.example.data.remote.GeneratedTopic
import com.example.data.remote.GeneratedUnit
import com.example.data.repository.SyllabusRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Syllabus Tracker", appName)
  }

  @Test
  fun `test streak calculation with consecutive days`() {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val cal = Calendar.getInstance()
    val today = sdf.format(cal.time)
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = sdf.format(cal.time)
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val dayBefore = sdf.format(cal.time)

    val activities = listOf(
      StudyActivityEntity(dateString = today, topicsCompletedCount = 2),
      StudyActivityEntity(dateString = yesterday, topicsCompletedCount = 1),
      StudyActivityEntity(dateString = dayBefore, topicsCompletedCount = 3)
    )

    val streak = DateUtils.calculateStreak(activities)
    assertEquals(3, streak)
  }

  @Test
  fun `test fallback syllabus generator generates structured units and topics`() {
    val syllabus = GeminiSyllabusService.generateFallbackSyllabus(
      subjectTitle = "Operating Systems",
      academicLevel = "College",
      examType = "Finals",
      targetUnitsCount = 4
    )

    assertEquals("Operating Systems", syllabus.subjectName)
    assertTrue(syllabus.units.isNotEmpty())
    assertTrue(syllabus.units.all { it.topics.isNotEmpty() })
    val totalTopics = syllabus.units.sumOf { it.topics.size }
    assertTrue(totalTopics > 0)
  }

  @Test
  fun `test importing generated syllabus into repository`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, SyllabusDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    val repo = SyllabusRepository(db.syllabusDao())

    val testSyllabus = GeneratedSyllabus(
      subjectName = "Organic Chemistry",
      subjectCode = "CHEM-201",
      examName = "End-Sem",
      units = listOf(
        GeneratedUnit(
          unitNumber = 1,
          name = "Unit 1: Reaction Mechanisms",
          topics = listOf(
            GeneratedTopic(
              title = "Electrophilic Addition",
              estimatedHours = 3.5f,
              subtopics = listOf("Markovnikov's Rule", "Carbocation Rearrangements")
            )
          )
        )
      )
    )

    val newId = repo.importGeneratedSyllabus(
      syllabus = testSyllabus,
      examDateEpochMillis = System.currentTimeMillis() + 864000000L,
      targetDateEpochMillis = System.currentTimeMillis() + 600000000L
    )

    assertTrue(newId > 0)
    val subjects = repo.subjects.first()
    assertEquals(1, subjects.size)
    assertEquals("Organic Chemistry", subjects[0].name)

    val chapters = repo.chapters.first()
    assertEquals(1, chapters.size)
    assertEquals("Unit 1: Reaction Mechanisms", chapters[0].name)

    val topics = repo.topics.first()
    assertEquals(1, topics.size)
    assertEquals("Electrophilic Addition", topics[0].title)

    db.close()
  }
}
