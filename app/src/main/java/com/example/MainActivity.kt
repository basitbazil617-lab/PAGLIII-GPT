package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserSettings
import com.example.data.repository.FirestoreRepository
import com.example.data.repository.PagliiAiRepository
import com.example.firebase.configureAppCheck
import com.example.ui.components.NeonGradientBackground
import com.example.ui.components.PagliiNeonAvatar
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeworkScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.ToolsScreen
import com.example.ui.screens.VisionScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.PagliiTheme
import com.example.ui.viewmodel.PagliiViewModel
import com.example.voice.VoiceAssistantManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

object Routes {
    const val CHAT = "chat"
    const val VISION = "vision"
    const val HOMEWORK = "homework"
    const val TOOLS = "tools"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val MAIN_TABS = listOf(
    NavTab(Routes.CHAT, "Chat", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    NavTab(Routes.VISION, "Photo AI", Icons.Filled.CameraAlt, Icons.Outlined.CameraAlt),
    NavTab(Routes.HOMEWORK, "Homework", Icons.Filled.MenuBook, Icons.Outlined.MenuBook),
    NavTab(Routes.TOOLS, "AI Tools", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    NavTab(Routes.HISTORY, "History", Icons.Filled.History, Icons.Outlined.History)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory: Configure App Check with launch intent before setContent or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            PagliiAppRoot()
        }
    }
}

@Composable
fun PagliiAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsRepository = remember(context) { UserPreferencesRepository(context.applicationContext) }
    val settings by prefsRepository.settingsFlow.collectAsStateWithLifecycle(initialValue = UserSettings())

