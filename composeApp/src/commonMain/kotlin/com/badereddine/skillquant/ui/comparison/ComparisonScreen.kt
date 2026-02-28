package com.badereddine.skillquant.ui.comparison

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

data class ComparisonScreen(val location: String = "Morocco") : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ComparisonViewModel> { parametersOf(location) }
        val state by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("⚔️ Skill Comparison", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) { Text("←", style = MaterialTheme.typography.titleLarge) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Skill Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SkillPicker(
                        label = "Skill A",
                        skills = state.allSkills.map { it.id to it.name },
                        selectedId = state.selectedIdA,
                        onSelect = { viewModel.selectSkillA(it) },
                        modifier = Modifier.weight(1f)
                    )
                    SkillPicker(
                        label = "Skill B",
                        skills = state.allSkills.map { it.id to it.name },
                        selectedId = state.selectedIdB,
                        onSelect = { viewModel.selectSkillB(it) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (state.isLoading) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                val a = state.skillA
                val b = state.skillB
                if (a != null && b != null) {
                    // Header
                    Text(
                        "${a.skillName} vs ${b.skillName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    CompareBar("Demand", a.demandScore, b.demandScore, a.skillName, b.skillName)
                    CompareBar("Supply", a.supplyScore, b.supplyScore, a.skillName, b.skillName)
                    CompareBar("Arbitrage", a.arbitrageScore, b.arbitrageScore, a.skillName, b.skillName)

                    // Salary comparison
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("💰 Salary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                SalaryBlock(a.skillName, a.avgSalary, location)
                                Text("vs", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
                                SalaryBlock(b.skillName, b.avgSalary, location)
                            }
                            val diff = ((a.avgSalary - b.avgSalary).toDouble() / b.avgSalary * 100)
                            val winner = if (diff > 0) a.skillName else b.skillName
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "$winner pays ${kotlin.math.abs(diff).toInt()}% more",
                                style = MaterialTheme.typography.labelMedium,
                                color = PositiveGreen,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Job counts
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatBlock("📋 Jobs", a.skillName, "${a.jobPostCount}", b.skillName, "${b.jobPostCount}")
                            StatBlock("💼 Gigs", a.skillName, "${a.freelanceGigCount}", b.skillName, "${b.freelanceGigCount}")
                        }
                    }
                } else if (a == null && b == null && !state.isLoading) {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Pick two skills above to compare",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkillPicker(
    label: String,
    skills: List<Pair<String, String>>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = skills.find { it.first == selectedId }?.second ?: label

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            skills.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = { onSelect(id); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun CompareBar(
    label: String,
    valueA: Double,
    valueB: Double,
    nameA: String,
    nameB: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            BarRow(nameA, valueA, TealPrimary)
            Spacer(Modifier.height(6.dp))
            BarRow(nameB, valueB, NeutralBlue)
        }
    }
}

@Composable
private fun BarRow(name: String, value: Double, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(name, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(80.dp))
        Box(
            Modifier
                .weight(1f)
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (value / 100.0).toFloat().coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(10.dp))
                    .background(color)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("${value.toInt()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SalaryBlock(name: String, salary: Long, location: String = "USA") {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(salary.toSalaryString(location), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatBlock(label: String, nameA: String, valA: String, nameB: String, valB: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("$nameA: $valA", style = MaterialTheme.typography.bodySmall)
        Text("$nameB: $valB", style = MaterialTheme.typography.bodySmall)
    }
}

