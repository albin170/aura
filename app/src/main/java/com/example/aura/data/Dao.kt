package com.example.aura.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AuraDao {
    // Reminders
    @Query("SELECT * FROM reminders ORDER BY id DESC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder)

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    // Habits
    @Query("SELECT * FROM habits ORDER BY name ASC")
    fun getAllHabits(): Flow<List<Habit>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit)

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    // Mood Logs
    @Query("SELECT * FROM mood_logs ORDER BY timestamp DESC")
    fun getAllMoodLogs(): Flow<List<MoodLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMoodLog(moodLog: MoodLog)

    // Study Notes
    @Query("SELECT * FROM study_notes ORDER BY timestamp DESC")
    fun getAllStudyNotes(): Flow<List<StudyNote>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudyNote(note: StudyNote): Long

    @Query("SELECT * FROM study_notes WHERE id = :id")
    suspend fun getStudyNoteById(id: Long): StudyNote?

    @Query("DELETE FROM study_notes WHERE id = :id")
    suspend fun deleteStudyNoteById(id: Long)

    // Quizzes
    @Query("SELECT * FROM quizzes WHERE studyNoteId = :noteId")
    fun getQuizzesForNote(noteId: Long): Flow<List<Quiz>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizzes(quizzes: List<Quiz>)

    @Query("DELETE FROM quizzes WHERE studyNoteId = :noteId")
    suspend fun deleteQuizzesByNoteId(noteId: Long)

    // Flashcards
    @Query("SELECT * FROM flashcards WHERE studyNoteId = :noteId")
    fun getFlashcardsForNote(noteId: Long): Flow<List<Flashcard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: Flashcard)

    @Update
    suspend fun updateFlashcard(flashcard: Flashcard)

    // Assistant Memory
    @Query("SELECT * FROM assistant_memories")
    fun getAllMemoriesFlow(): Flow<List<AssistantMemory>>

    @Query("SELECT * FROM assistant_memories WHERE `key` = :key")
    suspend fun getMemoryByKey(key: String): AssistantMemory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: AssistantMemory)
}
