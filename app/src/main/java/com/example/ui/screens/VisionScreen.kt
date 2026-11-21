package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AssistantLanguage
import com.example.data.model.LocalChatMessage
import com.example.ui.components.ChatMessageBubble
import com.example.ui.components.ErrorBanner
import com.example.ui.components.TypingDotsIndicator
import com.example.ui.components.VoicePlaybackBanner
import com.example.utils.ImageUtils
import com.example.voice.VoiceAssistantManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VisionScreen(
    bitmap: Bitmap?,
    messages: List<LocalChatMessage>,
    streamingReply: String,
    isGenerating: Boolean,
    visionError: String?,
    preferredLanguage: AssistantLanguage,
    voiceManager: VoiceAssistantManager,
    isSpeaking: Boolean,
    onSetImage: (Bitmap?) -> Unit,
    onAnalyzePhoto: (prompt: String, actionTag: String?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var followUpText by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val decoded = ImageUtils.decodeScaledBitmap(context, uri)
                if (decoded != null) {
                    onSetImage(decoded)
                } else {
                    Toast.makeText(context, "Unable to decode selected image.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            scope.launch {
                val decoded = ImageUtils.decodeScaledBitmap(context, uri)
                if (decoded != null) {
                    onSetImage(decoded)
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
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission is required to capture photos.", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCamera() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            val uri = ImageUtils.createCameraImageUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .testTag("vision_screen")
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ),
            title = {
                Column {
                    Text("Photo AI Vision", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "OCR, Handwritten Notes, Diagrams & Translation",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            },
            actions = {
                if (bitmap != null) {
                    IconButton(
                        onClick = { onSetImage(null) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("clear_vision_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear photo"
                        )
                    }
                }
            }
        )

        VoicePlaybackBanner(
            isSpeaking = isSpeaking,
            onStopClick = { voiceManager.stopSpeaking() }
        )

        ErrorBanner(message = visionError)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                if (bitmap == null) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Upload or Capture Any Photo",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Read printed or handwritten text, solve worksheets, explain diagrams & textbook pages, or translate image text.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { launchCamera() },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("vision_camera_button")
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Camera")
                                }
                                FilledTonalButton(
                                    onClick = {
                                        galleryLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("vision_gallery_button")
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Gallery")
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Selected photo for analysis",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(20.dp))
                            )
                        }
                    }
                }
            }

            if (bitmap != null) {
                item {
                    Text(
                        text = "Quick Photo Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val actions = listOf(
                        "Read Text (OCR)" to "Extract and read all printed and handwritten text from this image accurately.",
                        "Answer Questions" to "Find every question visible in this photo and answer each one clearly.",
                        "Explain Textbook Page" to "Explain this textbook page in simple, easy-to-understand language with key points.",
                        "Solve Worksheet" to "Solve all problems shown in this worksheet step by step.",
                        "Explain Diagram / Chart" to "Analyze and explain this diagram, chart, or figure clearly.",
                        "Translate Image Text" to "Extract all text from this image and translate it into ${preferredLanguage.label} (plus English)."
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        actions.forEach { (label, prompt) ->
                            AssistChip(
                                onClick = { onAnalyzePhoto(prompt, label) },
                                enabled = !isGenerating,
                                label = { Text(label) },
                                modifier = Modifier.testTag("vision_action_${label.lowercase().replace(" ", "_")}")
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                ChatMessageBubble(
                    message = msg,
                    isSpeaking = isSpeaking,
                    onSpeak = { text -> voiceManager.speak(text, preferredLanguage) },
                    onStopSpeaking = { voiceManager.stopSpeaking() },
                    onCopiedToast = {
                        Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            if (isGenerating && streamingReply.isNotEmpty()) {
                item(key = "vision_streaming") {
                    ChatMessageBubble(
                        message = LocalChatMessage(
                            id = "vision_stream",
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
                item(key = "vision_loading") {
                    TypingDotsIndicator()
                }
            }
        }

        if (bitmap != null) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = followUpText,
                        onValueChange = { followUpText = it },
                        placeholder = { Text("Ask a follow-up about this photo...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vision_followup_input")
                    )
                    FilledIconButton(
                        onClick = {
                            val q = followUpText
                            followUpText = ""
                            onAnalyzePhoto(q, "Follow-up")
                        },
                        enabled = !isGenerating && followUpText.isNotBlank(),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("vision_send_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Ask about photo"
                        )
                    }
                }
            }
        }
    }
}
