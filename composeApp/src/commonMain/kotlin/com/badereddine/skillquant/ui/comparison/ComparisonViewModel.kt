package com.badereddine.skillquant.ui.comparison

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.badereddine.skillquant.domain.model.Skill
import com.badereddine.skillquant.domain.model.SkillMetrics
import com.badereddine.skillquant.domain.repository.SkillRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ComparisonUiState(
    val allSkills: List<Skill> = emptyList(),
    val skillA: SkillMetrics? = null,
    val skillB: SkillMetrics? = null,
    val selectedIdA: String = "",
    val selectedIdB: String = "",
    val isLoading: Boolean = false,
    val location: String = "Morocco"
)

class ComparisonViewModel(
    private val skillRepository: SkillRepository,
    location: String
) : ScreenModel {

    private val _uiState = MutableStateFlow(ComparisonUiState(location = location))
    val uiState: StateFlow<ComparisonUiState> = _uiState.asStateFlow()

    init {
        screenModelScope.launch {
            skillRepository.getAllSkillsForLocation(location).collect { skills ->
                _uiState.update { it.copy(allSkills = skills) }
            }
        }
    }

    fun selectSkillA(skillId: String) {
        _uiState.update { it.copy(selectedIdA = skillId, isLoading = true) }
        screenModelScope.launch {
            skillRepository.getSkillMetrics(skillId, _uiState.value.location).collect { m ->
                _uiState.update { it.copy(skillA = m, isLoading = false) }
            }
        }
    }

    fun selectSkillB(skillId: String) {
        _uiState.update { it.copy(selectedIdB = skillId, isLoading = true) }
        screenModelScope.launch {
            skillRepository.getSkillMetrics(skillId, _uiState.value.location).collect { m ->
                _uiState.update { it.copy(skillB = m, isLoading = false) }
            }
        }
    }
}

