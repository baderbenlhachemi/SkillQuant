package com.badereddine.skillquant.auth

/**
 * Platform-specific Google Sign-In helper.
 * Returns a Google ID token string that can be used with Firebase Auth.
 */
expect class GoogleAuthHelper {
    /**
     * Launch the Google Sign-In flow and return the ID token.
     * Returns null if the user cancels or sign-in fails.
     */
    suspend fun getGoogleIdToken(): String?
}

/**
 * Whether Google Sign-In is available on this platform.
 */
expect val isGoogleSignInAvailable: Boolean

