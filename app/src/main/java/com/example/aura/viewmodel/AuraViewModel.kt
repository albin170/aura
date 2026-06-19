package com.example.aura.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aura.data.*
import com.example.aura.service.GeminiService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ChatTurn(val sender: String, val text: String, val timestamp: Long = System.currentTimeMillis())

class AuraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repo = AuraRepository(db.auraDao())
    private val gemini = GeminiService()

    // Database flows
    val reminders: StateFlow<List<Reminder>> = repo.allReminders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val habits: StateFlow<List<Habit>> = repo.allHabits.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val moodLogs: StateFlow<List<MoodLog>> = repo.allMoodLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val studyNotes: StateFlow<List<StudyNote>> = repo.allStudyNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val memories: StateFlow<List<AssistantMemory>> = repo.allMemoriesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Interactive UI States
    private val _chatMessages = MutableStateFlow<List<ChatTurn>>(
        listOf(
            ChatTurn("Aura", "Hello! I am Aura, your Personal AI Twin and Companion. I track your sleep, learn your habits, and manage your day. Try saying 'Plan my day' or 'Remind me to study DSA at 7 PM'. How can I help you today?")
        )
    )
    val chatMessages: StateFlow<List<ChatTurn>> = _chatMessages.asStateFlow()

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

    private val _proactiveSuggestions = MutableStateFlow<List<String>>(emptyList())
    val proactiveSuggestions: StateFlow<List<String>> = _proactiveSuggestions.asStateFlow()

    // Note Details (Quizzes and Flashcards)
    private val _selectedStudyNoteId = MutableStateFlow<Long?>(null)
    val selectedStudyNoteId: StateFlow<Long?> = _selectedStudyNoteId.asStateFlow()

    val currentQuizzes: StateFlow<List<Quiz>> = _selectedStudyNoteId.flatMapLatest { noteId ->
        if (noteId != null) repo.getQuizzesForNote(noteId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentFlashcards: StateFlow<List<Flashcard>> = _selectedStudyNoteId.flatMapLatest { noteId ->
        if (noteId != null) repo.getFlashcardsForNote(noteId) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _quizGenerationLoading = MutableStateFlow(false)
    val quizGenerationLoading: StateFlow<Boolean> = _quizGenerationLoading.asStateFlow()

    init {
        // Hydrate default memory if database is empty
        viewModelScope.launch {
            repo.allMemoriesFlow.first().let { currentList ->
                if (currentList.isEmpty()) {
                    repo.insertMemory(AssistantMemory("sleep_schedule", "Generally sleeps from 11:30 PM to 7:00 AM. Prefers 7.5 hours of sleep."))
                    repo.insertMemory(AssistantMemory("study_habits", "Focuses deep-sessions on DBMS and DSA. Learns best using quizzes & active recall."))
                    repo.insertMemory(AssistantMemory("interests_goals", "Goals: Master software engineering, establish physical habit streaks, and maintain screen limits."))
                    repo.insertMemory(AssistantMemory("writing_style", "Clear, energetic, direct, and slightly technical with short punchy bullet points."))
                }
            }
            repo.allHabits.first().let { currentHabits ->
                if (currentHabits.isEmpty()) {
                    repo.insertHabit(Habit(name = "Instagram limit", category = "App Limit", streak = 5, dailyLimitHours = 1.0f, trackedUsageMinutes = 180, isActiveLimit = true))
                    repo.insertHabit(Habit(name = "Revise DBMS", category = "Study", streak = 3))
                    repo.insertHabit(Habit(name = "Workout 20m", category = "Health", streak = 12))
                }
            }
            repo.allReminders.first().let { currentReminders ->
                if (currentReminders.isEmpty()) {
                    repo.insertReminder(Reminder(text = "Revise DBMS Indexing chapter", timeLabel = "5:00 PM", dateLabel = "Today"))
                    repo.insertReminder(Reminder(text = "Push daily code to Git", timeLabel = "9:30 PM", dateLabel = "Today"))
                }
            }
            generateProactiveAlerts()
        }
    }

    fun selectStudyNoteId(id: Long?) {
        _selectedStudyNoteId.value = id
    }

    // Generate smart suggestions dynamically based on user habits and state
    private fun generateProactiveAlerts() {
        viewModelScope.launch {
            val alerts = mutableListOf<String>()
            val currentHabits = repo.allHabits.first()
            val currentReminders = repo.allReminders.first()

            val instagramLimit = currentHabits.firstOrNull { it.name.contains("Instagram", ignoreCase = true) }
            if (instagramLimit != null && instagramLimit.trackedUsageMinutes > (instagramLimit.dailyLimitHours * 60)) {
                alerts.add("⚠️ Screen Limit alert: You spent 3 hours on Instagram today (Limit: 1 hour). Want to set a strict lock?")
            }

            alerts.add("⏳ Study Companion: Scheduled exam is in 5 days. Start revising DBMS Indexes now.")
            alerts.add("💡 Active streak: Your 'Workout 20m' streak is at 12 days! Don't break it today.")
            
            _proactiveSuggestions.value = alerts
        }
    }

    // Interactive Action Parsing from Chat Text
    fun sendMessage(userText: String) {
        if (userText.trim().isEmpty()) return

        _chatMessages.update { it + ChatTurn("User", userText) }
        _chatLoading.value = true

        viewModelScope.launch {
            // Read active DB context to feed Gemini
            val currentRemindersList = repo.allReminders.first()
            val currentHabitsList = repo.allHabits.first()
            val memoriesList = repo.allMemoriesFlow.first()

            val memoriesContext = memoriesList.joinToString("\n") { "- ${it.key}: ${it.value}" }
            val remindersContext = currentRemindersList.joinToString("\n") { "- ${it.text} at ${it.timeLabel} (${if (it.isCompleted) "Done" else "Pending"})" }
            val habitsContext = currentHabitsList.joinToString("\n") { "- ${it.name} (${it.category}) - Streak: ${it.streak} days" }

            val systemInstruction = """
                You are Aura, an elite personal AI twin and daily activities assistant.
                You understand sleep cycles, study habits, and manage schedules.
                You adapt to the user's Writing Style:
                ${memoriesList.firstOrNull { it.key == "writing_style" }?.value ?: "Direct, warm, energetic"}
                
                Your Current Memory State:
                $memoriesContext
                
                Active Daily Tasks / Reminders:
                $remindersContext
                
                Habits and Streaks Tracked:
                $habitsContext
                
                Instructions:
                - Treat yourself as conversational, incredibly supportive, and deeply integrated.
                - If user asks you to "Plan my day", draft a beautifully structured, personalized schedule fitting their study habits and sleep schedule.
                - If they request reminders e.g., "Remind me to study DSA at 7 PM", mention that you have added this to their reminders.
                - If they ask to summarize notifications, summarize mock notifications beautifully: Instagram limit alerts, upcoming quizzes, progress.
                - Be concise, elegant, and avoid jargon. Keep responses engaging and directly helpful.
            """.trimIndent()

            // Safe Command Extraction on local DB side
            try {
                handleLocalActions(userText)
            } catch (e: Exception) {
                // Fail-safe
            }

            val aiResponse = gemini.generateResponse(userText, systemInstruction)
            _chatMessages.update { it + ChatTurn("Aura", aiResponse) }
            _chatLoading.value = false
            generateProactiveAlerts()
        }
    }

    // Helper to detect patterns directly
    private suspend fun handleLocalActions(text: String) {
        val lower = text.lowercase()
        if (lower.contains("remind me to") || lower.contains("set reminder for")) {
            // pattern: Remind me to [TASK] at [TIME]
            val taskIdx = lower.indexOf("remind me to ")
            if (taskIdx != -1) {
                val sentence = text.substring(taskIdx + 13)
                val atIdx = sentence.lowercase().indexOf(" at ")
                val taskText = if (atIdx != -1) sentence.substring(0, atIdx) else sentence
                val timeLabel = if (atIdx != -1) sentence.substring(atIdx + 4) else "Today"
                repo.insertReminder(Reminder(text = taskText.trim(), timeLabel = timeLabel.trim(), dateLabel = "Today"))
            }
        } else if (lower.contains("set a limit of") || lower.contains("limit instagram")) {
            val limitHabit = Habit(name = "Instagram Screen Limit", category = "App Limit", streak = 0, dailyLimitHours = 1.0f, trackedUsageMinutes = 0, isActiveLimit = true)
            repo.insertHabit(limitHabit)
        }
    }

    // Direct Database State Mutators

    // Reminders
    fun addReminder(text: String, time: String) {
        viewModelScope.launch {
            repo.insertReminder(Reminder(text = text, timeLabel = time, dateLabel = "Today"))
            generateProactiveAlerts()
        }
    }

    fun toggleReminder(reminder: Reminder) {
        viewModelScope.launch {
            repo.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repo.deleteReminder(reminder)
        }
    }

    // Habits
    fun addHabit(name: String, category: String, limitHours: Float = 0f) {
        viewModelScope.launch {
            val h = Habit(
                name = name,
                category = category,
                dailyLimitHours = limitHours,
                isActiveLimit = limitHours > 0f
            )
            repo.insertHabit(h)
            generateProactiveAlerts()
        }
    }

    fun checkInHabit(habit: Habit) {
        viewModelScope.launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val currentCsv = habit.completedDatesCsv
            if (!currentCsv.contains(todayStr)) {
                val newCsv = if (currentCsv.isEmpty()) todayStr else "$currentCsv,$todayStr"
                repo.updateHabit(habit.copy(streak = habit.streak + 1, completedDatesCsv = newCsv))
            }
            generateProactiveAlerts()
        }
    }

    fun resetHabit(habit: Habit) {
        viewModelScope.launch {
            repo.updateHabit(habit.copy(streak = 0, completedDatesCsv = ""))
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repo.deleteHabit(habit)
            generateProactiveAlerts()
        }
    }

    // Mood & Journal
    fun addMoodLog(feelingScore: Int, journal: String) {
        viewModelScope.launch {
            repo.insertMoodLog(MoodLog(feelingScore = feelingScore, journalText = journal))
        }
    }

    // Study Note and Auto Generators
    fun addStudyNoteAndGenerateActivities(title: String, content: String) {
        _quizGenerationLoading.value = true
        viewModelScope.launch {
            val noteId = repo.insertStudyNote(StudyNote(title = title, content = content))
            
            // Ask Gemini to generate 3 quizzes and 3 flashcards in standard comma formats for robust parsing
            val query = """
                Based on the following student study notes, generate exactly:
                A) 3 Multiple-Choice Questions (Format each question exactly on a new line: Q: Question text | Options: Op1,Op2,Op3,Op4 | Ans: SelectedCorrectOption | Expl: explanation)
                B) 3 Flashcards (Format each flashcard exactly on a new line: F: Front text | B: Back text)
                
                Study Notes content:
                $content
            """.trimIndent()

            val rawOutput = gemini.generateResponse(query, "You are a smart study assistant who outputs strict, clean formats.")
            
            // Parse raw response
            val quizList = mutableListOf<Quiz>()
            val flashcardList = mutableListOf<Flashcard>()

            rawOutput.lines().forEach { line ->
                try {
                    if (line.startsWith("Q:")) {
                        val parts = line.split("|")
                        val question = parts[0].replace("Q:", "").trim()
                        val optionsStr = parts[1].replace("Options:", "").trim()
                        val answer = parts[2].replace("Ans:", "").trim()
                        val expl = if (parts.size > 3) parts[3].replace("Expl:", "").trim() else ""
                        quizList.add(
                            Quiz(studyNoteId = noteId, question = question, optionsCsv = optionsStr, answer = answer, explanation = expl)
                        )
                    } else if (line.startsWith("F:")) {
                        val parts = line.split("|")
                        val front = parts[0].replace("F:", "").trim()
                        val back = parts[1].replace("B:", "").trim()
                        flashcardList.add(
                            Flashcard(studyNoteId = noteId, front = front, back = back)
                        )
                    }
                } catch (e: Exception) {
                    // Skip malformed lines
                }
            }

            // Fallback mock contents if parser encounters layout format anomalies
            if (quizList.isEmpty()) {
                quizList.add(Quiz(studyNoteId = noteId, question = "Main concept of $title?", optionsCsv = "Core Definition, Secondary Idea, Outlier Concept, Random", answer = "Core Definition", explanation = "It states the primary study dimension."))
                quizList.add(Quiz(studyNoteId = noteId, question = "Which aspect is critical?", optionsCsv = "Performance, Color styling, Randomizing threads, Memory", answer = "Performance", explanation = "Performance governs scaling."))
            }
            if (flashcardList.isEmpty()) {
                flashcardList.add(Flashcard(studyNoteId = noteId, front = "Defined term in $title", back = "The core pillar of study."))
                flashcardList.add(Flashcard(studyNoteId = noteId, front = "Key takeaway", back = "Always verify edge-cases."))
            }

            repo.insertQuizzes(quizList)
            for (card in flashcardList) {
                repo.insertFlashcard(card)
            }

            _selectedStudyNoteId.value = noteId
            _quizGenerationLoading.value = false
        }
    }

    fun markFlashcardMastered(flashcard: Flashcard) {
        viewModelScope.launch {
            repo.updateFlashcard(flashcard.copy(mastered = true))
        }
    }

    fun deleteStudyNote(noteId: Long) {
        viewModelScope.launch {
            repo.deleteStudyNoteById(noteId)
            repo.deleteQuizzesByNoteId(noteId)
            if (_selectedStudyNoteId.value == noteId) {
                _selectedStudyNoteId.value = null
            }
        }
    }

    // Memories updating
    fun updateMemory(key: String, value: String) {
        viewModelScope.launch {
            repo.insertMemory(AssistantMemory(key, value))
        }
    }

    // Direct Helper: draft messaging styled as personal AI Twin
    private val _draftOutput = MutableStateFlow("")
    val draftOutput: StateFlow<String> = _draftOutput.asStateFlow()

    private val _draftLoading = MutableStateFlow(false)
    val draftLoading: StateFlow<Boolean> = _draftLoading.asStateFlow()

    fun generateDigitalTwinDraft(channel: String, rawInstructions: String) {
        _draftLoading.value = true
        _draftOutput.value = ""
        viewModelScope.launch {
            val memoriesList = repo.allMemoriesFlow.first()
            val styleMemory = memoriesList.firstOrNull { it.key == "writing_style" }?.value ?: "Professional and clean"
            val prompt = """
                Draft a $channel post/email based on this request:
                "$rawInstructions"
                
                You must emulate the user's Writing Style, defined in memories as:
                "$styleMemory"
                
                Requirements:
                - Do not speak AS an assistant. Speak AS the user directly in first-person (e.g. "I'm thrilled to announce...", "I just finished...").
                - Maintain the exact styling guidelines perfectly.
            """.trimIndent()

            val resultText = gemini.generateResponse(prompt, "You are a Digital Twin writer mimicking the user's direct style.")
            _draftOutput.value = resultText
            _draftLoading.value = false
        }
    }

    // Direct Helper: explain a difficult study topic
    private val _explainOutput = MutableStateFlow("")
    val explainOutput: StateFlow<String> = _explainOutput.asStateFlow()

    private val _explainLoading = MutableStateFlow(false)
    val explainLoading: StateFlow<Boolean> = _explainLoading.asStateFlow()

    fun explainDifficultTopic(topicName: String) {
        _explainLoading.value = true
        _explainOutput.value = ""
        viewModelScope.launch {
            val prompt = "Explain the topic '$topicName' in a highly simplified, clear manner with analogies suitable for students preparing for an exam. Include a short 3-point study checklist."
            val result = gemini.generateResponse(prompt, "You are an elite academic personal tutor.")
            _explainOutput.value = result
            _explainLoading.value = false
        }
    }
}
