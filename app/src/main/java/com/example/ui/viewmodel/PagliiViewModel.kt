package com.example.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AiToolType
import com.example.data.model.AppThemeMode
import com.example.data.model.AssistantLanguage
import com.example.data.model.ChatSession
import com.example.data.model.HomeworkSubject
import com.example.data.model.LocalChatMessage
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserSettings
import com.example.data.repository.FirestoreRepository
import com.example.data.repository.PagliiAiRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class PagliiViewModel(
    private val currentUserId: String,
    private val firestoreRepository: FirestoreRepository,
    private val aiRepository: PagliiAiRepository,
    private val prefsRepository: UserPreferencesRepository
) : ViewModel() {

    val userSettings: StateFlow<UserSettings> = prefsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = UserSettings()
        )

    val userProfile: StateFlow<UserProfile?> = firestoreRepository.observeUserProfile(currentUserId)
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = null
        )

    val chatSessionsState: StateFlow<UiState<List<ChatSession>>> =
        firestoreRepository.observeChatSessions(currentUserId)
            .map<List<ChatSession>, UiState<List<ChatSession>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                emit(UiState.Error(rootError.message ?: "Failed to load chat history."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    // --- Main Chat State ---
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _activeChatTitle = MutableStateFlow("New Chat")
    val activeChatTitle: StateFlow<String> = _activeChatTitle.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<LocalChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<LocalChatMessage>> = _chatMessages.asStateFlow()

    private val _streamingChatReply = MutableStateFlow("")
    val streamingChatReply: StateFlow<String> = _streamingChatReply.asStateFlow()

    private val _isChatGenerating = MutableStateFlow(false)
    val isChatGenerating: StateFlow<Boolean> = _isChatGenerating.asStateFlow()

    private val _chatError = MutableStateFlow<String?>(null)
    val chatError: StateFlow<String?> = _chatError.asStateFlow()

    private val _chatAttachedImage = MutableStateFlow<Bitmap?>(null)
    val chatAttachedImage: StateFlow<Bitmap?> = _chatAttachedImage.asStateFlow()

    private val _isLoadingHistoryMessages = MutableStateFlow(false)
    val isLoadingHistoryMessages: StateFlow<Boolean> = _isLoadingHistoryMessages.asStateFlow()

    // --- Photo Vision Screen State ---
    private val _visionBitmap = MutableStateFlow<Bitmap?>(null)
    val visionBitmap: StateFlow<Bitmap?> = _visionBitmap.asStateFlow()

    private val _visionMessages = MutableStateFlow<List<LocalChatMessage>>(emptyList())
    val visionMessages: StateFlow<List<LocalChatMessage>> = _visionMessages.asStateFlow()

    private val _visionStreamingReply = MutableStateFlow("")
    val visionStreamingReply: StateFlow<String> = _visionStreamingReply.asStateFlow()

    private val _isVisionGenerating = MutableStateFlow(false)
    val isVisionGenerating: StateFlow<Boolean> = _isVisionGenerating.asStateFlow()

    private val _visionError = MutableStateFlow<String?>(null)
    val visionError: StateFlow<String?> = _visionError.asStateFlow()

    private var visionChatId: String? = null

    // --- Homework Solver State ---
    private val _homeworkBitmap = MutableStateFlow<Bitmap?>(null)
    val homeworkBitmap: StateFlow<Bitmap?> = _homeworkBitmap.asStateFlow()

    private val _homeworkResult = MutableStateFlow("")
    val homeworkResult: StateFlow<String> = _homeworkResult.asStateFlow()

    private val _isHomeworkGenerating = MutableStateFlow(false)
    val isHomeworkGenerating: StateFlow<Boolean> = _isHomeworkGenerating.asStateFlow()

    private val _homeworkError = MutableStateFlow<String?>(null)
    val homeworkError: StateFlow<String?> = _homeworkError.asStateFlow()

    // --- AI Tools State ---
    private val _toolResult = MutableStateFlow("")
    val toolResult: StateFlow<String> = _toolResult.asStateFlow()

    private val _isToolGenerating = MutableStateFlow(false)
    val isToolGenerating: StateFlow<Boolean> = _isToolGenerating.asStateFlow()

    private val _toolError = MutableStateFlow<String?>(null)
    val toolError: StateFlow<String?> = _toolError.asStateFlow()

    private var chatJob: Job? = null
    private var visionJob: Job? = null
    private var homeworkJob: Job? = null
    private var toolJob: Job? = null

    init {
        syncUserProfileOnStart()
    }

    private fun syncUserProfileOnStart() {
        viewModelScope.launch {
            val user = Firebase.auth.currentUser ?: return@launch
            val displayName = user.displayName?.takeIf { it.isNotBlank() }
                ?: user.email?.substringBefore("@")
                ?: "Friend"
            firestoreRepository.upsertUserProfile(
                userId = currentUserId,
                displayName = displayName,
                email = user.email,
                preferredLanguage = userSettings.value.preferredLanguage.label
            )
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { prefsRepository.setThemeMode(mode) }
    }

    fun setPreferredLanguage(language: AssistantLanguage) {
        viewModelScope.launch {
            prefsRepository.setPreferredLanguage(language)
            firestoreRepository.updatePreferredLanguage(currentUserId, language.label)
        }
    }

    fun setAutoSpeak(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setAutoSpeak(enabled) }
    }

    fun attachChatImage(bitmap: Bitmap?) {
        _chatAttachedImage.value = bitmap
    }

    fun startNewChat() {
        chatJob?.cancel()
        _activeChatId.value = null
        _activeChatTitle.value = "New Chat"
        _chatMessages.value = emptyList()
        _streamingChatReply.value = ""
        _isChatGenerating.value = false
        _chatError.value = null
        _chatAttachedImage.value = null
    }

    fun sendChatMessage(
        text: String,
        onCompleteSpeak: (String) -> Unit = {}
    ) {
        val image = _chatAttachedImage.value
        val trimmed = text.trim()
        if (trimmed.isEmpty() && image == null) return
        if (_isChatGenerating.value) return

        val promptForDisplay = trimmed.ifEmpty { "Analyze this attached image" }
        val userMessage = LocalChatMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            text = promptForDisplay,
            hasImage = image != null,
            attachedBitmap = image
        )

        val historySnapshot = _chatMessages.value
        _chatMessages.value = historySnapshot + userMessage
        _chatAttachedImage.value = null
        _chatError.value = null
        _streamingChatReply.value = ""
        _isChatGenerating.value = true

        val currentChatId = _activeChatId.value
        val currentTitle = if (currentChatId == null) {
            promptForDisplay.take(48)
        } else {
            _activeChatTitle.value
        }
        if (currentChatId == null) {
            _activeChatTitle.value = currentTitle
        }

        chatJob = viewModelScope.launch {
            val lang = userSettings.value.preferredLanguage
            try {
                var finalReply = ""
                aiRepository.streamChatReply(
                    history = historySnapshot,
                    userPrompt = promptForDisplay,
                    attachedImage = image,
                    language = lang
                ).collect { accumulated ->
                    finalReply = accumulated
                    _streamingChatReply.value = accumulated
                }

                val modelMessage = LocalChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = "model",
                    text = finalReply
                )
                _chatMessages.value = _chatMessages.value + modelMessage
                _streamingChatReply.value = ""
                _isChatGenerating.value = false

                if (userSettings.value.autoSpeakEnabled) {
                    onCompleteSpeak(finalReply)
                }

                val saveResult = firestoreRepository.createOrAppendTurn(
                    userId = currentUserId,
                    existingChatId = _activeChatId.value,
                    title = currentTitle,
                    category = if (image != null) "vision" else "chat",
                    userText = promptForDisplay,
                    modelText = finalReply,
                    hasImage = image != null,
                    toolTag = null,
                    currentMessageCount = historySnapshot.size.toLong()
                )
                saveResult.onSuccess { savedId ->
                    _activeChatId.value = savedId
                }.onFailure { err ->
                    _chatError.value = "Reply shown, but cloud save failed: ${err.localizedMessage}"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isChatGenerating.value = false
                _streamingChatReply.value = ""
                _chatError.value = e.localizedMessage ?: "Failed to reach Paglii GPT. Please try again."
            }
        }
    }

    fun loadChatFromHistory(session: ChatSession, onLoaded: () -> Unit) {
        viewModelScope.launch {
            _isLoadingHistoryMessages.value = true
            _chatError.value = null
            val result = firestoreRepository.loadChatMessages(currentUserId, session.id)
            result.onSuccess { msgs ->
                _activeChatId.value = session.id
                _activeChatTitle.value = session.title.ifBlank { "Saved Chat" }
                _chatMessages.value = msgs.map { msg ->
                    LocalChatMessage(
                        id = msg.id,
                        role = msg.role,
                        text = msg.text,
                        hasImage = msg.hasImage,
                        toolTag = msg.toolTag
                    )
                }
                _streamingChatReply.value = ""
                _isLoadingHistoryMessages.value = false
                onLoaded()
            }.onFailure { err ->
                _isLoadingHistoryMessages.value = false
                _chatError.value = err.localizedMessage ?: "Unable to load messages."
            }
        }
    }

    fun deleteChatSession(chatId: String) {
        viewModelScope.launch {
            firestoreRepository.deleteChatSession(currentUserId, chatId)
            if (_activeChatId.value == chatId) {
                startNewChat()
            }
        }
    }

    // --- Vision / Photo AI ---
    fun setVisionImage(bitmap: Bitmap?) {
        visionJob?.cancel()
        _visionBitmap.value = bitmap
        _visionMessages.value = emptyList()
        _visionStreamingReply.value = ""
        _isVisionGenerating.value = false
        _visionError.value = null
        visionChatId = null
    }

    fun analyzeVisionPhoto(prompt: String, actionTag: String? = null) {
        val bitmap = _visionBitmap.value
        if (bitmap == null) {
            _visionError.value = "Please capture or select a photo first."
            return
        }
        val cleanPrompt = prompt.trim().ifEmpty { "Explain everything shown in this image clearly." }
        if (_isVisionGenerating.value) return

        val previousTurns = _visionMessages.value
        val userMsg = LocalChatMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            text = cleanPrompt,
            hasImage = previousTurns.isEmpty(),
            toolTag = actionTag
        )
        _visionMessages.value = previousTurns + userMsg
        _visionStreamingReply.value = ""
        _visionError.value = null
        _isVisionGenerating.value = true

        visionJob = viewModelScope.launch {
            val lang = userSettings.value.preferredLanguage
            try {
                var finalReply = ""
                aiRepository.streamVisionAnalysis(
                    image = bitmap,
                    prompt = cleanPrompt,
                    previousTurns = previousTurns,
                    language = lang
                ).collect { accumulated ->
                    finalReply = accumulated
                    _visionStreamingReply.value = accumulated
                }
                _visionMessages.value = _visionMessages.value + LocalChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = "model",
                    text = finalReply,
                    toolTag = actionTag
                )
                _visionStreamingReply.value = ""
                _isVisionGenerating.value = false

                val title = actionTag?.let { "$it: ${cleanPrompt.take(36)}" } ?: "Photo AI: ${cleanPrompt.take(36)}"
                firestoreRepository.createOrAppendTurn(
                    userId = currentUserId,
                    existingChatId = visionChatId,
                    title = title,
                    category = "vision",
                    userText = cleanPrompt,
                    modelText = finalReply,
                    hasImage = true,
                    toolTag = actionTag ?: "Photo AI",
                    currentMessageCount = previousTurns.size.toLong()
                ).onSuccess { savedId -> visionChatId = savedId }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isVisionGenerating.value = false
                _visionStreamingReply.value = ""
                _visionError.value = e.localizedMessage ?: "Failed to analyze photo."
            }
        }
    }

    // --- Homework Solver ---
    fun setHomeworkImage(bitmap: Bitmap?) {
        _homeworkBitmap.value = bitmap
    }

    fun solveHomework(
        subject: HomeworkSubject,
        questionText: String,
        preferShortAnswer: Boolean
    ) {
        val image = _homeworkBitmap.value
        if (questionText.isBlank() && image == null) {
            _homeworkError.value = "Type a homework question or attach a photo to solve."
            return
        }
        if (_isHomeworkGenerating.value) return

        _homeworkError.value = null
        _homeworkResult.value = ""
        _isHomeworkGenerating.value = true

        homeworkJob = viewModelScope.launch {
            val lang = userSettings.value.preferredLanguage
            try {
                var finalAnswer = ""
                aiRepository.streamHomeworkSolution(
                    subject = subject,
                    questionText = questionText,
                    image = image,
                    preferShortAnswer = preferShortAnswer,
                    language = lang
                ).collect { accumulated ->
                    finalAnswer = accumulated
                    _homeworkResult.value = accumulated
                }
                _isHomeworkGenerating.value = false

                val summaryPrompt = questionText.ifBlank { "${subject.label} (Photo)" }
                firestoreRepository.createOrAppendTurn(
                    userId = currentUserId,
                    existingChatId = null,
                    title = "${subject.label}: ${summaryPrompt.take(40)}",
                    category = "homework",
                    userText = summaryPrompt,
                    modelText = finalAnswer,
                    hasImage = image != null,
                    toolTag = subject.label,
                    currentMessageCount = 0L
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isHomeworkGenerating.value = false
                _homeworkError.value = e.localizedMessage ?: "Failed to solve homework."
            }
        }
    }

    // --- AI Tools ---
    fun clearToolOutput() {
        toolJob?.cancel()
        _toolResult.value = ""
        _toolError.value = null
        _isToolGenerating.value = false
    }

    fun runAiTool(tool: AiToolType, topic: String, extraContext: String) {
        if (topic.isBlank()) {
            _toolError.value = "Please enter what you want ${tool.title} to create."
            return
        }
        if (_isToolGenerating.value) return

        _toolError.value = null
        _toolResult.value = ""
        _isToolGenerating.value = true

        toolJob = viewModelScope.launch {
            val lang = userSettings.value.preferredLanguage
            try {
                var finalOutput = ""
                aiRepository.streamToolGeneration(
                    tool = tool,
                    topic = topic,
                    extraContext = extraContext,
                    language = lang
                ).collect { accumulated ->
                    finalOutput = accumulated
                    _toolResult.value = accumulated
                }
                _isToolGenerating.value = false

                firestoreRepository.createOrAppendTurn(
                    userId = currentUserId,
                    existingChatId = null,
                    title = "${tool.title}: ${topic.take(40)}",
                    category = "tools",
                    userText = topic,
                    modelText = finalOutput,
                    hasImage = false,
                    toolTag = tool.title,
                    currentMessageCount = 0L
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isToolGenerating.value = false
                _toolError.value = e.localizedMessage ?: "Failed to generate output."
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5000L
    }
}
