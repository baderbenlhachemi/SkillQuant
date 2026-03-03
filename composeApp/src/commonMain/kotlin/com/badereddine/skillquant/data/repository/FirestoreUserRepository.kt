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

        // Snapshot the anonymous user's data BEFORE any auth change
        val anonymousUid = auth.currentUser?.uid
        var anonWatchlist: List<String> = emptyList()
        var anonSkills: List<String> = emptyList()
        if (anonymousUid != null) {
            runCatching {
                val doc = firestore.collection(Constants.COLLECTION_USER_PROFILES)
                    .document(anonymousUid).get()
                if (doc.exists) {
                    anonWatchlist = runCatching { doc.get<List<String>>("watchlist") }.getOrNull() ?: emptyList()
                    anonSkills = runCatching { doc.get<List<String>>("currentSkills") }.getOrNull() ?: emptyList()
                }
            }
        }

        return try {
            // Happy path: link anonymous → Google (same UID, all data preserved)
            val result = auth.currentUser!!.linkWithCredential(credential)
            val user = result.user!!
            updateProfileAfterGoogleLink(user.uid, user.displayName, user.email, markOnboardingComplete = true)
            user.uid
        } catch (_: Exception) {
            // "credential-already-in-use": the Google account exists separately.
            // Sign in to that account, then MERGE the anonymous user's watchlist into it.
            val result = auth.signInWithCredential(credential)
            val user = result.user!!
            ensureProfileExists(user.uid, isAnonymous = false)
            updateProfileAfterGoogleLink(user.uid, user.displayName, user.email, markOnboardingComplete = true)

            // Migrate watchlist and currentSkills from the anonymous profile
            val docRef = firestore.collection(Constants.COLLECTION_USER_PROFILES).document(user.uid)
            if (anonWatchlist.isNotEmpty()) {
                docRef.update("watchlist" to FieldValue.arrayUnion(*anonWatchlist.toTypedArray()))
            }
            if (anonSkills.isNotEmpty()) {
                docRef.update("currentSkills" to FieldValue.arrayUnion(*anonSkills.toTypedArray()))
            }
            user.uid
        }
    }

    override suspend fun signInWithGoogle(idToken: String): String {
        val credential = GoogleAuthProvider.credential(idToken, null)
        val result = auth.signInWithCredential(credential)
        val user = result.user ?: throw IllegalStateException("Google sign-in failed")
        ensureProfileExists(user.uid, isAnonymous = false)
        updateProfileAfterGoogleLink(user.uid, user.displayName, user.email, markOnboardingComplete = true)
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
                    "currentSkills" to emptyList<String>(),
                    "notificationsEnabled" to true,
                    "fcmToken" to "",
                    "isAnonymous" to isAnonymous,
                    // Non-anonymous users coming from Google never need onboarding
                    "onboardingComplete" to !isAnonymous,
                    "createdAt" to Clock.System.now().toEpochMilliseconds()
                )
            )
        }
    }

    private suspend fun updateProfileAfterGoogleLink(
        userId: String,
        displayName: String?,
        email: String?,
        markOnboardingComplete: Boolean = false
    ) {
        val updates = mutableMapOf<String, Any>("isAnonymous" to false)
        if (!displayName.isNullOrBlank()) updates["displayName"] = displayName
        if (!email.isNullOrBlank()) updates["email"] = email
        if (markOnboardingComplete) updates["onboardingComplete"] = true

        firestore.collection(Constants.COLLECTION_USER_PROFILES).document(userId).update(updates)
    }
}

