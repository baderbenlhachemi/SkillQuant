package com.badereddine.skillquant.ui.calculator

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.badereddine.skillquant.domain.model.Skill
import com.badereddine.skillquant.domain.model.SkillMetrics
import com.badereddine.skillquant.domain.repository.SkillRepository
import com.badereddine.skillquant.domain.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CalcUiState(
    val allSkills: List<Skill> = emptyList(),
    val currentSkillIds: List<String> = emptyList(),
    val targetSkillId: String = "",
    val currentMetrics: List<SkillMetrics> = emptyList(),
    val targetMetrics: SkillMetrics? = null,
    val salaryIncrease: Double? = null,
    val isLoading: Boolean = false,
    val location: String = "Morocco"
)

class SalaryCalculatorViewModel(
    private val skillRepository: SkillRepository,
    private val userRepository: UserRepository,
    location: String
) : ScreenModel {

    private val _uiState = MutableStateFlow(CalcUiState(location = location))
    val uiState: StateFlow<CalcUiState> = _uiState.asStateFlow()

    init {
        screenModelScope.launch {
            skillRepository.getAllSkillsForLocation(location).collect { skills ->
                _uiState.update { it.copy(allSkills = skills) }
            }
        }
        // Load user's current skills
        screenModelScope.launch {
            val userId = userRepository.getCurrentUserId() ?: return@launch
            userRepository.getUserProfile(userId).collect { profile ->
                profile?.currentSkills?.let { ids ->
                    _uiState.update { it.copy(currentSkillIds = ids) }
                    loadCurrentMetrics(ids)
                }
            }
        }
    }

    fun toggleCurrentSkill(skillId: String) {
        val current = _uiState.value.currentSkillIds.toMutableList()
        if (skillId in current) current.remove(skillId) else current.add(skillId)
        _uiState.update { it.copy(currentSkillIds = current) }
        loadCurrentMetrics(current)
        recalculate()
    }

    fun selectTargetSkill(skillId: String) {
        _uiState.update { it.copy(targetSkillId = skillId, isLoading = true) }
        screenModelScope.launch {
            skillRepository.getSkillMetrics(skillId, _uiState.value.location).collect { m ->
                _uiState.update { it.copy(targetMetrics = m, isLoading = false) }
                recalculate()
            }
        }
    }

    private fun loadCurrentMetrics(ids: List<String>) {
        if (ids.isEmpty()) return
        screenModelScope.launch {
            skillRepository.getSkillMetricsList(ids, _uiState.value.location).collect { metrics ->
                _uiState.update { it.copy(currentMetrics = metrics) }
                recalculate()
            }
        }
    }

    private fun recalculate() {
        val state = _uiState.value
        val currentAvg = state.currentMetrics.map { it.avgSalary }.average().takeIf { !it.isNaN() } ?: return
        val target = state.targetMetrics ?: return
        if (currentAvg <= 0) return
        val increase = ((target.avgSalary - currentAvg) / currentAvg) * 100
        _uiState.update { it.copy(salaryIncrease = increase) }
    }
}

