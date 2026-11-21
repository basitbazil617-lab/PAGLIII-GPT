package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AiToolType
import com.example.data.model.AssistantLanguage
import com.example.ui.components.ErrorBanner
import com.example.ui.components.TypingDotsIndicator
import com.example.ui.components.VoicePlaybackBanner
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.utils.ImageUtils
import com.example.voice.VoiceAssistantManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    toolResult: String,
    isGenerating: Boolean,
    toolError: String?,
    preferredLanguage: AssistantLanguage,
    voiceManager: VoiceAssistantManager,
    isSpeaking: Boolean,
    onRunTool: (AiToolType, String, String) -> Unit,
    onClearOutput: () -> Unit
) {
    var selectedTool by remember { mutableStateOf<AiToolType?>(null) }
    var topicInput by remember { mutableStateOf("") }
    var extraContext by remember { mutableStateOf("") }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    if (selectedTool != null) {
        BackHandler {
            selectedTool = null
            topicInput = ""
            extraContext = ""
            onClearOutput()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tools_screen")
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ),
            navigationIcon = {
                if (selectedTool != null) {
                    IconButton(
                        onClick = {
                            selectedTool = null
                            topicInput = ""
                            extraContext = ""
                            onClearOutput()
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("tools_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to AI Tools"
                        )
                    }
                }
            },
            title = {
                Column {
                    Text(
                        text = selectedTool?.title ?: "AI Creator & Dev Tools",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = selectedTool?.description ?: "8 specialized assistants powered by Gemini",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        )

        VoicePlaybackBanner(
            isSpeaking = isSpeaking,
            onStopClick = { voiceManager.stopSpeaking() }
        )

        ErrorBanner(message = toolError)

        val currentTool = selectedTool
        if (currentTool == null) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(AiToolType.entries, key = { it.name }) { tool ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTool = tool
                                onClearOutput()
                            }
                            .testTag("tool_card_${tool.name.lowercase()}")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = toolIcon(tool),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(9.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = tool.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tool.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = topicInput,
                        onValueChange = { topicInput = it },
                        label = { Text("What should ${currentTool.title} create?") },
                        placeholder = { Text(currentTool.inputHint) },
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tool_topic_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = extraContext,
                        onValueChange = { extraContext = it },
                        label = { Text("Tone / Language / Extra Details (Optional)") },
                        placeholder = { Text("e.g., Funny Hinglish vibe, target audience, Kotlin Compose...") },
                        maxLines = 3,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tool_context_input")
                    )
                }

                item {
                    Button(
                        onClick = { onRunTool(currentTool, topicInput, extraContext) },
                        enabled = !isGenerating && topicInput.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("tool_generate_button")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isGenerating) "Generating..." else "Generate Now",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isGenerating && toolResult.isEmpty()) {
                    item { TypingDotsIndicator() }
                }

                if (toolResult.isNotEmpty()) {
                    item {
                        val isCodeTool = currentTool == AiToolType.CODE ||
                            currentTool == AiToolType.WEBSITE ||
                            currentTool == AiToolType.APP_DEV

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Generated Output",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row {
                                        IconButton(
                                            onClick = {
                                                clipboard.setText(AnnotatedString(toolResult))
                                                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy result")
                                        }
                                        IconButton(
                                            onClick = {
                                                ImageUtils.shareText(context, currentTool.title, toolResult)
                                            },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Share result")
                                        }
                                        IconButton(
                                            onClick = {
                                                if (isSpeaking) voiceManager.stopSpeaking()
                                                else voiceManager.speak(toolResult, preferredLanguage)
                                            },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Icon(
                                                imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                                contentDescription = if (isSpeaking) "Stop speaking" else "Read aloud"
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = toolResult,
                                    style = if (isCodeTool) {
                                        MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        )
                                    } else {
                                        MaterialTheme.typography.bodyLarge
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun toolIcon(tool: AiToolType): ImageVector = when (tool) {
    AiToolType.WRITING -> Icons.Default.Create
    AiToolType.CODE -> Icons.Default.Code
    AiToolType.WEBSITE -> Icons.Default.Language
    AiToolType.APP_DEV -> Icons.Default.PhoneAndroid
    AiToolType.YOUTUBE -> Icons.Default.PlayCircle
    AiToolType.GAMING -> Icons.Default.SportsEsports
    AiToolType.TRANSLATOR -> Icons.Default.Translate
    AiToolType.IDEAS -> Icons.Default.Lightbulb
}
