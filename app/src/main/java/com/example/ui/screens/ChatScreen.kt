package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AssistantLanguage
import com.example.data.model.LocalChatMessage
import com.example.ui.components.ChatMessageBubble
import com.example.ui.components.ErrorBanner
import com.example.ui.components.PagliiNeonAvatar
import com.example.ui.components.TypingDotsIndicator
import com.example.ui.components.VoicePlaybackBanner
import com.example.utils.ImageUtils
import com.example.voice.VoiceAssistantManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    chatTitle: String,
    messages: List<LocalChatMessage>,
    streamingReply: String,
    isGenerating: Boolean,
    isLoadingHistory: Boolean,
    chatError: String?,
    attachedBitmap: android.graphics.Bitmap?,
    preferredLanguage: AssistantLanguage,
    voiceManager: VoiceAssistantManager,
    isSpeaking: Boolean,
    isListening: Boolean,
    voiceError: String?,
    onSendMessage: (String) -> Unit,
    onAttachImage: (android.graphics.Bitmap?) -> Unit,
    onNewChat: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = ImageUtils.decodeScaledBitmap(context, uri)
                if (bmp != null) {
                    onAttachImage(bmp)
                } else {
                    Toast.makeText(context, "Could not load selected image.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            scope.launch {
                val bmp = ImageUtils.decodeScaledBitmap(context, uri)
                if (bmp != null) {
                    onAttachImage(bmp)
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = ImageUtils.createCameraImageUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission is needed to take a photo.", Toast.LENGTH_SHORT).show()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceManager.startListening(
                language = preferredLanguage,
                onPartialResult = { partial -> inputText = partial },
                onFinalResult = { finalText -> inputText = finalText }
            )
        } else {
            Toast.makeText(context, "Microphone permission is needed for voice input.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(messages.size, streamingReply) {
        val total = messages.size + if (streamingReply.isNotEmpty()) 1 else 0
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .testTag("chat_screen")
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PagliiNeonAvatar(size = 36.dp, pulse = isGenerating)
                    Column {
                        Text(
                            text = if (chatTitle == "New Chat") "PAGLII GPT" else chatTitle,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${preferredLanguage.label} • Friendly AI ❤️",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            },
            actions = {
                IconButton(
                    onClick = onNewChat,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("new_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddComment,
                        contentDescription = "New chat",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("chat_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        VoicePlaybackBanner(
            isSpeaking = isSpeaking,
            onStopClick = { voiceManager.stopSpeaking() }
        )

        ErrorBanner(message = chatError ?: voiceError)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isLoadingHistory) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                }
            } else if (messages.isEmpty() && !isGenerating) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        PagliiNeonAvatar(size = 86.dp, pulse = true)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Namaste, Salam & Hey! 🤖❤️",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Main hoon PAGLII GPT! Ask me anything in Hinglish, Hindi, Urdu, or English — chat, code, jokes, ya koi bhi sawaal!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    item {
                        val starters = listOf(
                            "Tell me a funny Hinglish joke 😄",
                            "Explain black holes in simple Hindi",
                            "Mujhe Urdu mein ek pyari motivational quote sunao",
                            "Help me plan a productive study routine",
                            "Write a clean Kotlin Jetpack Compose card"
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            starters.forEach { prompt ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                    ),
                                    modifier = Modifier
                                        .clickable { onSendMessage(prompt) }
                                        .testTag("starter_prompt_chip")
                                ) {
                                    Text(
                                        text = prompt,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageBubble(
                            message = msg,
                            isSpeaking = isSpeaking,
                            onSpeak = { text -> voiceManager.speak(text, preferredLanguage) },
                            onStopSpeaking = { voiceManager.stopSpeaking() },
                            onCopiedToast = {
                                Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    if (isGenerating && streamingReply.isNotEmpty()) {
                        item(key = "streaming_bubble") {
                            ChatMessageBubble(
                                message = LocalChatMessage(
                                    id = "streaming",
                                    role = "model",
                                    text = streamingReply
                                ),
                                isSpeaking = false,
                                onSpeak = {},
                                onStopSpeaking = {},
                                onCopiedToast = {}
                            )
                        }
                    } else if (isGenerating) {
                        item(key = "typing_indicator") {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                PagliiNeonAvatar(size = 32.dp, pulse = true)
                                Spacer(modifier = Modifier.width(8.dp))
                                TypingDotsIndicator()
                            }
                        }
                    }
                }
            }
        }

        // Attached photo preview bar
        if (attachedBitmap != null) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            bitmap = attachedBitmap.asImageBitmap(),
                            contentDescription = "Attached image preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Text(
                            text = "Photo attached — ask anything about it!",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    IconButton(
                        onClick = { onAttachImage(null) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove attached photo"
                        )
                    }
                }
            }
        }

        // Composer input bar
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 4.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("chat_gallery_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Attach photo from gallery",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    IconButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                val uri = ImageUtils.createCameraImageUri(context)
                                pendingCameraUri = uri
                                takePictureLauncher.launch(uri)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("chat_camera_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture photo with camera",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (isListening) "Listening..." else "Message Paglii GPT...",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        maxLines = 4,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )

                    IconButton(
                        onClick = {
                            voiceManager.clearVoiceError()
                            if (isListening) {
                                voiceManager.stopListening()
                            } else {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    voiceManager.startListening(
                                        language = preferredLanguage,
                                        onPartialResult = { partial -> inputText = partial },
                                        onFinalResult = { finalText -> inputText = finalText }
                                    )
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(
                                if (isListening) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
                                else Color.Transparent
                            )
                            .testTag("chat_mic_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                            contentDescription = if (isListening) "Stop listening" else "Voice input",
                            tint = if (isListening) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                    }

                    FilledIconButton(
                        onClick = {
                            val msg = inputText
                            inputText = ""
                            onSendMessage(msg)
                        },
                        enabled = !isGenerating && (inputText.isNotBlank() || attachedBitmap != null),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("chat_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send message"
                        )
                    }
                }
            }
        }
    }
}
