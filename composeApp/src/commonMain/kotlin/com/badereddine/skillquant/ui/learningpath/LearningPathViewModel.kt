package com.badereddine.skillquant.ui.learningpath

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.badereddine.skillquant.domain.model.SkillMetrics
import com.badereddine.skillquant.domain.repository.SkillRepository
import com.badereddine.skillquant.domain.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class LearningPathUiState(
    val pathItems: List<SkillMetrics> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false
)

class LearningPathViewModel(
    private val skillRepository: SkillRepository,
    private val userRepository: UserRepository,
    private val location: String
) : ScreenModel {

    private val _uiState = MutableStateFlow(LearningPathUiState())
    val uiState: StateFlow<LearningPathUiState> = _uiState.asStateFlow()

    init {
        screenModelScope.launch {
            val userId = userRepository.getCurrentUserId() ?: run {
                _uiState.update { it.copy(isLoading = false, isEmpty = true) }; return@launch
            }
            userRepository.getUserProfile(userId).collect { profile ->
                val watchlist = profile?.watchlist ?: emptyList()
                if (watchlist.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, isEmpty = true) }
                    return@collect
                }
                skillRepository.getSkillMetricsList(watchlist, location).collect { metrics ->
                    val sorted = metrics.sortedByDescending { it.arbitrageScore }
                    _uiState.update { it.copy(pathItems = sorted, isLoading = false, isEmpty = sorted.isEmpty()) }
                }
            }
        }
    }
}

