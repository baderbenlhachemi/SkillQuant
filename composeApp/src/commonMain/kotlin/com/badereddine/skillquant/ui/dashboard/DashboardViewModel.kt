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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

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

    // SupervisorJob so a dying Firestore listener (PERMISSION_DENIED after auth change)
    // never kills sibling coroutines or the whole screen.
    private val supervisedScope = screenModelScope + SupervisorJob()

    private var searchJob: Job? = null
    private var dataJob: Job? = null

    private fun isNetworkError(e: Throwable): Boolean {
        val msg = (e.message ?: "").lowercase()
        return msg.contains("unavailable") || msg.contains("network") ||
                msg.contains("connect") || msg.contains("unreachable") ||
                msg.contains("timeout") || msg.contains("failed to get") ||
                msg.contains("internet") || msg.contains("offline") ||
                msg.contains("grpc") || msg.contains("channel") || msg.contains("dns")
    }

    private fun isPermissionError(e: Throwable): Boolean {
        val msg = (e.message ?: "").lowercase()
        return msg.contains("permission_denied") ||
               msg.contains("missing or insufficient") ||
               msg.contains("permission denied")
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
        searchJob = supervisedScope.launch {
            delay(300)
            try {
                val location = _uiState.value.selectedLocation
                skillRepository.searchSkills(query, location)
                    .catch { _uiState.update { s -> s.copy(isSearching = false) } }
                    .collect { results ->
                        _uiState.update { it.copy(searchResults = results, isSearching = false) }
                    }
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
        supervisedScope.launch {
            try {
                if (userRepository.getCurrentUserId() == null) {
                    userRepository.signInAnonymously()
                }
            } catch (_: Exception) { }
            val location = _uiState.value.selectedLocation
            loadData(location)
            loadAuth()
        }
    }

    private fun loadData(location: String) {
        dataJob = supervisedScope.launch {
            // Arbitrage opportunities
            launch {
                skillRepository.getTopArbitrageOpportunities(10, location)
                    .catch { e ->
                        // Swallow permission/auth errors inline — never show them to the user
                        if (!isPermissionError(e)) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false, isRefreshing = false,
                                    isOffline = isNetworkError(e), error = e.message
                                )
                            }
                        } else {
                            _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
                        }
                    }
                    .collect { opportunities ->
                        _uiState.update {
                            it.copy(arbitrageOpportunities = opportunities, isLoading = false, isRefreshing = false)
                        }
                    }
            }
            // Trending skills
            launch {
                skillRepository.getTrendingSkills(10, location)
                    .catch { /* non-fatal */ }
                    .collect { trending ->
                        val arbIds = _uiState.value.arbitrageOpportunities.map { it.skillId }.toSet()
                        _uiState.update {
                            it.copy(trendingSkills = trending.filter { t -> t.skillId !in arbIds })
                        }
                    }
            }
        }
    }

    private fun loadAuth() {
        supervisedScope.launch {
            try {
                val userId = userRepository.getCurrentUserId()
                    ?: userRepository.signInAnonymously()

                launch {
                    userRepository.getUserProfile(userId)
                        .catch { /* auth changed — no-op */ }
                        .collect { profile ->
                            _uiState.update {
                                it.copy(
                                    userTier = profile?.tier ?: Constants.TIER_FREE,
                                    // Never show onboarding if:
                                    //  - user is already Google-signed-in (not anonymous)
                                    //  - user already has items in their watchlist
                                    //  - onboardingComplete flag is set
                                    showOnboarding = profile?.onboardingComplete == false
                                        && (profile.isAnonymous)
                                        && profile.watchlist.isEmpty()
                                )
                            }
                            profile?.darkThemeOverride?.let { themeManager.setThemeMode(it) }
                        }
                }

                launch {
                    alertRepository.getAlerts(userId)
                        .catch { /* auth changed — no-op */ }
                        .collect { alerts ->
                            _uiState.update {
                                it.copy(
                                    recentAlerts = alerts.take(3),
                                    unreadAlertCount = alerts.count { a -> !a.read }
                                )
                            }
                        }
                }

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
