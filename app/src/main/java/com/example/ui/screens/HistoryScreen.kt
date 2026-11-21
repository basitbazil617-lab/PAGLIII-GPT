package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ChatCategory
import com.example.data.model.ChatSession
import com.example.data.model.UiState
import com.example.ui.components.ErrorBanner
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    sessionsState: UiState<List<ChatSession>>,
    onOpenSession: (ChatSession) -> Unit,
    onDeleteSession: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ChatCategory.ALL) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("history_screen")
    ) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            ),
            title = {
                Column {
                    Text("Chat History", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Synced securely in your private cloud",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Search past chats or solutions...") },
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("history_search_input")
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ChatCategory.entries) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category.label) },
                    modifier = Modifier.testTag("history_filter_${category.key}")
                )
            }
        }

        when (sessionsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                }
            }
            is UiState.Error -> {
                ErrorBanner(message = sessionsState.message)
            }
            is UiState.Success -> {
                val filtered = remember(sessionsState.data, searchQuery, selectedCategory) {
                    sessionsState.data.filter { session ->
                        val matchesCat = selectedCategory == ChatCategory.ALL ||
                            session.category.equals(selectedCategory.key, ignoreCase = true)
                        val q = searchQuery.trim()
                        val matchesQuery = q.isEmpty() ||
                            session.title.contains(q, ignoreCase = true) ||
                            session.lastMessagePreview.contains(q, ignoreCase = true)
                        matchesCat && matchesQuery
                    }
                }

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Conversations Yet",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Start chatting, scanning photos, or solving homework — your history will appear right here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filtered, key = { it.id }) { session ->
                            val displayTime = session.updatedAt ?: session.createdAt ?: Timestamp.now()
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenSession(session) }
                                    .testTag("history_item_${session.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ChatBubbleOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = session.title.ifBlank { "Untitled Chat" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = session.lastMessagePreview,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = session.category.uppercase(),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = "• ${dateFormatter.format(displayTime.toDate())}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { onDeleteSession(session.id) },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("delete_chat_${session.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete chat",
                                            tint = MaterialTheme.colorScheme.error
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
