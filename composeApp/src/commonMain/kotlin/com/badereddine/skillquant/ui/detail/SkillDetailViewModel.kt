package com.badereddine.skillquant.ui.detail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.badereddine.skillquant.domain.model.SkillMetrics
import com.badereddine.skillquant.domain.repository.SkillRepository
import com.badereddine.skillquant.domain.repository.UserRepository
import com.badereddine.skillquant.util.Constants
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

data class SkillDetailUiState(
    val metrics: SkillMetrics? = null,
    val isLoading: Boolean = true,
    val isOnWatchlist: Boolean = false,
    val isTogglingWatchlist: Boolean = false,
    val userTier: String = Constants.TIER_FREE,
    val watchlistFull: Boolean = false,
    val error: String? = null,
    val selectedTrend: TrendType = TrendType.DEMAND
)

enum class TrendType { DEMAND, SALARY }

class SkillDetailViewModel(
    private val skillId: String,
    private val location: String,
    private val skillRepository: SkillRepository,
    private val userRepository: UserRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(SkillDetailUiState())
    val uiState: StateFlow<SkillDetailUiState> = _uiState.asStateFlow()

    private val supervisedScope = screenModelScope + SupervisorJob()

    init {
        loadSkillDetail()
    }

    private fun loadSkillDetail() {
        supervisedScope.launch {
            // Load skill metrics immediately — no auth needed
            launch {
                skillRepository.getSkillMetrics(skillId, location)
                    .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                    .collect { metrics ->
                        _uiState.update { it.copy(metrics = metrics, isLoading = false) }
                    }
            }

            // Auth + watchlist — fails gracefully
            launch {
                try {
                    val userId = userRepository.getCurrentUserId()
                        ?: userRepository.signInAnonymously()

                    userRepository.getUserProfile(userId)
                        .catch { /* auth changed — no-op */ }
                        .collect { profile ->
                            _uiState.update {
                                it.copy(
                                    isOnWatchlist = profile?.watchlist?.contains(skillId) == true,
                                    userTier = profile?.tier ?: Constants.TIER_FREE,
                                    watchlistFull = profile?.tier == Constants.TIER_FREE &&
                                            (profile.watchlist.size >= Constants.FREE_WATCHLIST_LIMIT)
                                )
                            }
                        }
                } catch (_: Exception) { }
            }
        }
    }

    fun toggleWatchlist() {
        val currentState = _uiState.value
        if (currentState.isTogglingWatchlist) return // prevent double-tap

        screenModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId() ?: return@launch
                val wasOnWatchlist = currentState.isOnWatchlist

                if (!wasOnWatchlist) {
                    if (currentState.watchlistFull && currentState.userTier == Constants.TIER_FREE) {
                        return@launch
                    }
                }

                // Optimistic UI update
                _uiState.update {
                    it.copy(isOnWatchlist = !wasOnWatchlist, isTogglingWatchlist = true)
                }

                if (wasOnWatchlist) {
                    userRepository.removeFromWatchlist(userId, skillId)
                } else {
                    userRepository.addToWatchlist(userId, skillId)
                }

                _uiState.update { it.copy(isTogglingWatchlist = false) }
            } catch (e: Exception) {
                // Revert on failure
                _uiState.update {
                    it.copy(
                        isOnWatchlist = currentState.isOnWatchlist,
                        isTogglingWatchlist = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun selectTrend(type: TrendType) {
        _uiState.update { it.copy(selectedTrend = type) }
    }
}
