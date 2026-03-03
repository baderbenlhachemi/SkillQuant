package com.badereddine.skillquant.ui.detail

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.badereddine.skillquant.ui.components.ScoreGauge
import com.badereddine.skillquant.ui.components.SkillChip
import com.badereddine.skillquant.ui.components.TrendChart
import com.badereddine.skillquant.ui.theme.*
import com.badereddine.skillquant.util.Constants
import com.badereddine.skillquant.util.toSalaryString
import com.badereddine.skillquant.util.toFreelanceRateString
import org.koin.core.parameter.parametersOf
import cafe.adriel.voyager.koin.koinScreenModel

data class SkillDetailScreen(val skillId: String, val location: String = "Morocco") : Screen {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SkillDetailViewModel> { parametersOf(skillId, location) }
        val state by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            state.metrics?.skillName ?: "Skill Detail",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Text("←", style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            floatingActionButton = {
                if (!state.isLoading && state.metrics != null) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (!state.isTogglingWatchlist) viewModel.toggleWatchlist()
                        },
                        containerColor = if (state.isOnWatchlist)
                            MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = if (state.isOnWatchlist) "★ Watching" else "☆ Watch",
                            color = if (state.isOnWatchlist)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        ) { padding ->
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Loading skill data…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (state.error != null) {
                val errorMsg = state.error!!.lowercase()
                val isOffline = errorMsg.contains("unavailable") || errorMsg.contains("network") ||
                        errorMsg.contains("connect") || errorMsg.contains("timeout") ||
                        errorMsg.contains("grpc") || errorMsg.contains("internet")
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            if (isOffline) "📡" else "⚠️",
                            style = MaterialTheme.typography.displayLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (isOffline) "No Internet Connection" else "Something went wrong",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            if (isOffline) "Please check your connection and try again."
                            else state.error!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(onClick = { navigator.pop() }) {
                            Text("Go Back")
                        }
                    }
                }
            } else if (state.metrics == null) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊", style = MaterialTheme.typography.displayMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No data available for this skill yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { navigator.pop() }) {
                            Text("Go Back")
                        }
                    }
                }
            } else {
                val metrics = state.metrics!!

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // Category + location chip
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SkillChip(text = metrics.category, category = metrics.category)
                            SkillChip(text = "📍 ${metrics.location}")
                            if (state.userTier == Constants.TIER_FREE) {
                                SkillChip(text = "Free Tier", category = "")
                            }
                        }
                    }

                    // Arbitrage Score Gauge
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Arbitrage Score",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                ScoreGauge(
                                    score = metrics.arbitrageScore,
                                    size = 140.dp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = when {
                                        metrics.arbitrageScore >= 75 -> "🔥 High opportunity — underserved skill with strong demand"
                                        metrics.arbitrageScore >= 50 -> "📈 Good opportunity — growing demand gap"
                                        metrics.arbitrageScore >= 25 -> "📊 Moderate — balanced market"
                                        else -> "📉 Low opportunity — well-supplied market"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Demand vs Supply
                    item {
                        DemandSupplyCard(
                            demandScore = metrics.demandScore,
                            supplyScore = metrics.supplyScore,
                            jobCount = metrics.jobPostCount,
                            gigCount = metrics.freelanceGigCount
                        )
                    }

                    // Trend Chart
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Trend toggle
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = state.selectedTrend == TrendType.DEMAND,
                                        onClick = { viewModel.selectTrend(TrendType.DEMAND) },
                                        label = { Text("Demand") }
                                    )
                                    FilterChip(
                                        selected = state.selectedTrend == TrendType.SALARY,
                                        onClick = { viewModel.selectTrend(TrendType.SALARY) },
                                        label = { Text("Salary") }
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                val trendData = when (state.selectedTrend) {
                                    TrendType.DEMAND -> {
                                        if (state.userTier == Constants.TIER_FREE)
                                            metrics.demandTrend.takeLast(Constants.FREE_HISTORY_DAYS)
                                        else metrics.demandTrend
                                    }
                                    TrendType.SALARY -> {
                                        if (state.userTier == Constants.TIER_FREE)
                                            metrics.salaryTrend.takeLast(Constants.FREE_HISTORY_DAYS)
                                        else metrics.salaryTrend
                                    }
                                }
                                val lineColor = when (state.selectedTrend) {
                                    TrendType.DEMAND -> PositiveGreen
                                    TrendType.SALARY -> NeutralBlue
                                }
                                TrendChart(
                                    data = trendData,
                                    lineColor = lineColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Salary section
                    item {
                        SalaryCard(
                            avgSalary = metrics.avgSalary,
                            medianSalary = metrics.medianSalary,
                            freelanceRate = metrics.freelanceHourlyRate,
                            isPro = state.userTier == Constants.TIER_PRO,
                            location = location
                        )
                    }

                    // Top employers
                    if (metrics.topEmployers.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "🏢 Top Employers",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        metrics.topEmployers.take(5).forEach { employer ->
                                            SkillChip(text = employer)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Learning Resources
                    if (metrics.learningResources.isNotEmpty()) {
                        item {
                            Text(
                                text = "📚 Learning Resources",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        val resources = if (state.userTier == Constants.TIER_FREE)
                            metrics.learningResources.take(Constants.FREE_LEARNING_RESOURCES_LIMIT)
                        else metrics.learningResources

                        items(resources) { resource ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (resource.url.isNotBlank()) {
                                            runCatching { uriHandler.openUri(resource.url) }
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = resource.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${resource.platform} • ${resource.type}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "Open →",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (state.userTier == Constants.TIER_FREE &&
                            metrics.learningResources.size > Constants.FREE_LEARNING_RESOURCES_LIMIT
                        ) {
                            item {
                                Text(
                                    text = "🔒 +${metrics.learningResources.size - Constants.FREE_LEARNING_RESOURCES_LIMIT} more with Pro",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldAccent
                                )
                            }
                        }
                    }

                    // Job Listings
                    if (metrics.jobListings.isNotEmpty()) {
                        item {
                            Text(
                                text = "💼 Job Listings in ${metrics.location}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        items(metrics.jobListings) { job ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (job.url.isNotBlank()) {
                                            runCatching { uriHandler.openUri(job.url) }
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    // Title + type chip
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = job.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.weight(1f)
                                        )
                                        SkillChip(text = job.type)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Company + source badge
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = job.company,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (job.source.isNotBlank()) {
                                            Text(
                                                text = "via ${job.source}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    // Salary + location
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "💰 ${job.salaryRange}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "📍 ${job.location}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Posted date + "View on source →"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when (job.postedDaysAgo) {
                                                0 -> "🟢 Posted today"
                                                1 -> "Posted yesterday"
                                                else -> "Posted ${job.postedDaysAgo}d ago"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (job.postedDaysAgo == 0) PositiveGreen
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (job.source.isNotBlank()) "View on ${job.source} →" else "View →",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom spacer for FAB
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun DemandSupplyCard(
    demandScore: Double,
    supplyScore: Double,
    jobCount: Int,
    gigCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Demand vs Supply",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Demand bar
            MetricBar(
                label = "Demand",
                value = demandScore,
                color = PositiveGreen,
                detail = "$jobCount jobs + $gigCount gigs"
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Supply bar
            MetricBar(
                label = "Supply",
                value = supplyScore,
                color = NegativeRed,
                detail = "Available talent pool"
            )
        }
    }
}

@Composable
private fun MetricBar(
    label: String,
    value: Double,
    color: androidx.compose.ui.graphics.Color,
    detail: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${value.toInt()}/100",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (value / 100.0).toFloat().coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
        Text(
            text = detail,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SalaryCard(
    avgSalary: Long,
    medianSalary: Long,
    freelanceRate: Double,
    isPro: Boolean,
    location: String = "USA"
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "💰 Compensation",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SalaryMetric(
                    label = "Avg Salary",
                    value = avgSalary.toDouble().toSalaryString(location) + "/yr"
                )
                if (isPro) {
                    SalaryMetric(
                        label = "Median",
                        value = medianSalary.toDouble().toSalaryString(location) + "/yr"
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔒", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Median (Pro)",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldAccent
                        )
                    }
                }
                SalaryMetric(
                    label = "Freelance",
                    value = freelanceRate.toFreelanceRateString(location)
                )
            }
        }
    }
}

@Composable
private fun SalaryMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

