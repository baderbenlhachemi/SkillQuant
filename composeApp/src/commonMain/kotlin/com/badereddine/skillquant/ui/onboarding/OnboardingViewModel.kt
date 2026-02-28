package com.badereddine.skillquant.ui.onboarding

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.badereddine.skillquant.domain.model.Skill
import com.badereddine.skillquant.domain.repository.SkillRepository
import com.badereddine.skillquant.domain.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val step: Int = 0, // 0=welcome, 1=pick skills, 2=done
    val allSkills: List<Skill> = emptyList(),
    val selectedSkills: List<String> = emptyList(),
    val isSaving: Boolean = false
)

class OnboardingViewModel(
    private val skillRepository: SkillRepository,
    private val userRepository: UserRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        screenModelScope.launch {
            skillRepository.getAllSkills().collect { skills ->
                _uiState.update { it.copy(allSkills = skills) }
            }
        }
    }

    fun nextStep() {
        _uiState.update { it.copy(step = it.step + 1) }
    }

    fun toggleSkill(skillId: String) {
        val current = _uiState.value.selectedSkills.toMutableList()
        if (skillId in current) current.remove(skillId) else current.add(skillId)
        _uiState.update { it.copy(selectedSkills = current) }
    }

    fun finishOnboarding(onComplete: () -> Unit) {
        _uiState.update { it.copy(isSaving = true) }
        screenModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId()
                    ?: userRepository.signInAnonymously()
                userRepository.updateCurrentSkills(userId, _uiState.value.selectedSkills)
                userRepository.completeOnboarding(userId)
                onComplete()
            } catch (_: Exception) {
                onComplete() // Still navigate away
            }
        }
    }

    fun skipOnboarding(onComplete: () -> Unit) {
        screenModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId()
                    ?: userRepository.signInAnonymously()
                userRepository.completeOnboarding(userId)
            } catch (_: Exception) { }
            onComplete()
        }
    }
}

