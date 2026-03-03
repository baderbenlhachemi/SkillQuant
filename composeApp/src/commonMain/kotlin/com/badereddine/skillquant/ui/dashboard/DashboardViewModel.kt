package com.badereddine.skillquant.ui.dashboard

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.badereddine.skillquant.domain.model.Alert
import com.badereddine.skillquant.domain.model.ArbitrageOpportunity
import com.badereddine.skillquant.domain.model.Skill
import com.badereddine.skillquant.domain.model.TrendingSkill
import com.badereddine.skillquant.domain.repository.AlertRepository
import com.badereddine.skillquant.domain.repository.SkillRepository
import com.badereddine.skillquant.domain.repository.UserRepository
import com.badereddine.skillquant.util.Constants
import com.badereddine.skillquant.util.ThemeManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val arbitrageOpportunities: List<ArbitrageOpportunity> = emptyList(),
    val trendingSkills: List<TrendingSkill> = emptyList(),
    val recentAlerts: List<Alert> = emptyList(),
    val unreadAlertCount: Int = 0,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val isOffline: Boolean = false,
    val showOnboarding: Boolean = false,
    val userTier: String = Constants.TIER_FREE,
    val searchQuery: String = "",
    val searchResults: List<Skill> = emptyList(),
    val isSearching: Boolean = false,
    val selectedLocation: String = "Morocco",
    val availableLocations: List<String> = listOf("Morocco", "France", "USA")
)

class DashboardViewModel(
    private val skillRepository: SkillRepository,
    private val alertRepository: AlertRepository,
    private val userRepository: UserRepository,
    private val themeManager: ThemeManager
) : ScreenModel {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null
    private var dataJob: Job? = null

    private fun isNetworkError(e: Exception): Boolean {
        val msg = (e.message ?: "").lowercase()
        return msg.contains("unavailable") ||
                msg.contains("network") ||
                msg.contains("connect") ||
                msg.contains("unreachable") ||
                msg.contains("timeout") ||
                msg.contains("failed to get") ||
                msg.contains("internet") ||
                msg.contains("offline") ||
                msg.contains("grpc") ||
                msg.contains("channel") ||
                msg.contains("dns")
    }

    init {
        loadDashboard()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        _uiState.update { it.copy(isSearching = true) }
        searchJob = screenModelScope.launch {
            delay(300)
            try {
                val location = _uiState.value.selectedLocation
                skillRepository.searchSkills(query, location).collect { results ->
                    _uiState.update { it.copy(searchResults = results, isSearching = false) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _uiState.update { it.copy(isSearching = false) }
            }
        }
    }

    fun onLocationSelected(location: String) {
        if (location == _uiState.value.selectedLocation) return
        dataJob?.cancel()
        _uiState.update {
            it.copy(
                selectedLocation = location,
                isLoading = true,
                error = null,
                isOffline = false,
                arbitrageOpportunities = emptyList(),
                trendingSkills = emptyList()
            )
        }
        loadData(location)
    }

    private fun loadDashboard() {
        screenModelScope.launch {
            // ── Step 1: ensure we have an authenticated session FIRST ──────────
            // Running loadData() before auth completes causes a permission_denied
            // flash on first launch because Firestore rejects unauthenticated reads.
            try {
                if (userRepository.getCurrentUserId() == null) {
                    userRepository.signInAnonymously()
                }
            } catch (_: Exception) {
                // If anonymous sign-in fails (offline), proceed anyway —
                // loadData will show the offline/error state instead.
            }

            // ── Step 2: now safe to start data + profile listeners in parallel ─
            val location = _uiState.value.selectedLocation
            loadData(location)
            loadAuth()
        }
    }

    private fun isPermissionError(e: Exception): Boolean {
        val msg = (e.message ?: "").lowercase()
        return msg.contains("permission_denied") ||
               msg.contains("missing or insufficient") ||
               msg.contains("permission denied")
    }

    private fun loadData(location: String) {
        dataJob = screenModelScope.launch {
            launch {
                try {
                    skillRepository.getTopArbitrageOpportunities(10, location)
                        .collect { opportunities ->
                            _uiState.update {
                                it.copy(arbitrageOpportunities = opportunities, isLoading = false, isRefreshing = false)
                            }
                        }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Suppress permission errors — they are a transient auth-startup
                    // race and should never be shown to the user.
                    if (isPermissionError(e)) {
                        _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                isOffline = isNetworkError(e),
                                error = e.message
                            )
                        }
                    }
                }
            }

            launch {
                try {
                    skillRepository.getTrendingSkills(10, location).collect { trending ->
                        val arbIds = _uiState.value.arbitrageOpportunities.map { it.skillId }.toSet()
                        _uiState.update {
                            it.copy(trendingSkills = trending.filter { t -> t.skillId !in arbIds })
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { /* non-fatal */ }
            }
        }
    }

    private fun loadAuth() {
        screenModelScope.launch {
            try {
                // Auth is already guaranteed by loadDashboard() — just get the current ID
                val userId = userRepository.getCurrentUserId()
                    ?: userRepository.signInAnonymously()

                launch {
                    try {
                        userRepository.getUserProfile(userId).collect { profile ->
                            _uiState.update {
                                it.copy(
                                    userTier = profile?.tier ?: Constants.TIER_FREE,
                                    showOnboarding = profile?.onboardingComplete == false
                                )
                            }
                            profile?.darkThemeOverride?.let { themeManager.setThemeMode(it) }
                        }
                    } catch (_: Exception) {}
                }

                launch {
                    try {
                        alertRepository.getAlerts(userId).collect { alerts ->
                            _uiState.update {
                                it.copy(
                                    recentAlerts = alerts.take(3),
                                    unreadAlertCount = alerts.count { a -> !a.read }
                                )
                            }
                        }
                    } catch (_: Exception) {}
                }

                // If loadData() fired before auth was ready and got no results,
                // retry now that we are definitely authenticated.
                if (_uiState.value.arbitrageOpportunities.isEmpty() && !_uiState.value.isLoading) {
                    dataJob?.cancel()
                    loadData(_uiState.value.selectedLocation)
                }

            } catch (_: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true, error = null, isOffline = false) }
        dataJob?.cancel()
        loadData(_uiState.value.selectedLocation)
    }
}
