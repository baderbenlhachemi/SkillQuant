package com.badereddine.skillquant.ui.radar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.badereddine.skillquant.ui.components.RadarChart
import com.badereddine.skillquant.ui.components.RadarEntry
import com.badereddine.skillquant.ui.theme.*
import com.badereddine.skillquant.util.toSalaryString
import org.koin.core.parameter.parametersOf

private val CHART_COLORS = listOf(TealPrimary, NeutralBlue, GoldAccent, PositiveGreen, NegativeRed, Color(0xFFAB47BC), Color(0xFFFF7043))

data class RadarScreen(val location: String = "Morocco") : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<RadarViewModel> { parametersOf(location) }
        val state by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("🕸️ Skill Radar", fontWeight = FontWeight.Bold) },
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
                        Text("🕸️", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(16.dp))
                        Text("Add skills to your watchlist first", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Star some skills to see them on the radar", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
                else -> {
                    Column(
                        Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val axisLabels = listOf("Demand", "Supply Gap", "Salary", "Arbitrage", "Jobs")

                        // Compute max values for relative normalization
                        val maxSalary = state.metrics.maxOfOrNull { it.avgSalary }?.coerceAtLeast(1L) ?: 1L
                        val maxJobs = state.metrics.maxOfOrNull { it.jobPostCount }?.coerceAtLeast(1) ?: 1

                        val entries = state.metrics.mapIndexed { i, m ->
                            val normalized = m.toRadarNormalizedMetrics(maxSalary = maxSalary, maxJobs = maxJobs)
                            RadarEntry(
                                skillName = m.skillName,
                                values = listOf(
                                    normalized.demand,
                                    normalized.supplyGap,
                                    normalized.salary,
                                    normalized.arbitrage,
                                    normalized.jobs
                                ),
                                color = CHART_COLORS[i % CHART_COLORS.size]
                            )
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Watchlist Radar", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "All axes 0-100. Supply Gap is inverted (100 - Supply), so lower talent supply plots farther out. Salary & Jobs are relative to the highest in your watchlist.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                RadarChart(entries = entries, axisLabels = axisLabels)
                            }
                        }

                        // Legend with actual values
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Skill Values", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                state.metrics.forEachIndexed { i, m ->
                                    val color = CHART_COLORS[i % CHART_COLORS.size]
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(Modifier.size(12.dp).clip(CircleShape).background(color))
                                            Text(m.skillName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        }
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            ValueChip("Demand", "${m.demandScore.toInt()}")
                                            ValueChip("Supply Gap", "${toSupplyGapScore(m.supplyScore).toInt()}")
                                            val salaryStr = m.avgSalary.toSalaryString(location)
                                            ValueChip("Salary", salaryStr)
                                            ValueChip("Arb.", "${m.arbitrageScore.toInt()}")
                                            ValueChip("Jobs", "${m.jobPostCount}")
                                        }
                                        if (i < state.metrics.size - 1) {
                                            HorizontalDivider(Modifier.padding(top = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)
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
}

@Composable
private fun ValueChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
