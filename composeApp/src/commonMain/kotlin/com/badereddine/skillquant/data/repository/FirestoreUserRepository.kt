package com.badereddine.skillquant.data.repository

import com.badereddine.skillquant.domain.model.UserProfile
import com.badereddine.skillquant.domain.repository.UserRepository
import com.badereddine.skillquant.util.Constants
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.GoogleAuthProvider
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
                        displayName = runCatching { doc.get<String>("displayName") }.getOrNull() ?: "",
                        tier = runCatching { doc.get<String>("tier") }.getOrNull() ?: Constants.TIER_FREE,
                        watchlist = runCatching { doc.get<List<String>>("watchlist") }.getOrNull() ?: emptyList(),
                        currentSkills = runCatching { doc.get<List<String>>("currentSkills") }.getOrNull() ?: emptyList(),
                        notificationsEnabled = runCatching { doc.get<Boolean>("notificationsEnabled") }.getOrNull() ?: true,
                        fcmToken = runCatching { doc.get<String>("fcmToken") }.getOrNull() ?: "",
                        onboardingComplete = runCatching { doc.get<Boolean>("onboardingComplete") }.getOrNull() ?: false,
                        darkThemeOverride = runCatching { doc.get<String>("darkThemeOverride") }.getOrNull() ?: "system",
                        isAnonymous = runCatching { doc.get<Boolean>("isAnonymous") }.getOrNull() ?: true,
                        createdAt = runCatching { doc.get<Long>("createdAt") }.getOrNull() ?: 0L
                    )
                }.getOrNull()
            }
    }

    // ...existing code for updateWatchlist, addToWatchlist, removeFromWatchlist...
    override suspend fun updateWatchlist(userId: String, skillIds: List<String>) {
        firestore.collection(Constants.COLLECTION_USER_PROFILES).document(userId)
            .update("watchlist" to skillIds)
    }

    override suspend fun addToWatchlist(userId: String, skillId: String) {
        firestore.collection(Constants.COLLECTION_USER_PROFILES).document(userId)
            .update("watchlist" to FieldValue.arrayUnion(skillId))
    }

    override suspend fun removeFromWatchlist(userId: String, skillId: String) {
        firestore.collection(Constants.COLLECTION_USER_PROFILES).document(userId)
            .update("watchlist" to FieldValue.arrayRemove(skillId))
    }

    override suspend fun updateNotificationPrefs(userId: String, enabled: Boolean) {
        firestore.collection(Constants.COLLECTION_USER_PROFILES).document(userId)
            .update("notificationsEnabled" to enabled)
    }

    override suspend fun updateThemePreference(userId: String, theme: String) {
        firestore.collection(Constants.COLLECTION_USER_PROFILES).document(userId)
            .update("darkThemeOverride" to theme)
    }

    override suspend fun updateCurrentSkills(userId: String, skills: List<String>) {
        firestore.collection(Constants.COLLECTION_USER_PROFILES).document(userId)
            .update("currentSkills" to skills)
    }

    override suspend fun completeOnboarding(userId: String) {
        firestore.collection(Constants.COLLECTION_USER_PROFILES).document(userId)
            .update("onboardingComplete" to true)
    }

    override suspend fun signInAnonymously(): String {
        val result = auth.signInAnonymously()
        val userId = result.user?.uid ?: throw IllegalStateException("Anonymous sign-in failed")
        ensureProfileExists(userId, isAnonymous = true)
        return userId
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    override fun isAnonymous(): Boolean {
        return auth.currentUser?.isAnonymous ?: true
    }

    override suspend fun linkGoogleAccount(idToken: String): String {
        val credential = GoogleAuthProvider.credential(idToken, null)
        return try {
            // Try to link the anonymous account to Google (preserves UID + data)
            val result = auth.currentUser!!.linkWithCredential(credential)
            val user = result.user!!
            updateProfileAfterGoogleLink(user.uid, user.displayName, user.email)
            user.uid
        } catch (e: Exception) {
            // If "credential-already-in-use", the Google account was used before.
            // Fall back to signInWithCredential — this creates/restores the Google user.
            val result = auth.signInWithCredential(credential)
            val user = result.user!!
            ensureProfileExists(user.uid, isAnonymous = false)
            updateProfileAfterGoogleLink(user.uid, user.displayName, user.email)
            user.uid
        }
    }

    override suspend fun signInWithGoogle(idToken: String): String {
        val credential = GoogleAuthProvider.credential(idToken, null)
        val result = auth.signInWithCredential(credential)
        val user = result.user ?: throw IllegalStateException("Google sign-in failed")
        ensureProfileExists(user.uid, isAnonymous = false)
        updateProfileAfterGoogleLink(user.uid, user.displayName, user.email)
        return user.uid
    }

    override suspend fun signOut() {
        auth.signOut()
        // Create a new anonymous session so the app continues to work
        signInAnonymously()
    }

    private suspend fun ensureProfileExists(userId: String, isAnonymous: Boolean) {
        val docRef = firestore.collection(Constants.COLLECTION_USER_PROFILES).document(userId)
        val doc = docRef.get()
        if (!doc.exists) {
            docRef.set(
                mapOf(
                    "email" to "",
                    "displayName" to "",
                    "tier" to Constants.TIER_FREE,
                    "watchlist" to emptyList<String>(),
                    "notificationsEnabled" to true,
                    "fcmToken" to "",
                    "isAnonymous" to isAnonymous,
                    "createdAt" to Clock.System.now().toEpochMilliseconds()
                )
            )
        }
    }

    private suspend fun updateProfileAfterGoogleLink(userId: String, displayName: String?, email: String?) {
        val updates = mutableMapOf<String, Any>(
            "isAnonymous" to false
        )
        if (!displayName.isNullOrBlank()) updates["displayName"] = displayName
        if (!email.isNullOrBlank()) updates["email"] = email

        val docRef = firestore.collection(Constants.COLLECTION_USER_PROFILES).document(userId)
        // Use set with merge to handle both existing and new docs
        docRef.update(updates)
    }
}

