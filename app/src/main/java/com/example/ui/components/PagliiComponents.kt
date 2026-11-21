package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.LocalChatMessage
import com.example.utils.ImageUtils

@Composable
fun PagliiNeonAvatar(
    size: Dp = 40.dp,
    pulse: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatarPulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (pulse) 1.08f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(glowScale)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                        MaterialTheme.colorScheme.tertiary
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_paglii_icon),
            contentDescription = "Paglii GPT robot avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
        )
    }
}

@Composable
fun TypingDotsIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "typingDots")
    val dot1 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, delayMillis = 0),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, delayMillis = 180),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3 by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, delayMillis = 360),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(dot1, dot2, dot3).forEachIndexed { index, alpha ->
            val color = when (index) {
                0 -> MaterialTheme.colorScheme.primary
                1 -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.tertiary
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Paglii is thinking...",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ChatMessageBubble(
    message: LocalChatMessage,
    isSpeaking: Boolean,
    onSpeak: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onCopiedToast: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.role == "user"
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!isUser) {
                PagliiNeonAvatar(size = 32.dp, modifier = Modifier.padding(top = 4.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isUser) 20.dp else 6.dp,
                    topEnd = if (isUser) 6.dp else 20.dp,
                    bottomStart = 20.dp,
                    bottomEnd = 20.dp
                ),
                color = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
                },
                border = BorderStroke(
                    1.dp,
                    if (isUser) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    }
                ),
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (!message.toolTag.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = message.toolTag,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    message.attachedBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Attached photo in chat",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } ?: if (message.hasImage && isUser) {
                        Text(
                            text = "[Photo attached]",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    } else Unit

                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    if (!isUser && message.text.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(message.text))
                                    onCopiedToast()
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("copy_answer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy answer",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    ImageUtils.shareText(context, "PAGLII GPT Answer", message.text)
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("share_answer_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share answer",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (isSpeaking) onStopSpeaking() else onSpeak(message.text)
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("speak_answer_button")
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = if (isSpeaking) "Stop speaking" else "Read answer aloud",
                                    tint = if (isSpeaking) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VoicePlaybackBanner(
    isSpeaking: Boolean,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = isSpeaking, modifier = modifier) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Speaking aloud",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Paglii is speaking...",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                FilledTonalButton(
                    onClick = onStopClick,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    modifier = Modifier.testTag("stop_voice_playback_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.StopCircle,
                        contentDescription = "Stop voice playback",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Stop Voice", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ErrorBanner(
    message: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = !message.isNullOrBlank(), modifier = modifier) {
        Surface(
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = message.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun NeonGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val bg = MaterialTheme.colorScheme.background
    val primaryGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val cyanGlow = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        bg,
                        bg.copy(alpha = 0.98f)
                    )
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(primaryGlow, Color.Transparent),
                    radius = 900f
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(cyanGlow, Color.Transparent),
                    radius = 1100f
                )
            )
    ) {
        content()
    }
}
