package com.example.aura.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val timeLabel: String, // e.g. "7:00 PM"
    val isCompleted: Boolean = false,
    val dateLabel: String = "" // e.g. "Today" or "Tomorrow"
)

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String, // "Study", "Health", "App Limit", "Personal"
    val streak: Int = 0,
    val completedDatesCsv: String = "", // comma separated dates e.g. "2026-06-18,2026-06-19"
    val dailyLimitHours: Float = 0f, // limit for frequently used apps count
    val trackedUsageMinutes: Int = 0, // mock tracked minutes for "App limit" habits e.g. 180 min Instagram
    val isActiveLimit: Boolean = false
)

@Entity(tableName = "mood_logs")
data class MoodLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val feelingScore: Int, // 1 to 5 (Terrible, Bad, Neutral, Good, Awesome)
    val journalText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val physicalMocks: String = "" // mock voice/sound or heartbeat indicators if any
)

@Entity(tableName = "study_notes")
data class StudyNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "quizzes")
data class Quiz(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studyNoteId: Long,
    val question: String,
    val optionsCsv: String, // e.g. "Stack,Queue,Tree,Graph"
    val answer: String,
    val explanation: String = ""
)

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studyNoteId: Long,
    val front: String,
    val back: String,
    val mastered: Boolean = false
)

@Entity(tableName = "assistant_memories")
data class AssistantMemory(
    @PrimaryKey val key: String, // e.g. "sleep_schedule", "study_habits", "interests_goals", "writing_style"
    val value: String,
    val lastUpdated: Long = System.currentTimeMillis()
)
