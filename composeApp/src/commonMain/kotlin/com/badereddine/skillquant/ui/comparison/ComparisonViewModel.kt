package com.badereddine.skillquant.ui.comparison

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.badereddine.skillquant.domain.model.Skill
import com.badereddine.skillquant.domain.model.SkillMetrics
import com.badereddine.skillquant.domain.repository.SkillRepository
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

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

    private val supervisedScope = screenModelScope + SupervisorJob()

    init {
        supervisedScope.launch {
            skillRepository.getAllSkillsForLocation(location)
                .catch { }
                .collect { skills -> _uiState.update { it.copy(allSkills = skills) } }
        }
    }

    fun selectSkillA(skillId: String) {
        _uiState.update { it.copy(selectedIdA = skillId, isLoading = true) }
        supervisedScope.launch {
            skillRepository.getSkillMetrics(skillId, _uiState.value.location)
                .catch { _uiState.update { it.copy(isLoading = false) } }
                .collect { m -> _uiState.update { it.copy(skillA = m, isLoading = false) } }
        }
    }

    fun selectSkillB(skillId: String) {
        _uiState.update { it.copy(selectedIdB = skillId, isLoading = true) }
        supervisedScope.launch {
            skillRepository.getSkillMetrics(skillId, _uiState.value.location)
                .catch { _uiState.update { it.copy(isLoading = false) } }
                .collect { m -> _uiState.update { it.copy(skillB = m, isLoading = false) } }
        }
    }
}
