package com.badereddine.skillquant.data.repository

import com.badereddine.skillquant.domain.model.UserProfile
import com.badereddine.skillquant.domain.repository.UserRepository
import com.badereddine.skillquant.util.Constants
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class FirestoreUserRepository(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : UserRepository {

    override fun getUserProfile(userId: String): Flow<UserProfile?> {
        return firestore
            .collection(Constants.COLLECTION_USER_PROFILES)
            .document(userId)
            .snapshots
            .map { doc ->
                if (!doc.exists) return@map null
                runCatching {
                    UserProfile(
                        id = doc.id,
                        email = runCatching { doc.get<String>("email") }.getOrNull() ?: "",
                        tier = runCatching { doc.get<String>("tier") }.getOrNull() ?: Constants.TIER_FREE,
                        watchlist = runCatching { doc.get<List<String>>("watchlist") }.getOrNull() ?: emptyList(),
                        currentSkills = runCatching { doc.get<List<String>>("currentSkills") }.getOrNull() ?: emptyList(),
                        notificationsEnabled = runCatching { doc.get<Boolean>("notificationsEnabled") }.getOrNull() ?: true,
                        fcmToken = runCatching { doc.get<String>("fcmToken") }.getOrNull() ?: "",
                        onboardingComplete = runCatching { doc.get<Boolean>("onboardingComplete") }.getOrNull() ?: false,
                        darkThemeOverride = runCatching { doc.get<String>("darkThemeOverride") }.getOrNull() ?: "system",
                        createdAt = runCatching { doc.get<Long>("createdAt") }.getOrNull() ?: 0L
                    )
                }.getOrNull()
            }
    }

    override suspend fun updateWatchlist(userId: String, skillIds: List<String>) {
        firestore
            .collection(Constants.COLLECTION_USER_PROFILES)
            .document(userId)
            .update("watchlist" to skillIds)
    }

    override suspend fun addToWatchlist(userId: String, skillId: String) {
        firestore
            .collection(Constants.COLLECTION_USER_PROFILES)
            .document(userId)
            .update("watchlist" to FieldValue.arrayUnion(skillId))
    }

    override suspend fun removeFromWatchlist(userId: String, skillId: String) {
        firestore
            .collection(Constants.COLLECTION_USER_PROFILES)
            .document(userId)
            .update("watchlist" to FieldValue.arrayRemove(skillId))
    }

    override suspend fun updateNotificationPrefs(userId: String, enabled: Boolean) {
        firestore
            .collection(Constants.COLLECTION_USER_PROFILES)
            .document(userId)
            .update("notificationsEnabled" to enabled)
    }

    override suspend fun updateThemePreference(userId: String, theme: String) {
        firestore
            .collection(Constants.COLLECTION_USER_PROFILES)
            .document(userId)
            .update("darkThemeOverride" to theme)
    }

    override suspend fun updateCurrentSkills(userId: String, skills: List<String>) {
        firestore
            .collection(Constants.COLLECTION_USER_PROFILES)
            .document(userId)
            .update("currentSkills" to skills)
    }

    override suspend fun completeOnboarding(userId: String) {
        firestore
            .collection(Constants.COLLECTION_USER_PROFILES)
            .document(userId)
            .update("onboardingComplete" to true)
    }

    override suspend fun signInAnonymously(): String {
        val result = auth.signInAnonymously()
        val userId = result.user?.uid ?: throw IllegalStateException("Anonymous sign-in failed")

        // Create profile doc if it doesn't exist
        val docRef = firestore
            .collection(Constants.COLLECTION_USER_PROFILES)
            .document(userId)

        val doc = docRef.get()
        if (!doc.exists) {
            docRef.set(
                mapOf(
                    "email" to "",
                    "tier" to Constants.TIER_FREE,
                    "watchlist" to emptyList<String>(),
                    "notificationsEnabled" to true,
                    "fcmToken" to "",
                    "createdAt" to Clock.System.now().toEpochMilliseconds()
                )
            )
        }
        return userId
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}

