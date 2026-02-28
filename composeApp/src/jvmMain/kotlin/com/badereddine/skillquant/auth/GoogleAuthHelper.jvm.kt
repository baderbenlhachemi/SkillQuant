package com.badereddine.skillquant.auth

actual class GoogleAuthHelper {
    actual suspend fun getGoogleIdToken(): String? {
        throw UnsupportedOperationException("Google Sign-In is not available on desktop")
    }
}

actual val isGoogleSignInAvailable: Boolean = false

