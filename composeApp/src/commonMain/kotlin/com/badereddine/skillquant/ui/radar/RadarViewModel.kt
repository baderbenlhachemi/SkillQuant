package com.badereddine.skillquant.ui.radar

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.badereddine.skillquant.domain.model.SkillMetrics
import com.badereddine.skillquant.domain.repository.SkillRepository
import com.badereddine.skillquant.domain.repository.UserRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RadarUiState(
    val metrics: List<SkillMetrics> = emptyList(),
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false
)

class RadarViewModel(
    private val skillRepository: SkillRepository,
    private val userRepository: UserRepository,
    private val location: String
) : ScreenModel {

    private val _uiState = MutableStateFlow(RadarUiState())
    val uiState: StateFlow<RadarUiState> = _uiState.asStateFlow()

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
                skillRepository.getSkillMetricsList(watchlist, location).collect { m ->
                    _uiState.update { it.copy(metrics = m, isLoading = false, isEmpty = m.isEmpty()) }
                }
            }
        }
    }
}

