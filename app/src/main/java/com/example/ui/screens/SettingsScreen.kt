package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.auth.signOutUser
import com.example.data.model.AppThemeMode
import com.example.data.model.AssistantLanguage
import com.example.data.preferences.UserSettings
import com.example.ui.components.PagliiNeonAvatar
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: UserSettings,
    onThemeChange: (AppThemeMode) -> Unit,
    onLanguageChange: (AssistantLanguage) -> Unit,
    onAutoSpeakChange: (Boolean) -> Unit,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val currentUser = Firebase.auth.currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen")
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ),
            navigationIcon = {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            title = { Text("Settings", style = MaterialTheme.typography.titleLarge) }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PagliiNeonAvatar(size = 56.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser?.displayName?.ifBlank { "Paglii Friend" } ?: "Paglii Friend",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentUser?.email ?: "Signed in with Google",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text("Appearance Mode", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            AppThemeMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = settings.themeMode == mode,
                                    onClick = { onThemeChange(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(
                                        index = index,
                                        count = AppThemeMode.entries.size
                                    ),
                                    modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}")
                                ) {
                                    Text(mode.label, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Text("AI & Voice Language", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Paglii GPT understands English, Hindi, Urdu & Hinglish seamlessly.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistantLanguage.entries.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowItems.forEach { lang ->
                                        FilterChip(
                                            selected = settings.preferredLanguage == lang,
                                            onClick = { onLanguageChange(lang) },
                                            label = { Text("${lang.label} (${lang.nativeHint})") },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("lang_chip_${lang.name.lowercase()}")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Column {
                                Text("Auto-Speak AI Replies", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "Automatically read chat replies aloud",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = settings.autoSpeakEnabled,
                            onCheckedChange = onAutoSpeakChange,
                            modifier = Modifier.testTag("auto_speak_switch")
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAbout() }
                        .testTag("settings_about_row")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text("About PAGLII GPT", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Developer SAHIB • Version 1.0 • AI & Security Info",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = {
                        signOutUser(credentialManager = credentialManager, scope = scope)
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
