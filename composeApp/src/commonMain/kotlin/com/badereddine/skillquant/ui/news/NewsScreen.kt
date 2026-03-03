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
                        IconButton(onClick = { navigator.pop() }) {
                            Text("←", style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    actions = {
                        TextButton(onClick = { viewModel.refresh() }) { Text("Refresh") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            when {
                state.isLoading -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Fetching stories from HN, Dev.to, Reddit, Lobsters & GitHub…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                state.error != null -> Box(
                    Modifier.fillMaxSize().padding(padding).padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚠️", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("Failed to load news", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { viewModel.refresh() }) { Text("Retry") }
                    }
                }
                state.allNews.isEmpty() -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No news found for your skills",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // ── For You section ───────────────────────────────────────
                    if (state.watchlistNews.isNotEmpty()) {
                        item {
                            SectionHeader(
                                emoji = "🎯",
                                title = "For You",
                                subtitle = "Based on your watchlist — ${state.watchlistNews.size} stories"
                            )
                        }
                        items(state.watchlistNews) { item ->
                            NewsCard(item) {
                                if (item.url.isNotBlank()) runCatching { uriHandler.openUri(item.url) }
                            }
                        }
                    }

                    // ── General section ───────────────────────────────────────
                    if (state.generalNews.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(if (state.watchlistNews.isNotEmpty()) 8.dp else 0.dp))
                            SectionHeader(
                                emoji = "🌐",
                                title = if (state.watchlistNews.isEmpty()) "Top Stories" else "More Stories",
                                subtitle = "Hacker News · Dev.to · Reddit · Lobsters · GitHub"
                            )
                        }
                        items(state.generalNews) { item ->
                            NewsCard(item) {
                                if (item.url.isNotBlank()) runCatching { uriHandler.openUri(item.url) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(emoji: String, title: String, subtitle: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(
            "$emoji $title",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun NewsCard(item: NewsItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.points > 0) {
                    Text("▲ ${item.points}", style = MaterialTheme.typography.labelSmall, color = GoldAccent)
                }
                if (item.createdAt.isNotBlank()) {
                    Text(
                        formatRelativeDate(item.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.author.isNotBlank()) {
                    Text(
                        "by ${item.author}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Skill tag
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
                val (sourceBg, sourceFg) = sourceBadgeColors(item.source)
                Surface(shape = RoundedCornerShape(4.dp), color = sourceBg) {
                    Text(
                        item.source,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = sourceFg
                    )
                }
            }
        }
    }
}

@Composable
private fun sourceBadgeColors(source: String): Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> {
    return when {
        source == "Hacker News"     -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        source == "Dev.to"          -> NeutralBlue.copy(alpha = 0.15f) to NeutralBlue
        source.startsWith("Reddit") -> PositiveGreen.copy(alpha = 0.15f) to PositiveGreen
        source == "Lobsters"        -> NegativeRed.copy(alpha = 0.15f) to NegativeRed
        source == "GitHub Trending" -> GoldAccent.copy(alpha = 0.15f) to GoldAccent
        else                        -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun formatRelativeDate(isoDate: String): String {
    return try {
        val dateStr = isoDate.substringBefore("T")
        val parts = dateStr.split("-")
        if (parts.size != 3) return ""
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()
        val nowMs = System.currentTimeMillis()
        val approxArticleMs = run {
            val daysFromYear = (year - 1970L) * 365L
            val daysFromMonth = (month - 1) * 30L
            (daysFromYear + daysFromMonth + day) * 24 * 60 * 60 * 1000
        }
        val diffDays = ((nowMs - approxArticleMs) / (24 * 60 * 60 * 1000)).toInt()
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
