package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aura.data.*
import com.example.aura.viewmodel.AuraViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AuraAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuraAppScreen(viewModel: AuraViewModel = viewModel()) {
    var selectedTab by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val proactiveAlerts by viewModel.proactiveSuggestions.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(10.dp),
                            color = CyberSecondary,
                            shape = CircleShape
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AURA PERSONAL AI",
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = CyberBackground
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = CyberSurface,
                modifier = Modifier.navigationBarsPadding()
            ) {
                val tabs = listOf(
                    Triple("Assistant", Icons.Default.Chat, 0),
                    Triple("Study", Icons.Default.MenuBook, 1),
                    Triple("Twin", Icons.Default.ManageAccounts, 2),
                    Triple("Mood", Icons.Default.LightMode, 3),
                    Triple("Habits", Icons.Default.CheckCircle, 4)
                )
                tabs.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyberSecondary,
                            selectedTextColor = CyberSecondary,
                            unselectedIconColor = CyberMuted,
                            unselectedTextColor = CyberMuted,
                            indicatorColor = CyberSurface.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        },
        containerColor = CyberBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Proactive critical notification banner if suggestions exist
            if (proactiveAlerts.isNotEmpty()) {
                val systemAlert = proactiveAlerts.first()
                Surface(
                    color = CyberSurface.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .border(1.dp, CyberSecondary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.BatteryAlert,
                            contentDescription = "Alert",
                            tint = CyberAccentAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = systemAlert,
                            color = CyberText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "TabTransition",
                modifier = Modifier.weight(1f)
            ) { targetTab ->
                when (targetTab) {
                    0 -> AssistantDashboard(viewModel)
                    1 -> StudyCompanionTab(viewModel)
                    2 -> DigitalTwinTab(viewModel)
                    3 -> MoodTab(viewModel)
                    4 -> HabitsTab(viewModel)
                }
            }
        }
    }
}

// ==================== TABS IMPLEMENTATIONS ====================