    var showSplash by remember { mutableStateOf(true) }
    var welcomeDismissed by remember { mutableStateOf(false) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    PagliiTheme(themeMode = settings.themeMode) {
        when {
            showSplash -> {
                SplashScreen(onFinished = { showSplash = false })
            }
            currentUser == null -> {
                if (!settings.hasSeenWelcome && !welcomeDismissed) {
                    WelcomeScreen(
                        onGetStarted = {
                            welcomeDismissed = true
                            scope.launch { prefsRepository.markWelcomeSeen() }
                        }
                    )
                } else {
                    AuthScreen(
                        onAuthSuccess = {
                            currentUser = Firebase.auth.currentUser
                        }
                    )
                }
            }
            else -> {
                val uid = currentUser!!.uid
                key(uid) {
                    val viewModel: PagliiViewModel = viewModel(
                        key = uid,
                        factory = viewModelFactory {
                            initializer {
                                val app = checkNotNull(this[APPLICATION_KEY]) {
                                    "APPLICATION_KEY missing from CreationExtras"
                                }
                                val databaseId = app.getString(R.string.firestore_database_id)
                                val db = FirebaseFirestore.getInstance(databaseId)
                                PagliiViewModel(
                                    currentUserId = uid,
                                    firestoreRepository = FirestoreRepository(db),
                                    aiRepository = PagliiAiRepository(app),
                                    prefsRepository = UserPreferencesRepository(app)
                                )
                            }
                        }
                    )
                    PagliiMainShell(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun PagliiMainShell(viewModel: PagliiViewModel) {
    val context = LocalContext.current
    val voiceManager = remember(context) { VoiceAssistantManager(context) }
    DisposableEffect(voiceManager) {
        onDispose { voiceManager.shutdown() }
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Routes.CHAT

    val settings by viewModel.userSettings.collectAsStateWithLifecycle()
    val chatTitle by viewModel.activeChatTitle.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val streamingChatReply by viewModel.streamingChatReply.collectAsStateWithLifecycle()
    val isChatGenerating by viewModel.isChatGenerating.collectAsStateWithLifecycle()
    val isLoadingHistory by viewModel.isLoadingHistoryMessages.collectAsStateWithLifecycle()
    val chatError by viewModel.chatError.collectAsStateWithLifecycle()
    val chatAttachedImage by viewModel.chatAttachedImage.collectAsStateWithLifecycle()

    val visionBitmap by viewModel.visionBitmap.collectAsStateWithLifecycle()
    val visionMessages by viewModel.visionMessages.collectAsStateWithLifecycle()
    val visionStreaming by viewModel.visionStreamingReply.collectAsStateWithLifecycle()
    val isVisionGenerating by viewModel.isVisionGenerating.collectAsStateWithLifecycle()
    val visionError by viewModel.visionError.collectAsStateWithLifecycle()

    val homeworkBitmap by viewModel.homeworkBitmap.collectAsStateWithLifecycle()
    val homeworkResult by viewModel.homeworkResult.collectAsStateWithLifecycle()
    val isHomeworkGenerating by viewModel.isHomeworkGenerating.collectAsStateWithLifecycle()
    val homeworkError by viewModel.homeworkError.collectAsStateWithLifecycle()

    val toolResult by viewModel.toolResult.collectAsStateWithLifecycle()
    val isToolGenerating by viewModel.isToolGenerating.collectAsStateWithLifecycle()
    val toolError by viewModel.toolError.collectAsStateWithLifecycle()

    val sessionsState by viewModel.chatSessionsState.collectAsStateWithLifecycle()

    val isSpeaking by voiceManager.isSpeaking.collectAsStateWithLifecycle()
    val isListening by voiceManager.isListening.collectAsStateWithLifecycle()
    val voiceError by voiceManager.voiceError.collectAsStateWithLifecycle()

    val showChrome = currentRoute in MAIN_TABS.map { it.route }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    NeonGradientBackground(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (showChrome && !useRail) {
                        NavigationBar(
                            windowInsets = WindowInsets.navigationBars,
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                        ) {
                            MAIN_TABS.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_${tab.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (showChrome && useRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(modifier = Modifier.padding(top = 12.dp))
                            PagliiNeonAvatar(size = 40.dp)
                            Spacer(modifier = Modifier.padding(top = 16.dp))
                            MAIN_TABS.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag("rail_${tab.route}")
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        NavHost(
                            navController = navController,
                            startDestination = Routes.CHAT
                        ) {
                            composable(Routes.CHAT) {
                                ChatScreen(
                                    chatTitle = chatTitle,
                                    messages = chatMessages,
                                    streamingReply = streamingChatReply,
                                    isGenerating = isChatGenerating,
                                    isLoadingHistory = isLoadingHistory,
                                    chatError = chatError,
                                    attachedBitmap = chatAttachedImage,
                                    preferredLanguage = settings.preferredLanguage,
                                    voiceManager = voiceManager,
                                    isSpeaking = isSpeaking,
                                    isListening = isListening,
                                    voiceError = voiceError,
                                    onSendMessage = { msg ->
                                        viewModel.sendChatMessage(msg) { reply ->
                                            voiceManager.speak(reply, settings.preferredLanguage)
                                        }
                                    },
                                    onAttachImage = { bmp -> viewModel.attachChatImage(bmp) },
                                    onNewChat = { viewModel.startNewChat() },
                                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                                )
                            }

                            composable(Routes.VISION) {
                                VisionScreen(
                                    bitmap = visionBitmap,
                                    messages = visionMessages,
                                    streamingReply = visionStreaming,
                                    isGenerating = isVisionGenerating,
                                    visionError = visionError,
                                    preferredLanguage = settings.preferredLanguage,
                                    voiceManager = voiceManager,
                                    isSpeaking = isSpeaking,
                                    onSetImage = { bmp -> viewModel.setVisionImage(bmp) },
                                    onAnalyzePhoto = { prompt, tag ->
                                        viewModel.analyzeVisionPhoto(prompt, tag)
                                    }
                                )
                            }

                            composable(Routes.HOMEWORK) {
                                HomeworkScreen(
                                    attachedBitmap = homeworkBitmap,
                                    solutionText = homeworkResult,
                                    isGenerating = isHomeworkGenerating,
                                    homeworkError = homeworkError,
                                    preferredLanguage = settings.preferredLanguage,
                                    voiceManager = voiceManager,
                                    isSpeaking = isSpeaking,
                                    onSetImage = { bmp -> viewModel.setHomeworkImage(bmp) },
                                    onSolveHomework = { subject, q, short ->
                                        viewModel.solveHomework(subject, q, short)
                                    }
                                )
                            }

                            composable(Routes.TOOLS) {
                                ToolsScreen(
                                    toolResult = toolResult,
                                    isGenerating = isToolGenerating,
                                    toolError = toolError,
                                    preferredLanguage = settings.preferredLanguage,
                                    voiceManager = voiceManager,
                                    isSpeaking = isSpeaking,
                                    onRunTool = { tool, topic, extra ->
                                        viewModel.runAiTool(tool, topic, extra)
                                    },
                                    onClearOutput = { viewModel.clearToolOutput() }
                                )
                            }

                            composable(Routes.HISTORY) {
                                HistoryScreen(
                                    sessionsState = sessionsState,
                                    onOpenSession = { session ->
                                        viewModel.loadChatFromHistory(session) {
                                            navigateToTab(Routes.CHAT)
                                        }
                                    },
                                    onDeleteSession = { chatId ->
                                        viewModel.deleteChatSession(chatId)
                                    }
                                )
                            }

                            composable(Routes.SETTINGS) {
                                SettingsScreen(
                                    settings = settings,
                                    onThemeChange = { mode -> viewModel.setThemeMode(mode) },
                                    onLanguageChange = { lang -> viewModel.setPreferredLanguage(lang) },
                                    onAutoSpeakChange = { enabled -> viewModel.setAutoSpeak(enabled) },
                                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Routes.ABOUT) {
                                AboutScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
