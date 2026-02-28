package com.badereddine.skillquant.domain.repository

import com.badereddine.skillquant.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserProfile(userId: String): Flow<UserProfile?>
    suspend fun updateWatchlist(userId: String, skillIds: List<String>)
    suspend fun addToWatchlist(userId: String, skillId: String)
    suspend fun removeFromWatchlist(userId: String, skillId: String)
    suspend fun updateNotificationPrefs(userId: String, enabled: Boolean)
    suspend fun updateThemePreference(userId: String, theme: String)
    suspend fun updateCurrentSkills(userId: String, skills: List<String>)
    suspend fun completeOnboarding(userId: String)
    suspend fun signInAnonymously(): String
    fun getCurrentUserId(): String?
    fun isAnonymous(): Boolean
    suspend fun linkGoogleAccount(idToken: String): String
    suspend fun signInWithGoogle(idToken: String): String
    suspend fun signOut()
}

