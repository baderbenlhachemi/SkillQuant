package com.badereddine.skillquant.ui.news

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.badereddine.skillquant.ui.theme.*

class NewsScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<NewsViewModel>()
        val state by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("📰 Market News", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) { Text("←", style = MaterialTheme.typography.titleLarge) }
                    },
                    actions = {
                        TextButton(onClick = { viewModel.refresh() }) { Text("Refresh") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        ) { padding ->
            when {
                state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Fetching stories from Hacker News…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                state.error != null -> Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Failed to load news", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { viewModel.refresh() }) { Text("Retry") }
                    }
                }
                state.news.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No news found for your skills", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "Recent stories from Hacker News & Dev.to matching your watchlist",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    items(state.news) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (item.url.isNotBlank()) runCatching { uriHandler.openUri(item.url) }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        item.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (item.points > 0) {
                                        Text("▲ ${item.points}", style = MaterialTheme.typography.labelSmall, color = GoldAccent)
                                    }
                                    if (item.createdAt.isNotBlank()) {
                                        Text(formatRelativeDate(item.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (item.author.isNotBlank()) {
                                        Text("by ${item.author}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = TealPrimary.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            item.matchedSkill,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TealPrimary
                                        )
                                    }
                                    // Source badge
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (item.source == "Dev.to")
                                            NeutralBlue.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            item.source,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (item.source == "Dev.to") NeutralBlue
                                            else MaterialTheme.colorScheme.onSurfaceVariant
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

private fun formatRelativeDate(isoDate: String): String {
    return try {
        // Parse ISO 8601 like "2026-02-25T10:30:00.000Z"
        val dateStr = isoDate.substringBefore("T")
        val parts = dateStr.split("-")
        if (parts.size != 3) return ""
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()
        // Rough days-since calculation
        val nowMs = System.currentTimeMillis()
        val approxArticleMs = run {
            val daysInYear = 365L
            val daysFromYear = (year - 1970L) * daysInYear
            val daysFromMonth = (month - 1) * 30L
            (daysFromYear + daysFromMonth + day) * 24 * 60 * 60 * 1000
        }
        val diffMs = nowMs - approxArticleMs
        val diffDays = (diffMs / (24 * 60 * 60 * 1000)).toInt()
        when {
            diffDays <= 0 -> "today"
            diffDays == 1 -> "1d ago"
            diffDays < 7 -> "${diffDays}d ago"
            diffDays < 30 -> "${diffDays / 7}w ago"
            diffDays < 365 -> "${diffDays / 30}mo ago"
            else -> "${diffDays / 365}y ago"
        }
    } catch (_: Exception) { "" }
}