// --- 1. ASSISTANT DASHBOARD TAB ---
@Composable
fun AssistantDashboard(viewModel: AuraViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isLoading by viewModel.chatLoading.collectAsStateWithLifecycle()
    val remindersList by viewModel.reminders.collectAsStateWithLifecycle()

    var textInput by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberScrollState()

    // Aura Breathing Orb Animation Setup
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransition")
    val multiplier by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbPulse"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Upper Dashboard Banner + Core
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Glow Interactive Canvas Core
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clickable {
                        viewModel.sendMessage("Analyze my daily habits context and give me a proactive advice.")
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(65.dp)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(CyberSecondary, CyberPrimary.copy(alpha = 0.6f), Color.Transparent),
                            center = center,
                            radius = size.width * (0.5f * multiplier)
                        )
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = size.width * 0.15f,
                        center = center
                    )
                }
                // Thin orbital boundary lines
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = CyberSecondary.copy(alpha = 0.4f),
                        style = Stroke(width = 1.dp.toPx()),
                        radius = (size.width / 2.2f) * multiplier
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Aura Twin Core",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = "System Online • Fully Synchronized",
                    fontSize = 12.sp,
                    color = CyberMuted
                )
                Text(
                    text = "Tip: Tap Aura to trigger proactive self-analysis.",
                    fontSize = 11.sp,
                    color = CyberSecondary.copy(alpha = 0.8f)
                )
            }
        }

        Divider(color = CyberSurface, modifier = Modifier.padding(bottom = 12.dp))

        // Chat Conversation box
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CyberSurface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Message List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                ) {
                    val messageScrollState = rememberScrollState()
                    LaunchedEffect(messages.size, isLoading) {
                        messageScrollState.animateScrollTo(messageScrollState.maxValue)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(messageScrollState)
                    ) {
                        messages.forEach { msg ->
                            val isUser = msg.sender == "User"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    color = if (isUser) CyberPrimary.copy(alpha = 0.85f) else CyberSurface,
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isUser) 16.dp else 0.dp,
                                        bottomEnd = if (isUser) 0.dp else 16.dp
                                    ),
                                    border = if (isUser) null else BorderStroke(1.dp, CyberSecondary.copy(alpha = 0.2f)),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = msg.sender,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isUser) Color.White.copy(alpha = 0.8f) else CyberSecondary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = msg.text,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        if (isLoading) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Surface(
                                    color = CyberSurface,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, CyberSecondary.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = CyberSecondary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Aura is thinking...", fontSize = 12.sp, color = CyberMuted)
                                    }
                                }
                            }
                        }
                    }
                }

                Divider(color = CyberSurface)

                // Input bar
                val keyboard = LocalSoftwareKeyboardController.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Ask Aura / Enter command...", color = CyberMuted, fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text)
                    )

                    IconButton(
                        onClick = {
                            if (textInput.trim().isNotEmpty()) {
                                viewModel.sendMessage(textInput)
                                textInput = ""
                                keyboard?.hide()
                            }
                        },
                        modifier = Modifier
                            .testTag("send_message_button")
                            .minimumInteractiveComponentSize(),
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (textInput.trim().isEmpty()) CyberMuted else CyberSecondary
                        )
                    }
                }
            }
        }

        // Quick Active Reminders Block
        Text(
            text = "Active Daily Reminders",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var reminderText by remember { mutableStateOf("") }
            var reminderTime by remember { mutableStateOf("") }

            TextField(
                value = reminderText,
                onValueChange = { reminderText = it },
                placeholder = { Text("Study DSA...", fontSize = 12.sp, color = CyberMuted) },
                modifier = Modifier.weight(1.5f),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface,
                    focusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )

            TextField(
                value = reminderTime,
                onValueChange = { reminderTime = it },
                placeholder = { Text("7 PM", fontSize = 12.sp, color = CyberMuted) },
                modifier = Modifier.weight(0.8f),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface,
                    focusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )

            Button(
                onClick = {
                    if (reminderText.isNotEmpty()) {
                        viewModel.addReminder(reminderText, reminderTime.ifEmpty { "Today" })
                        reminderText = ""
                        reminderTime = ""
                    }
                },
                modifier = Modifier
                    .testTag("add_reminder_button")
                    .height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary)
            ) {
                Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(0.6f)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(remindersList) { item ->
                Surface(
                    color = CyberSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, CyberSurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = item.isCompleted,
                            onCheckedChange = { viewModel.toggleReminder(item) },
                            colors = CheckboxDefaults.colors(checkedColor = CyberSecondary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.text,
                                fontSize = 13.sp,
                                color = if (item.isCompleted) CyberMuted else Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.timeLabel,
                                fontSize = 11.sp,
                                color = CyberSecondary
                            )
                        }
                        IconButton(
                            onClick = { viewModel.deleteReminder(item) }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = CyberMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// --- 2. STUDY COMPANION TAB ---
@Composable
fun StudyCompanionTab(viewModel: AuraViewModel) {
    val notes by viewModel.studyNotes.collectAsStateWithLifecycle()
    val noteIdSelected by viewModel.selectedStudyNoteId.collectAsStateWithLifecycle()
    val quizzesList by viewModel.currentQuizzes.collectAsStateWithLifecycle()
    val flashcardsList by viewModel.currentFlashcards.collectAsStateWithLifecycle()

    val generatorLoading by viewModel.quizGenerationLoading.collectAsStateWithLifecycle()

    // Editor inputs
    var noteTitle by remember { mutableStateOf("") }
    var noteBody by remember { mutableStateOf("") }

    // Direct Academic explainer inputs
    var topicToExplain by remember { mutableStateOf("") }
    val explainText by viewModel.explainOutput.collectAsStateWithLifecycle()
    val explainLoading by viewModel.explainLoading.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "AI Student Tutor",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Segment 1: Quick Concept Explainer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Smart Tutor Explain Topic", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CyberSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = topicToExplain,
                        onValueChange = { topicToExplain = it },
                        placeholder = { Text("e.g. Normalization BCNF vs 3NF", fontSize = 12.sp, color = CyberMuted) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CyberSurface,
                            unfocusedContainerColor = CyberSurface,
                            focusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )

                    Button(
                        onClick = {
                            if (topicToExplain.trim().isNotEmpty()) {
                                viewModel.explainDifficultTopic(topicToExplain)
                            }
                        },
                        enabled = !explainLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimary)
                    ) {
                        Text("Explain", fontSize = 12.sp)
                    }
                }

                if (explainLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally).padding(top = 12.dp), color = CyberSecondary)
                }

                if (explainText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = CyberSurface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = explainText,
                            fontSize = 12.sp,
                            color = Color.White,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // Segment 2: Notes upload and AI Flashcard/Quiz generator
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Upload Notes & Generate Quiz/Cards", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CyberSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    placeholder = { Text("Title e.g. DBMS Indexes", fontSize = 12.sp, color = CyberMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(focusedContainerColor = CyberBackground, unfocusedContainerColor = CyberBackground, focusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = noteBody,
                    onValueChange = { noteBody = it },
                    placeholder = { Text("Paste your study notes content here. Aura AI will draft quizzes and flashcards for active recall study...", fontSize = 12.sp, color = CyberMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    colors = TextFieldDefaults.colors(focusedContainerColor = CyberBackground, unfocusedContainerColor = CyberBackground, focusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (noteTitle.isNotEmpty() && noteBody.isNotEmpty()) {
                            viewModel.addStudyNoteAndGenerateActivities(noteTitle, noteBody)
                            noteTitle = ""
                            noteBody = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_note_button"),
                    enabled = !generatorLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary)
                ) {
                    if (generatorLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generating content via Aura AI...", fontSize = 12.sp)
                    } else {
                        Text("Parse & Build Playable Quizzes", fontSize = 12.sp)
                    }
                }
            }
        }

        // Notes Stack List
        Text("Your Study Notes Library", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White, modifier = Modifier.padding(vertical = 8.dp))

        if (notes.isEmpty()) {
            Text("No notes found. Enter title and study material above to generate AI content.", fontSize = 12.sp, color = CyberMuted, modifier = Modifier.padding(vertical = 12.dp))
        }

        notes.forEach { note ->
            val isSelected = noteIdSelected == note.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { viewModel.selectStudyNoteId(if (isSelected) null else note.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) CyberPrimary.copy(alpha = 0.2f) else CyberSurface
                ),
                border = BorderStroke(1.dp, if (isSelected) CyberPrimary else Color.Transparent)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = CyberSecondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(note.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.deleteStudyNote(note.id) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CyberMuted, modifier = Modifier.size(16.dp))
                        }
                    }

                    if (isSelected) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(note.content, fontSize = 12.sp, color = CyberText, modifier = Modifier.padding(vertical = 4.dp))

                        Divider(color = CyberSurface, modifier = Modifier.padding(vertical = 8.dp))

                        // INTERACTIVE FLASHCARDS ROW
                        Text("Flip Flashcards (Active recall)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CyberSecondary)
                        Spacer(modifier = Modifier.height(4.dp))

                        if (flashcardsList.isEmpty()) {
                            Text("No cards generated for this note.", fontSize = 11.sp, color = CyberMuted)
                        } else {
                            // Flashcards swipe view
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                flashcardsList.take(3).forEachIndexed { i, card ->
                                    var flipped by remember(card.id) { mutableStateOf(false) }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(85.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (flipped) CyberSecondary.copy(alpha = 0.15f) else CyberSurface)
                                            .border(1.dp, if (card.mastered) CyberAccentGreen else CyberSurface)
                                            .clickable { flipped = !flipped },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (flipped) card.back else card.front,
                                            fontSize = 11.sp,
                                            color = if (flipped) CyberSecondary else Color.White,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(4.dp),
                                            fontWeight = FontWeight.Medium
                                        )

                                        // Tiny mastered icon checked in corner
                                        if (flipped && !card.mastered) {
                                            IconButton(
                                                onClick = { viewModel.markFlashcardMastered(card) },
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .size(22.dp)
                                            ) {
                                                Icon(Icons.Default.Check, "Mastered", tint = CyberAccentGreen, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // INTERACTIVE QUIZZES DECK
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Play Generated Quiz", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CyberSecondary)

                        if (quizzesList.isEmpty()) {
                            Text("No quiz generated.", fontSize = 11.sp, color = CyberMuted)
                        } else {
                            quizzesList.forEachIndexed { qIdx, quiz ->
                                var selectedAns by remember(quiz.id) { mutableStateOf("") }
                                val optionsList = quiz.optionsCsv.split(",")

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text("${qIdx + 1}. ${quiz.question}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    optionsList.forEach { opt ->
                                        val trimmedOpt = opt.trim()
                                        val isCorrect = trimmedOpt.equals(quiz.answer.trim(), ignoreCase = true)
                                        val isChosen = trimmedOpt == selectedAns

                                        val blockColor = when {
                                            selectedAns.isEmpty() -> CyberBackground
                                            isCorrect -> CyberAccentGreen.copy(alpha = 0.2f)
                                            isChosen && !isCorrect -> CyberTertiary.copy(alpha = 0.2f)
                                            else -> CyberBackground
                                        }

                                        val blockBorder = when {
                                            selectedAns.isEmpty() -> BorderStroke(1.dp, CyberSurface)
                                            isCorrect -> BorderStroke(1.dp, CyberAccentGreen)
                                            isChosen && !isCorrect -> BorderStroke(1.dp, CyberTertiary)
                                            else -> BorderStroke(1.dp, CyberSurface)
                                        }

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .clickable(enabled = selectedAns.isEmpty()) {
                                                    selectedAns = trimmedOpt
                                                },
                                            shape = RoundedCornerShape(8.dp),
                                            color = blockColor,
                                            border = blockBorder
                                        ) {
                                            Text(
                                                trimmedOpt,
                                                fontSize = 11.sp,
                                                color = Color.White,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }

                                    if (selectedAns.isNotEmpty()) {
                                        Text(
                                            text = "Aura Feedback: " + (if (selectedAns.equals(quiz.answer.trim(), ignoreCase = true)) "Correct! " else "Incorrect. Correct is: ${quiz.answer}. ") + quiz.explanation,
                                            fontSize = 10.sp,
                                            color = CyberSecondary,
                                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- 3. DIGITAL TWIN TAB ---
@Composable
fun DigitalTwinTab(viewModel: AuraViewModel) {
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    var instructionInput by remember { mutableStateOf("") }
    var channelSelected by remember { mutableStateOf("LinkedIn") } // LinkedIn, X/Twitter, Email

    val draftOutput by viewModel.draftOutput.collectAsStateWithLifecycle()
    val draftLoading by viewModel.draftLoading.collectAsStateWithLifecycle()

    var showMemoryEditDialog by remember { mutableStateOf(false) }
    var activeMemoryKey by remember { mutableStateOf("") }
    var activeMemoryVal by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Digital Twin & Persona",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Learnt Memory Profile Cards
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "Aura AI Learnt Contexts (Real Memory)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = CyberSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                memories.forEach { memory ->
                    val readableName = when (memory.key) {
                        "sleep_schedule" -> "🌙 Sleep schedule"
                        "study_habits" -> "📚 Study habits & slots"
                        "interests_goals" -> "🎯 Interests and Goals"
                        "writing_style" -> "✍️ Writing & Expression Style"
                        else -> memory.key
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                activeMemoryKey = memory.key
                                activeMemoryVal = memory.value
                                showMemoryEditDialog = true
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = CyberBackground,
                        border = BorderStroke(1.dp, CyberSurface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(readableName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CyberSecondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(memory.value, fontSize = 12.sp, color = Color.White)
                            }
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CyberMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Digital Twin Auto-Writer Drafts
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "Twin Writer: Emulated Social Drafts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = CyberSecondary
                )
                Text(
                    "Aura writes exact personal copies adhering strictly to your writing style.",
                    fontSize = 11.sp,
                    color = CyberMuted,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val channels = listOf("LinkedIn", "X / Twitter", "Email")
                    channels.forEach { channel ->
                        val isSelected = channelSelected == channel
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clickable { channelSelected = channel },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) CyberPrimary else CyberBackground,
                            border = BorderStroke(1.dp, if (isSelected) CyberPrimary else CyberSurface)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(channel, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = instructionInput,
                    onValueChange = { instructionInput = it },
                    placeholder = { Text("Describe the post. e.g. Create a LinkedIn post from this Cloud Architect certificate...", fontSize = 12.sp, color = CyberMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CyberBackground,
                        unfocusedContainerColor = CyberBackground,
                        focusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (instructionInput.isNotEmpty()) {
                            viewModel.generateDigitalTwinDraft(channelSelected, instructionInput)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("generate_twin_draft_button"),
                    enabled = !draftLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary)
                ) {
                    if (draftLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mimicking writing style...")
                    } else {
                        Text("Draft in twin voice", fontSize = 12.sp)
                    }
                }

                if (draftOutput.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Generated Draft Copy", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CyberSecondary)
                    Surface(
                        color = CyberBackground,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        border = BorderStroke(1.dp, CyberSurface)
                    ) {
                        Text(
                            text = draftOutput,
                            fontSize = 12.sp,
                            color = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal Edit dialog
    if (showMemoryEditDialog) {
        AlertDialog(
            onDismissRequest = { showMemoryEditDialog = false },
            title = { Text("Update Aura Memory context", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = activeMemoryVal,
                    onValueChange = { activeMemoryVal = it },
                    label = { Text("Learnt description", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateMemory(activeMemoryKey, activeMemoryVal)
                        showMemoryEditDialog = false
                    }
                ) {
                    Text("Save Memory", color = CyberSecondary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMemoryEditDialog = false }) {
                    Text("Cancel", color = CyberMuted)
                }
            }
        )
    }
}

// --- 4. MOOD TAB ---
@Composable
fun MoodTab(viewModel: AuraViewModel) {
    val moodHistory by viewModel.moodLogs.collectAsStateWithLifecycle()

    var moodSliderVal by remember { mutableStateOf(3f) } // 1.Terrible 2.Bad 3.Neutral 4.Good 5.Awesome
    var journalEntry by remember { mutableStateOf("") }
    var mockWaveTrigger by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val moodName = when (moodSliderVal.toInt()) {
        1 -> "😭 Terrible"
        2 -> "🙁 Bad"
        3 -> "😐 Neutral"
        4 -> "🙂 Good"
        5 -> "🤩 Awesome"
        else -> "Neutral"
    }

    val moodAccentColor = when (moodSliderVal.toInt()) {
        1 -> Color(0xFFEF5350)
        2 -> Color(0xFFFF9800)
        3 -> Color(0xFF9E9E9E)
        4 -> Color(0xFF00E676)
        5 -> Color(0xFF00B0FF)
        else -> CyberSecondary
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Mood Tracker & Well-being",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Visual Slider Logger Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("How are you feeling right now?", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                
                Spacer(modifier = Modifier.height(12.dp))

                Text(moodName, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = moodAccentColor)

                Slider(
                    value = moodSliderVal,
                    onValueChange = { moodSliderVal = it },
                    valueRange = 1f..5f,
                    steps = 3,
                    colors = SliderDefaults.colors(
                        thumbColor = moodAccentColor,
                        activeTrackColor = moodAccentColor,
                        inactiveTrackColor = CyberBackground
                    ),
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .testTag("mood_slider")
                )

                // Simulated Voice Waveform Indicator
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(35.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberBackground)
                        .clickable { mockWaveTrigger = !mockWaveTrigger },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice input", tint = if (mockWaveTrigger) CyberSecondary else CyberMuted, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (mockWaveTrigger) "Voice mood scanning in progress..." else "Tap to trigger Voice Sentiment analysis",
                        fontSize = 11.sp,
                        color = if (mockWaveTrigger) CyberSecondary else CyberMuted
                    )
                }

                if (mockWaveTrigger) {
                    val pulseTransition = rememberInfiniteTransition()
                    val waveHeight by pulseTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse)
                    )
                    Canvas(modifier = Modifier.fillMaxWidth().height(25.dp).padding(vertical = 4.dp)) {
                        for (i in 0..15) {
                            val h = sin((i + waveHeight) * 0.9f) * 15f
                            drawLine(
                                color = CyberSecondary,
                                start = Offset(x = i * 25f + 100f, y = 12.5f - h / 2),
                                end = Offset(x = i * 25f + 100f, y = 12.5f + h / 2),
                                strokeWidth = 3.dp.toPx()
                            )
                        }
                    }
                }

                // Journal Text Box
                Spacer(modifier = Modifier.height(12.dp))
                TextField(
                    value = journalEntry,
                    onValueChange = { journalEntry = it },
                    placeholder = { Text("What made you feel this way? Write your mental wellness journal entry...", fontSize = 12.sp, color = CyberMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CyberBackground,
                        unfocusedContainerColor = CyberBackground,
                        focusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (journalEntry.isNotEmpty()) {
                            viewModel.addMoodLog(moodSliderVal.toInt(), journalEntry)
                            journalEntry = ""
                            mockWaveTrigger = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_mood_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary)
                ) {
                    Text("Log Mood and Check In")
                }
            }
        }

        // Segment 2: Activity and Audio Music recommendation chips based on latest mood log
        val latestLog = moodHistory.firstOrNull()
        if (latestLog != null) {
            val suggestionsList = when (latestLog.feelingScore) {
                1, 2 -> listOf("🧘 Diaphragmatic Loop Breathing", "☕ Hot Camomile Green Tea", "🎵 Deep Ambient Lofi Track")
                3 -> listOf("🚶 15m Brisk Outdoors walking", "📚 Read 1 Chapter of DSA", "🎵 Dynamic Synthwave Beats")
                4, 5 -> listOf("🏃 High-intensity HIIT blast", "🧩 Code 1 Leetcode problem", "🎵 Uplifting House Dance playlist")
                else -> listOf("🧘 Grounding Breathing", "🎵 Ambient Music")
            }

            Text("Aura AI Generated Activity suggestions", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                suggestionsList.forEach { chip ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurface)
                            .border(1.dp, CyberSecondary.copy(alpha = 0.3f))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(chip, fontSize = 10.sp, color = Color.White, textAlign = TextAlign.Center, maxLines = 2, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Logs History
        Text("Mood History & Well-being Cards", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White, modifier = Modifier.padding(vertical = 8.dp))
        moodHistory.forEach { mLog ->
            val emoji = when (mLog.feelingScore) {
                1 -> "😭"
                2 -> "🙁"
                3 -> "😐"
                4 -> "🙂"
                5 -> "🤩"
                else -> "😐"
            }
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = CyberSurface
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(mLog.journalText, fontSize = 12.sp, color = Color.White)
                        val dateString = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(mLog.timestamp))
                        Text(dateString, fontSize = 10.sp, color = CyberMuted, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

// --- 5. HABITS TAB ---
@Composable
fun HabitsTab(viewModel: AuraViewModel) {
    val habitsList by viewModel.habits.collectAsStateWithLifecycle()

    var habitName by remember { mutableStateOf("") }
    var habitCategory by remember { mutableStateOf("Study") } // Study, Health, App Limit, Personal
    var trackingLimit by remember { mutableStateOf("") } // float string for limit check-ins

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "AI Adaptive Habit Builder",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Custom Habit Generator Editor
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberSurface.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Create new Habit Challenge", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CyberSecondary)
                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = habitName,
                    onValueChange = { habitName = it },
                    placeholder = { Text("e.g. Study DSA recursion...", fontSize = 12.sp, color = CyberMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CyberBackground,
                        unfocusedContainerColor = CyberBackground,
                        focusedTextColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )

                // Category selector Row
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val cats = listOf("Study", "Health", "App Limit", "Personal")
                    cats.forEach { cat ->
                        val selected = habitCategory == cat
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clickable { habitCategory = cat },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) CyberSecondary else CyberBackground,
                            border = BorderStroke(1.dp, if (selected) CyberSecondary else CyberSurface)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    cat,
                                    fontSize = 10.sp,
                                    color = if (selected) CyberBackground else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (habitCategory == "App Limit") {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = trackingLimit,
                        onValueChange = { trackingLimit = it },
                        placeholder = { Text("App screening daily hours limit. e.g. 1.0", fontSize = 12.sp, color = CyberMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = CyberBackground,
                            unfocusedContainerColor = CyberBackground,
                            focusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (habitName.isNotEmpty()) {
                            val limitVal = if (habitCategory == "App Limit") trackingLimit.toFloatOrNull() ?: 0f else 0f
                            viewModel.addHabit(habitName, habitCategory, limitVal)
                            habitName = ""
                            trackingLimit = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_habit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary)
                ) {
                    Text("Lock Challenge Streak", fontSize = 12.sp)
                }
            }
        }

        // Active list Deck
        Text("Routine Challenges (Gamified Streaks)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))

        habitsList.forEach { hItem ->
            val isAppLimit = hItem.category == "App Limit"
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            color = when (hItem.category) {
                                "Study" -> CyberSecondary
                                "Health" -> CyberAccentGreen
                                "App Limit" -> CyberTertiary
                                "Personal" -> CyberAccentAmber
                                else -> Color.White
                            },
                            shape = CircleShape
                        ) {}
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(hItem.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White, modifier = Modifier.weight(1f))
                        Text("🔥 ${hItem.streak} day streak", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CyberAccentAmber)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isAppLimit) {
                        Text(
                            text = "Aura Tracker: usage spent today: 180 min (Limit set: ${hItem.dailyLimitHours} h)",
                            fontSize = 11.sp,
                            color = if (180 > (hItem.dailyLimitHours * 60)) CyberTertiary else CyberAccentGreen
                        )
                    } else {
                        val progress = (hItem.streak % 7f) / 7f
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = CyberSecondary,
                                trackColor = CyberBackground
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("${(progress * 100).toInt()}% towards next level", fontSize = 10.sp, color = CyberMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { viewModel.resetHabit(hItem) }
                        ) {
                            Text("Reset", fontSize = 11.sp, color = CyberMuted)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = { viewModel.deleteHabit(hItem) }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = CyberMuted, modifier = Modifier.size(16.dp))
                        }

                        if (!isAppLimit) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = { viewModel.checkInHabit(hItem) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberSecondary.copy(alpha = 0.2f), contentColor = CyberSecondary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Check In", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
