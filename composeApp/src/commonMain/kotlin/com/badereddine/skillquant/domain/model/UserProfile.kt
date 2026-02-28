package com.badereddine.skillquant.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",
    val tier: String = "free",
    val watchlist: List<String> = emptyList(),
    val currentSkills: List<String> = emptyList(),
    val notificationsEnabled: Boolean = true,
    val fcmToken: String = "",
    val onboardingComplete: Boolean = false,
    val darkThemeOverride: String = "system",  // "system", "dark", "light"
    val isAnonymous: Boolean = true,
    val createdAt: Long = 0L
)

