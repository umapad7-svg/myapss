package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.SyllabusDao
import com.example.data.local.entity.ChapterEntity
import com.example.data.local.entity.RevisionReminderEntity
import com.example.data.local.entity.StudyActivityEntity
import com.example.data.local.entity.StudyGoalEntity
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TopicEntity

@Database(
    entities = [
        SubjectEntity::class,
        ChapterEntity::class,
        TopicEntity::class,
        StudyGoalEntity::class,
        StudyActivityEntity::class,
        RevisionReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SyllabusDatabase : RoomDatabase() {
    abstract fun syllabusDao(): SyllabusDao

    companion object {
        @Volatile
        private var INSTANCE: SyllabusDatabase? = null

        fun getInstance(context: Context): SyllabusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SyllabusDatabase::class.java,
                    "syllabus_tracker_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
