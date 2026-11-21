package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.PagliiNeonAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("about_screen")
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
                        .testTag("about_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            title = { Text("About PAGLII GPT", style = MaterialTheme.typography.titleLarge) }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                PagliiNeonAvatar(size = 104.dp, pulse = true)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "PAGLII GPT",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.tagline),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Developer: SAHIB  •  Category: AI Assistant  •  v1.0",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Core Capabilities",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        AboutFeatureItem(
                            icon = Icons.Default.Translate,
                            title = "Multilingual Conversations",
                            detail = "Natural conversations in Hindi, Urdu, English, and Hinglish with multi-turn memory."
                        )
                        AboutFeatureItem(
                            icon = Icons.Default.CameraAlt,
                            title = "Multimodal Photo Vision",
                            detail = "Understands photos, printed & handwritten text, diagrams, charts, and translates image text."
                        )
                        AboutFeatureItem(
                            icon = Icons.Default.MenuBook,
                            title = "Step-by-Step Homework Solver",
                            detail = "Maths, Science, SST, English, Urdu, worksheet scan, and practice Q&A generation."
                        )
                        AboutFeatureItem(
                            icon = Icons.Default.AutoAwesome,
                            title = "8 Creator & Developer Tools",
                            detail = "Writing, Code, Websites, App Dev, YouTube SEO, Gaming, Translation, and Ideas."
                        )
                        AboutFeatureItem(
                            icon = Icons.Default.Shield,
                            title = "Zero-Key Secure Backend",
                            detail = "Powered by Firebase AI Logic, App Check attestation, Google Sign-In, and private per-user Firestore storage—no API keys are stored inside the APK."
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutFeatureItem(
    icon: ImageVector,
    title: String,
    detail: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(22.dp)
        )
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
