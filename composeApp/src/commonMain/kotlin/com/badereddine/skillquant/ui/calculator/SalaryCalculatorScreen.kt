package com.badereddine.skillquant.ui.calculator

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.badereddine.skillquant.ui.theme.*
import com.badereddine.skillquant.util.toSalaryString
import org.koin.core.parameter.parametersOf

data class SalaryCalculatorScreen(val location: String = "Morocco") : Screen {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<SalaryCalculatorViewModel> { parametersOf(location) }
        val state by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("💰 Salary Calculator", fontWeight = FontWeight.Bold) },
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
                // Current skills
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Your Current Skills", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Select the skills you already have", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.allSkills.forEach { skill ->
                                val selected = skill.id in state.currentSkillIds
                                FilterChip(
                                    selected = selected,
                                    onClick = { viewModel.toggleCurrentSkill(skill.id) },
                                    label = { Text(skill.name) }
                                )
                            }
                        }
                        if (state.currentMetrics.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            val avg = state.currentMetrics.map { it.avgSalary }.average().toLong()
                            Text(
                                "Current avg salary: ${avg.toSalaryString(location)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Target skill
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Target Skill to Learn", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        var expanded by remember { mutableStateOf(false) }
                        val targetName = state.allSkills.find { it.id == state.targetSkillId }?.name ?: "Pick a skill…"
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            OutlinedTextField(
                                value = targetName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                state.allSkills
                                    .filter { it.id !in state.currentSkillIds }
                                    .forEach { skill ->
                                        DropdownMenuItem(
                                            text = { Text(skill.name) },
                                            onClick = { viewModel.selectTargetSkill(skill.id); expanded = false }
                                        )
                                    }
                            }
                        }
                    }
                }

                // Result
                val increase = state.salaryIncrease
                if (increase != null && state.targetMetrics != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (increase >= 0) PositiveGreen.copy(alpha = 0.1f) else NegativeRed.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📊 Projected Impact", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (increase >= 0) "↑ +${((increase * 10).toInt() / 10.0)}%" else "↓ ${((increase * 10).toInt() / 10.0)}%",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (increase >= 0) PositiveGreen else NegativeRed
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "salary change if you learn ${state.targetMetrics!!.skillName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Target salary: ${state.targetMetrics!!.avgSalary.toSalaryString(location)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else if (state.currentSkillIds.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Select your current skills to get started", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

