package com.badereddine.skillquant.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.badereddine.skillquant.util.Constants
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Sealed result for Google Sign-In attempts.
 */
sealed class GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult()
    data class Error(val message: String) : GoogleSignInResult()
    data object Cancelled : GoogleSignInResult()
}

actual class GoogleAuthHelper(private val context: Context) {

    // Activity reference — set by the screen before calling getGoogleIdToken
    var activityContext: Activity? = null

    actual suspend fun getGoogleIdToken(): String? {
        val result = getGoogleIdTokenResult()
        return when (result) {
            is GoogleSignInResult.Success -> result.idToken
            is GoogleSignInResult.Error -> throw Exception(result.message)
            is GoogleSignInResult.Cancelled -> null
        }
    }

    suspend fun getGoogleIdTokenResult(): GoogleSignInResult {
        val ctx = activityContext ?: context
        return try {
            val credentialManager = CredentialManager.create(ctx)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(Constants.GOOGLE_WEB_CLIENT_ID)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(ctx, request)
            val credential = result.credential

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleSignInResult.Success(googleIdTokenCredential.idToken)
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (_: NoCredentialException) {
            GoogleSignInResult.Error(
                "No Google account found on this device.\n\n" +
                "Please add a Google account in your device Settings → Accounts, then try again."
            )
        } catch (e: Exception) {
            val msg = e.message ?: e.toString()
            when {
                msg.contains("REPLACE_WITH") ->
                    GoogleSignInResult.Error(
                        "Google Sign-In is not configured yet.\n\n" +
                        "Enable Google provider in Firebase Console → Authentication → Sign-in method, " +
                        "then update the Web Client ID in Constants.kt."
                    )
                msg.contains("10:") || msg.contains("DEVELOPER_ERROR") ->
                    GoogleSignInResult.Error(
                        "SHA-1 fingerprint mismatch.\n\n" +
                        "Run: ./gradlew signingReport\n" +
                        "Then add the SHA-1 to Firebase Console → Project Settings → Your Android app."
                    )
                else ->
                    GoogleSignInResult.Error("Sign-in failed: $msg")
            }
        }
    }
}

actual val isGoogleSignInAvailable: Boolean = true

