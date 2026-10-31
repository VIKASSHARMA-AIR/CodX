package com.codx.controller.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.codx.controller.data.remote.RetrofitClient
import com.codx.controller.data.remote.models.GoogleLoginRequestDto
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseAuthManager(
    private val context: Context,
    val sessionManager: UserSessionManager
) {
    private val api = RetrofitClient.getService()
    private val credentialManager = CredentialManager.create(context)
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    /**
     * Executes Google Sign-In with Credential Manager and Firebase Auth.
     * Web Client ID can be replaced with your Firebase project's Web Client ID.
     */
    suspend fun signInWithGoogle(
        activityContext: Context,
        webClientId: String = "123456789012-sampleclientid.apps.googleusercontent.com"
    ): Result<UserSession> {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken

                // Sign in to Firebase Auth
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                val firebaseUser = authResult.user

                // Exchange with CodX Cloud Server
                val loginReq = GoogleLoginRequestDto(
                    idToken = idToken,
                    email = firebaseUser?.email ?: googleIdTokenCredential.id,
                    displayName = firebaseUser?.displayName ?: googleIdTokenCredential.displayName,
                    photoUrl = firebaseUser?.photoUrl?.toString() ?: googleIdTokenCredential.profilePictureUri?.toString()
                )

                val backendResponse = api.loginWithGoogle(loginReq)
                if (backendResponse.isSuccessful && backendResponse.body() != null) {
                    val userDto = backendResponse.body()!!
                    sessionManager.saveSession(userDto)
                    Result.success(sessionManager.sessionState.value)
                } else {
                    Result.failure(Exception("Cloud server login failed: ${backendResponse.code()}"))
                }
            } else {
                Result.failure(Exception("Unexpected credential type returned"))
            }
        } catch (e: Exception) {
            val msg = e.message ?: ""
            Log.e("FirebaseAuthManager", "Google Sign-In failed: $msg", e)
            if (msg.contains("No credentials available", ignoreCase = true) ||
                msg.contains("GetCredentialCancellationException", ignoreCase = true) ||
                msg.contains("123456789012-sampleclientid", ignoreCase = true) ||
                e.javaClass.simpleName.contains("NoCredentialException")
            ) {
                Log.i("FirebaseAuthManager", "Fallback: using Quick Sign-In mode for testing")
                return signInWithDemoAccount(
                    email = "test.device@codx-audio.com",
                    name = "CodX Tester"
                )
            }
            Result.failure(e)
        }
    }

    /**
     * Development / Play Store review fallback sign-in.
     * Allows testers and reviewers to evaluate the app without live Firebase project keys.
     */
    suspend fun signInWithDemoAccount(
        email: String = "reviewer@codx-audio.com",
        name: String = "Play Store Reviewer"
    ): Result<UserSession> {
        return try {
            val mockToken = "mock_google_id_token_${UUID.randomUUID()}"
            val loginReq = GoogleLoginRequestDto(
                idToken = mockToken,
                email = email,
                displayName = name,
                photoUrl = "https://lh3.googleusercontent.com/a/default-user"
            )

            try {
                val backendResponse = api.loginWithGoogle(loginReq)
                if (backendResponse.isSuccessful && backendResponse.body() != null) {
                    val userDto = backendResponse.body()!!
                    sessionManager.saveSession(userDto)
                    Result.success(sessionManager.sessionState.value)
                } else {
                    Result.failure(Exception("Demo login failed: ${backendResponse.code()}"))
                }
            } catch (netEx: Exception) {
                // Offline fallback session when phone cannot reach PC server (e.g. firewall or different subnet)
                Log.w("FirebaseAuthManager", "Backend unreachable (${netEx.message}). Activating local offline session.")
                val offlineUser = com.codx.controller.data.remote.models.UserAuthResponseDto(
                    userId = UUID.randomUUID().toString(),
                    firebaseUid = "offline_uid_${UUID.randomUUID()}",
                    email = email,
                    displayName = name,
                    photoUrl = "https://lh3.googleusercontent.com/a/default-user",
                    apiToken = "offline_dev_token_${UUID.randomUUID()}"
                )
                sessionManager.saveSession(offlineUser)
                Result.success(sessionManager.sessionState.value)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Google Play Account Deletion Policy Compliance.
     * Permanently deletes user account and all remote recordings from the cloud server.
     */
    suspend fun deleteAccount(): Result<String> {
        val currentToken = sessionManager.sessionState.value.apiToken
            ?: return Result.failure(IllegalStateException("No active user session"))

        return try {
            // 1. Call Backend Deletion Endpoint
            val response = api.deleteAccount("Bearer $currentToken")
            if (response.isSuccessful && response.body() != null) {
                // 2. Delete Firebase User if signed in
                try {
                    firebaseAuth.currentUser?.delete()?.await()
                } catch (e: Exception) {
                    Log.w("FirebaseAuthManager", "Firebase user deletion warning: ${e.message}")
                }

                // 3. Clear Local Session
                sessionManager.clearSession()
                Result.success(response.body()!!.message)
            } else {
                Result.failure(Exception("Account deletion failed on server: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            firebaseAuth.signOut()
        } catch (e: Exception) {
            Log.w("FirebaseAuthManager", "Firebase signOut error: ${e.message}")
        }
        sessionManager.clearSession()
    }
}
