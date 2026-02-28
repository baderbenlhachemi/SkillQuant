package com.badereddine.skillquant.ui.settings

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.badereddine.skillquant.domain.model.Alert
import com.badereddine.skillquant.domain.model.UserProfile
import com.badereddine.skillquant.domain.repository.AlertRepository
import com.badereddine.skillquant.domain.repository.SkillRepository
import com.badereddine.skillquant.domain.repository.UserRepository
import com.badereddine.skillquant.util.Constants
import com.badereddine.skillquant.util.ThemeManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val alerts: List<Alert> = emptyList(),
    val userProfile: UserProfile? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val skillNames: Map<String, String> = emptyMap(),
    val themeMode: String = "system"
) {
    val isPro: Boolean get() = userProfile?.tier == Constants.TIER_PRO
    val watchlistCount: Int get() = userProfile?.watchlist?.size ?: 0
    val watchlistLimit: Int get() = if (isPro) Int.MAX_VALUE else Constants.FREE_WATCHLIST_LIMIT
    val notificationsEnabled: Boolean get() = userProfile?.notificationsEnabled ?: true
}

class AlertsSettingsViewModel(
    private val alertRepository: AlertRepository,
    private val userRepository: UserRepository,
    private val skillRepository: SkillRepository,
    private val themeManager: ThemeManager
) : ScreenModel {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(themeMode = themeManager.themeMode.value) }
        loadSettings()
    }

    private fun loadSettings() {
        screenModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId()
                    ?: userRepository.signInAnonymously()

                launch {
                    userRepository.getUserProfile(userId).collect { profile ->
                        _uiState.update { it.copy(userProfile = profile, isLoading = false) }
                        // Resolve skill names for watchlist
                        profile?.watchlist?.forEach { skillId ->
                            if (skillId !in _uiState.value.skillNames) {
                                launch {
                                    val name = skillRepository.getSkillName(skillId)
                                    _uiState.update {
                                        it.copy(skillNames = it.skillNames + (skillId to name))
                                    }
                                }
                            }
                        }
                    }
                }

                launch {
                    alertRepository.getAlerts(userId).collect { alerts ->
                        _uiState.update { it.copy(alerts = alerts) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun toggleNotifications() {
        screenModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId() ?: return@launch
                val current = _uiState.value.notificationsEnabled
                userRepository.updateNotificationPrefs(userId, !current)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun removeFromWatchlist(skillId: String) {
        screenModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId() ?: return@launch
                userRepository.removeFromWatchlist(userId, skillId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun markAlertAsRead(alertId: String) {
        screenModelScope.launch {
            try {
                alertRepository.markAsRead(alertId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun markAllAlertsAsRead() {
        screenModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId() ?: return@launch
                alertRepository.markAllAsRead(userId)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun setTheme(mode: String) {
        themeManager.setThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
        screenModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId() ?: return@launch
                userRepository.updateThemePreference(userId, mode)
            } catch (_: Exception) {}
        }
    }
}
