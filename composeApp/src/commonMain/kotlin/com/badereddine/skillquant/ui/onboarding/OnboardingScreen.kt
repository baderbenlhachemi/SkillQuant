package com.badereddine.skillquant.ui.onboarding

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
import com.badereddine.skillquant.ui.theme.TealPrimary

class OnboardingScreen : Screen {
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<OnboardingViewModel>()
        val state by viewModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (state.step) {
                0 -> WelcomeStep(
                    onNext = { viewModel.nextStep() },
                    onSkip = { viewModel.skipOnboarding { navigator.pop() } }
                )
                1 -> PickSkillsStep(
                    skills = state.allSkills.map { it.id to it.name },
                    selectedIds = state.selectedSkills,
                    onToggle = { viewModel.toggleSkill(it) },
                    onNext = { viewModel.nextStep() }
                )
                2 -> DoneStep(
                    isSaving = state.isSaving,
                    count = state.selectedSkills.size,
                    onFinish = { viewModel.finishOnboarding { navigator.pop() } }
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📊", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(24.dp))
        Text("Welcome to SkillQuant", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "Discover where to invest your learning effort.\nFind the highest-ROI skills in your market.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Get Started", modifier = Modifier.padding(vertical = 4.dp))
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onSkip) {
            Text("Skip for now")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PickSkillsStep(
    skills: List<Pair<String, String>>,
    selectedIds: List<String>,
    onToggle: (String) -> Unit,
    onNext: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredSkills = remember(skills, searchQuery) {
        if (searchQuery.isBlank()) skills
        else skills.filter { (_, name) ->
            name.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Text("What skills do you have?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "Select your current skills so we can personalize recommendations.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search skills…") },
            leadingIcon = { Text("🔍") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Text("✕", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(8.dp))

        Text(
            "${selectedIds.size} selected · ${filteredSkills.size} shown",
            style = MaterialTheme.typography.labelMedium,
            color = TealPrimary,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(8.dp))

        // Scrollable skill chips area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val scrollState = rememberScrollState()
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                filteredSkills.forEach { (id, name) ->
                    val selected = id in selectedIds
                    FilterChip(
                        selected = selected,
                        onClick = { onToggle(id) },
                        label = { Text(name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealPrimary.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = selectedIds.isNotEmpty()
        ) {
            Text("Continue (${selectedIds.size} selected)", modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun DoneStep(isSaving: Boolean, count: Int, onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🎉", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(24.dp))
        Text("You're all set!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            "$count skills selected.\nWe'll show you personalized insights.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onFinish,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Let's Go! 🚀", modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}

