package com.badereddine.skillquant.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.badereddine.skillquant.domain.model.Alert
import com.badereddine.skillquant.domain.model.Skill
import com.badereddine.skillquant.domain.model.TrendingSkill
import com.badereddine.skillquant.ui.calculator.SalaryCalculatorScreen
import com.badereddine.skillquant.ui.comparison.ComparisonScreen
import com.badereddine.skillquant.ui.components.ArbitrageCard
import com.badereddine.skillquant.ui.detail.SkillDetailScreen
import com.badereddine.skillquant.ui.learningpath.LearningPathScreen
import com.badereddine.skillquant.ui.news.NewsScreen
import com.badereddine.skillquant.ui.onboarding.OnboardingScreen
import com.badereddine.skillquant.ui.radar.RadarScreen
import com.badereddine.skillquant.ui.settings.AlertsSettingsScreen
import com.badereddine.skillquant.ui.theme.NegativeRed
import com.badereddine.skillquant.ui.theme.PositiveGreen
import com.badereddine.skillquant.ui.theme.TealPrimary
import com.badereddine.skillquant.util.Constants
import com.badereddine.skillquant.util.toPercentString
import com.badereddine.skillquant.util.toRelativeTimeString
import kotlin.math.abs

class DashboardScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<DashboardViewModel>()
        val state by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        // Show onboarding only once per session
        var hasShownOnboarding by remember { mutableStateOf(false) }
        LaunchedEffect(state.showOnboarding) {
            if (state.showOnboarding && !hasShownOnboarding) {
                hasShownOnboarding = true
                navigator.push(OnboardingScreen())
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "SkillQuant",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        BadgedBox(
                            badge = {
                                if (state.unreadAlertCount > 0) {
                                    Badge { Text("${state.unreadAlertCount}") }
                                }
                            }
                        ) {
                            IconButton(onClick = { navigator.push(AlertsSettingsScreen()) }) {
                                Text("🔔")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                // Search + Location filter on same row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Search skills…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    if (state.searchQuery.isBlank()) {
                        LocationChip(
                            selected = state.selectedLocation,
                            locations = state.availableLocations,
                            onSelected = { viewModel.onLocationSelected(it) }
                        )
                    }
                }

                // Search results overlay
                if (state.searchQuery.isNotBlank()) {
                    if (state.isSearching) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    } else if (state.searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No skills found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            items(state.searchResults) { skill ->
                                SearchResultRow(
                                    skill = skill,
                                    onClick = { navigator.push(SkillDetailScreen(skill.id, state.selectedLocation)) }
                                )
                            }
                        }
                    }
                } else if (state.isLoading) {
                    LoadingDashboard()
                } else if (state.isOffline) {
                    OfflineState(onRetry = { viewModel.refresh() })
                } else if (state.error != null) {
                    ErrorState(
                        error = state.error!!,
                        onRetry = { viewModel.refresh() }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Refresh indicator
                            if (state.isRefreshing) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    }
                                }
                            }

                            // Quick Actions Row
                            item {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    item {
                                        QuickActionChip("⚔️ Compare") {
                                            navigator.push(ComparisonScreen(state.selectedLocation))
                                        }
                                    }
                                    item {
                                        QuickActionChip("💰 Salary Calc") {
                                            navigator.push(SalaryCalculatorScreen(state.selectedLocation))
                                        }
                                    }
                                    item {
                                        QuickActionChip("🎯 Learn Path") {
                                            navigator.push(LearningPathScreen(state.selectedLocation))
                                        }
                                    }
                                    item {
                                        QuickActionChip("🕸️ Radar") {
                                            navigator.push(RadarScreen(state.selectedLocation))
                                        }
                                    }
                                    item {
                                        QuickActionChip("📰 News") {
                                            navigator.push(NewsScreen())
                                        }
                                    }
                                    item {
                                        QuickActionChip("🔄 Refresh") {
                                            viewModel.refresh()
                                        }
                                    }
                                }
                            }

                            // Top Arbitrage Opportunities
                            item {
                            SectionHeader(
                                title = "🔥 Top Arbitrage Opportunities",
                                subtitle = if (state.userTier == Constants.TIER_FREE)
                                    "Free tier • Top 15" else null
                            )
                        }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.arbitrageOpportunities) { opportunity ->
                                    ArbitrageCard(
                                        opportunity = opportunity,
                                        onClick = {
                                            navigator.push(SkillDetailScreen(opportunity.skillId, state.selectedLocation))
                                        }
                                    )
                                }
                            }
                        }

                        // Trending Skills
                        item {
                            SectionHeader(
                                title = "📈 Trending Skills",
                                infoText = """
                                    How this works:
                                    - We compare today's demand score with the score from 7 days ago.
                                    - If the score goes up, trend is positive. If it goes down, trend is negative.
                                    - We only show skills where the weekly change is bigger than 3%.

                                    Example:
                                    60 now vs 50 last week = +20% (trending up)
                                """.trimIndent()
                            )
                        }
                        items(state.trendingSkills) { skill ->
                            TrendingSkillRow(
                                skill = skill,
                                onClick = {
                                    navigator.push(SkillDetailScreen(skill.skillId, state.selectedLocation))
                                }
                            )
                        }

                        // Recent Alerts
                        if (state.recentAlerts.isNotEmpty()) {
                            item {
                                SectionHeader(
                                    title = "🔔 Recent Alerts",
                                    action = "View All",
                                    onAction = { navigator.push(AlertsSettingsScreen()) }
                                )
                            }
                            items(state.recentAlerts) { alert ->
                                AlertRow(alert = alert)
                            }
                        }

                        item { Spacer(modifier = Modifier.height(32.dp)) }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionChip(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        tonalElevation = 1.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun SearchResultRow(skill: Skill, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = skill.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${skill.category} • 📍 ${skill.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("→", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationChip(
    selected: String,
    locations: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        Surface(
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 2.dp,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = locationFlag(selected),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = selected,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1
                )
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        }
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            locations.forEach { location ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "${locationFlag(location)} $location",
                                fontWeight = if (location == selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                    onClick = {
                        onSelected(location)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun locationFlag(location: String): String = when (location) {
    "Morocco" -> "🇲🇦"
    "France" -> "🇫🇷"
    "USA" -> "🇺🇸"
    else -> "📍"
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String? = null,
    infoText: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (infoText != null) {
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Trending skills info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(action)
            }
        }
    }

    if (showInfoDialog && infoText != null) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("How trending is calculated") },
            text = { Text(infoText) },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Got it")
                }
            }
        )
    }
}

@Composable
private fun TrendingSkillRow(
    skill: TrendingSkill,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = skill.skillName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${skill.location} • ${skill.period}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val trendColor = if (skill.trendDirection == "up") PositiveGreen else NegativeRed
            val arrow = if (skill.trendDirection == "up") "↑" else "↓"
            val signedChangePercent = if (skill.trendDirection == "down") {
                -abs(skill.changePercent)
            } else {
                abs(skill.changePercent)
            }
            Text(
                text = "$arrow ${signedChangePercent.toPercentString()}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = trendColor
            )
        }
    }
}

@Composable
private fun AlertRow(alert: Alert) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!alert.read) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = alert.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (!alert.read) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = alert.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text(
            text = alert.createdAt.toRelativeTimeString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoadingDashboard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                "Loading market data…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OfflineState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "📡",
            style = MaterialTheme.typography.displayLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Internet Connection",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Please check your Wi-Fi or mobile data\nand try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Retry")
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "⚠️",
            style = MaterialTheme.typography.displayMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}
