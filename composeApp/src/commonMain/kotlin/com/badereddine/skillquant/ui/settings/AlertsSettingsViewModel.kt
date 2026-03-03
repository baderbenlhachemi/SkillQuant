package com.badereddine.skillquant.ui.settings

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.badereddine.skillquant.auth.GoogleAuthHelper
import com.badereddine.skillquant.domain.model.Alert
import com.badereddine.skillquant.domain.model.UserProfile
import com.badereddine.skillquant.domain.repository.AlertRepository
import com.badereddine.skillquant.domain.repository.SkillRepository
import com.badereddine.skillquant.domain.repository.UserRepository
import com.badereddine.skillquant.util.Constants
import com.badereddine.skillquant.util.ThemeManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val alerts: List<Alert> = emptyList(),
    val userProfile: UserProfile? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val skillNames: Map<String, String> = emptyMap(),
    val themeMode: String = "system",
    val isAnonymous: Boolean = true,
    val isSigningIn: Boolean = false,
    val authMessage: String? = null
) {
    val isPro: Boolean get() = userProfile?.tier == Constants.TIER_PRO
    val watchlistCount: Int get() = userProfile?.watchlist?.size ?: 0
    val watchlistLimit: Int get() = if (isPro) Int.MAX_VALUE else Constants.FREE_WATCHLIST_LIMIT
    val notificationsEnabled: Boolean get() = userProfile?.notificationsEnabled ?: true
    val displayName: String get() = userProfile?.displayName ?: ""
    val email: String get() = userProfile?.email ?: ""
}

class AlertsSettingsViewModel(
    private val alertRepository: AlertRepository,
    private val userRepository: UserRepository,
    private val skillRepository: SkillRepository,
    private val themeManager: ThemeManager,
    private val googleAuthHelper: GoogleAuthHelper
) : ScreenModel {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Track listener jobs so we can cancel + restart them on auth change
    private var profileJob: Job? = null
    private var alertsJob: Job? = null

    init {
        _uiState.update {
            it.copy(
                themeMode = themeManager.themeMode.value,
                isAnonymous = userRepository.isAnonymous()
            )
        }
        loadSettings()
    }

    private fun loadSettings() {
        screenModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId()
                    ?: userRepository.signInAnonymously()

                startListeners(userId)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /** Cancel existing Firestore listeners then re-subscribe for [userId]. */
    private fun startListeners(userId: String) {
        // Cancel stale listeners before creating new ones — prevents double-subscriptions
        // and the coroutine-scope crash that looks like an app close on Samsung devices.
        profileJob?.cancel()
        alertsJob?.cancel()

        profileJob = screenModelScope.launch {
            userRepository.getUserProfile(userId).collect { profile ->
                _uiState.update {
                    it.copy(
                        userProfile = profile,
                        isLoading = false,
                        isAnonymous = userRepository.isAnonymous()
                    )
                }
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

        alertsJob = screenModelScope.launch {
            alertRepository.getAlerts(userId).collect { alerts ->
                _uiState.update { it.copy(alerts = alerts) }
            }
        }
    }

    fun signInWithGoogle() {
        screenModelScope.launch {
            _uiState.update { it.copy(isSigningIn = true, authMessage = null) }
            try {
                val idToken = googleAuthHelper.getGoogleIdToken()
                if (idToken == null) {
                    _uiState.update { it.copy(isSigningIn = false) }
                    return@launch
                }

                // Link or sign in, get the resulting user ID
                val newUserId = if (userRepository.isAnonymous()) {
                    userRepository.linkGoogleAccount(idToken)
                } else {
                    userRepository.signInWithGoogle(idToken)
                }

                // Update state first so the UI reflects the new account immediately
                _uiState.update {
                    it.copy(
                        isSigningIn = false,
                        isAnonymous = false,
                        authMessage = "✅ Signed in successfully!"
                    )
                }

                // Restart listeners for the (possibly new) user ID without re-entering loadSettings
                startListeners(newUserId)

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSigningIn = false,
                        authMessage = e.message ?: "Sign-in failed"
                    )
                }
            }
        }
    }

    fun signOut() {
        screenModelScope.launch {
            try {
                // signOut() internally signs in anonymously, so getCurrentUserId() is
                // always non-null immediately after — no second signInAnonymously() needed.
                userRepository.signOut()
                val newUserId = userRepository.getCurrentUserId()
                    ?: return@launch // should never happen

                _uiState.update {
                    it.copy(
                        isAnonymous = true,
                        authMessage = "Signed out",
                        userProfile = null,
                        skillNames = emptyMap(),
                        alerts = emptyList()
                    )
                }

                // Restart listeners for the new anonymous user
                startListeners(newUserId)

            } catch (e: Exception) {
                _uiState.update { it.copy(authMessage = "Sign-out failed: ${e.message}") }
            }
        }
    }

    fun clearAuthMessage() {
        _uiState.update { it.copy(authMessage = null) }
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
