package com.codx.controller.auth

import android.content.Context
import android.content.SharedPreferences
import com.codx.controller.data.remote.models.UserAuthResponseDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSession(
    val isLoggedIn: Boolean = false,
    val userId: String? = null,
    val firebaseUid: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val apiToken: String? = null
)

class UserSessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("codx_user_session", Context.MODE_PRIVATE)

    private val _sessionState = MutableStateFlow(loadSession())
    val sessionState: StateFlow<UserSession> = _sessionState.asStateFlow()

    private fun loadSession(): UserSession {
        val isLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val userId = prefs.getString(KEY_USER_ID, null)
        val firebaseUid = prefs.getString(KEY_FIREBASE_UID, null)
        val email = prefs.getString(KEY_EMAIL, null)
        val displayName = prefs.getString(KEY_DISPLAY_NAME, null)
        val photoUrl = prefs.getString(KEY_PHOTO_URL, null)
        val apiToken = prefs.getString(KEY_API_TOKEN, null)

        return UserSession(
            isLoggedIn = isLoggedIn && apiToken != null,
            userId = userId,
            firebaseUid = firebaseUid,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            apiToken = apiToken
        )
    }

    fun saveSession(user: UserAuthResponseDto) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_USER_ID, user.userId)
            .putString(KEY_FIREBASE_UID, user.firebaseUid)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_DISPLAY_NAME, user.displayName)
            .putString(KEY_PHOTO_URL, user.photoUrl)
            .putString(KEY_API_TOKEN, user.apiToken)
            .apply()

        _sessionState.value = UserSession(
            isLoggedIn = true,
            userId = user.userId,
            firebaseUid = user.firebaseUid,
            email = user.email,
            displayName = user.displayName,
            photoUrl = user.photoUrl,
            apiToken = user.apiToken
        )
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        _sessionState.value = UserSession()
    }

    companion object {
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_USER_ID = "key_user_id"
        private const val KEY_FIREBASE_UID = "key_firebase_uid"
        private const val KEY_EMAIL = "key_email"
        private const val KEY_DISPLAY_NAME = "key_display_name"
        private const val KEY_PHOTO_URL = "key_photo_url"
        private const val KEY_API_TOKEN = "key_api_token"
    }
}
