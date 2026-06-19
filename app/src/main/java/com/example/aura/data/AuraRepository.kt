package com.example.aura.data

import kotlinx.coroutines.flow.Flow

class AuraRepository(private val dao: AuraDao) {
    // Reminders
    val allReminders: Flow<List<Reminder>> = dao.getAllReminders()
    suspend fun insertReminder(reminder: Reminder) = dao.insertReminder(reminder)
    suspend fun updateReminder(reminder: Reminder) = dao.updateReminder(reminder)
    suspend fun deleteReminder(reminder: Reminder) = dao.deleteReminder(reminder)
    suspend fun deleteReminderById(id: Long) = dao.deleteReminderById(id)

    // Habits
    val allHabits: Flow<List<Habit>> = dao.getAllHabits()
    suspend fun insertHabit(habit: Habit) = dao.insertHabit(habit)
    suspend fun updateHabit(habit: Habit) = dao.updateHabit(habit)
    suspend fun deleteHabit(habit: Habit) = dao.deleteHabit(habit)

    // Mood Logs
    val allMoodLogs: Flow<List<MoodLog>> = dao.getAllMoodLogs()
    suspend fun insertMoodLog(moodLog: MoodLog) = dao.insertMoodLog(moodLog)

    // Study Notes
    val allStudyNotes: Flow<List<StudyNote>> = dao.getAllStudyNotes()
    suspend fun insertStudyNote(note: StudyNote): Long = dao.insertStudyNote(note)
    suspend fun getStudyNoteById(id: Long) = dao.getStudyNoteById(id)
    suspend fun deleteStudyNoteById(id: Long) = dao.deleteStudyNoteById(id)

    // Quizzes
    fun getQuizzesForNote(noteId: Long): Flow<List<Quiz>> = dao.getQuizzesForNote(noteId)
    suspend fun insertQuizzes(quizzes: List<Quiz>) = dao.insertQuizzes(quizzes)
    suspend fun deleteQuizzesByNoteId(noteId: Long) = dao.deleteQuizzesByNoteId(noteId)

    // Flashcards
    fun getFlashcardsForNote(noteId: Long): Flow<List<Flashcard>> = dao.getFlashcardsForNote(noteId)
    suspend fun insertFlashcard(flashcard: Flashcard) = dao.insertFlashcard(flashcard)
    suspend fun updateFlashcard(flashcard: Flashcard) = dao.updateFlashcard(flashcard)

    // Assistant Memory
    val allMemoriesFlow: Flow<List<AssistantMemory>> = dao.getAllMemoriesFlow()
    suspend fun getMemoryByKey(key: String) = dao.getMemoryByKey(key)
    suspend fun insertMemory(memory: AssistantMemory) = dao.insertMemory(memory)
}
