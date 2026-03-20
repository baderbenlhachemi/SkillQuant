package com.badereddine.skillquant.ui.learningpath

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.badereddine.skillquant.domain.model.SkillMetrics
import com.badereddine.skillquant.ui.theme.*
import com.badereddine.skillquant.util.toSalaryString
import org.koin.core.parameter.parametersOf

data class LearningPathScreen(val location: String = "Morocco") : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<LearningPathViewModel> { parametersOf(location) }
        val state by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("🎯 Learning Path", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) { Text("←", style = MaterialTheme.typography.titleLarge) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        ) { padding ->
            when {
                state.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.isEmpty -> Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📚", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(16.dp))
                        Text("No skills in your watchlist", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Star some skills first to build a learning path", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Prioritized by ROI (Arbitrage Score)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    itemsIndexed(state.pathItems) { index, metrics ->
                        LearningPathCard(index + 1, metrics, uriHandler, location)
                    }
                }
            }
        }
    }
}

@Composable
private fun LearningPathCard(
    rank: Int,
    metrics: SkillMetrics,
    uriHandler: androidx.compose.ui.platform.UriHandler,
    location: String = "USA"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Rank badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (rank) {
                        1 -> GoldAccent
                        2 -> NeutralBlue
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        "#$rank",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (rank <= 2) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(metrics.skillName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(metrics.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("ROI ${metrics.arbitrageScore.toInt()}", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = PositiveGreen)
                    Text(metrics.avgSalary.toSalaryString(location), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (metrics.learningResources.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("📚 Top Resources", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                metrics.learningResources.take(2).forEach { res ->
                    val resUrl = res.url.ifBlank {
                        "https://www.google.com/search?q=${res.title.replace(" ", "%20")}"
                    }
                    TextButton(
                        onClick = { runCatching { uriHandler.openUri(resUrl) } },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "${res.title} • ${res.platform}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

