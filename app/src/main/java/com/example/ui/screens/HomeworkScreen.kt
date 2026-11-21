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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.AssistantLanguage
import com.example.data.model.HomeworkSubject
import com.example.ui.components.ErrorBanner
import com.example.ui.components.TypingDotsIndicator
import com.example.ui.components.VoicePlaybackBanner
import com.example.utils.ImageUtils
import com.example.voice.VoiceAssistantManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeworkScreen(
    attachedBitmap: Bitmap?,
    solutionText: String,
    isGenerating: Boolean,
    homeworkError: String?,
    preferredLanguage: AssistantLanguage,
    voiceManager: VoiceAssistantManager,
    isSpeaking: Boolean,
    onSetImage: (Bitmap?) -> Unit,
    onSolveHomework: (HomeworkSubject, String, Boolean) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var selectedSubject by remember { mutableStateOf(HomeworkSubject.SOLVE_MATHS) }
    var preferShortAnswer by remember { mutableStateOf(false) }
    var questionInput by remember { mutableStateOf("") }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = ImageUtils.decodeScaledBitmap(context, uri)
                if (bmp != null) {
                    onSetImage(bmp)
                    if (selectedSubject == HomeworkSubject.SCAN_HOMEWORK) {
                        onSolveHomework(selectedSubject, questionInput, preferShortAnswer)
                    }
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
                val bmp = ImageUtils.decodeScaledBitmap(context, uri)
                if (bmp != null) {
                    onSetImage(bmp)
                    if (selectedSubject == HomeworkSubject.SCAN_HOMEWORK) {
                        onSolveHomework(selectedSubject, questionInput, preferShortAnswer)
                    }
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
            Toast.makeText(context, "Camera permission is required to scan homework.", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerCameraScan() {
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
            .testTag("homework_screen")
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ),
            title = {
                Column {
                    Text("Homework Solver", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Step-by-Step Study Buddy 📚",
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

        ErrorBanner(message = homeworkError)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_homework_banner),
                        contentDescription = "Paglii GPT Homework Solver Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                }
            }

            item {
                Text(
                    text = "Choose Homework Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HomeworkSubject.entries.forEach { subject ->
                        val isSelected = selectedSubject == subject
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedSubject = subject
                                when {
                                    subject == HomeworkSubject.SHORT_ANSWERS -> preferShortAnswer = true
                                    subject == HomeworkSubject.DETAILED_ANSWERS -> preferShortAnswer = false
                                    subject.triggersScan -> triggerCameraScan()
                                }
                            },
                            label = { Text(subject.label) },
                            modifier = Modifier.testTag("homework_btn_${subject.name.lowercase()}")
                        )
                    }
                }
            }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !preferShortAnswer,
                        onClick = { preferShortAnswer = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("homework_detailed_toggle")
                    ) {
                        Text("Detailed Step-by-Step")
                    }
                    SegmentedButton(
                        selected = preferShortAnswer,
                        onClick = { preferShortAnswer = true },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("homework_short_toggle")
                    ) {
                        Text("Short Answers")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { triggerCameraScan() },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("homework_scan_camera_button")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan Camera")
                    }
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("homework_pick_gallery_button")
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload Photo")
                    }
                }
            }

            if (attachedBitmap != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    bitmap = attachedBitmap.asImageBitmap(),
                                    contentDescription = "Homework page photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Text(
                                    text = "Homework photo ready",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            IconButton(
                                onClick = { onSetImage(null) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove homework photo")
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = questionInput,
                    onValueChange = { questionInput = it },
                    label = { Text("${selectedSubject.label} — Question or Topic") },
                    placeholder = { Text(selectedSubject.subtitle) },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("homework_question_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        onSolveHomework(selectedSubject, questionInput, preferShortAnswer)
                    },
                    enabled = !isGenerating,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("homework_solve_button")
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isGenerating) "Solving..." else "Solve with Paglii GPT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isGenerating && solutionText.isEmpty()) {
                item { TypingDotsIndicator() }
            }

            if (solutionText.isNotEmpty()) {
                item {
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
                                    text = "${selectedSubject.label} Solution",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Row {
                                    IconButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(solutionText))
                                            Toast.makeText(context, "Solution copied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy solution")
                                    }
                                    IconButton(
                                        onClick = {
                                            ImageUtils.shareText(context, "Homework Solution", solutionText)
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share solution")
                                    }
                                    IconButton(
                                        onClick = {
                                            if (isSpeaking) voiceManager.stopSpeaking()
                                            else voiceManager.speak(solutionText, preferredLanguage)
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = if (isSpeaking) "Stop speaking" else "Speak solution"
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = solutionText,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
