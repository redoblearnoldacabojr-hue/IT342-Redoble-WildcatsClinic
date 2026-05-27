package com.example.mobile

import android.content.Context
import android.content.Intent
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject

data class AuthSession(
    val token: String,
    val userId: Long,
    val email: String,
    val firstName: String,
    val lastName: String,
    val provider: String,
    val role: Int,
)

fun parseAuthSession(body: String): AuthSession? {
    return try {
        val json = JSONObject(body)
        if (!json.has("token") || json.isNull("token")) return null
        val token = json.optString("token")
        AuthSession(
            token = token,
            userId = json.optLong("userId", -1L),
            email = json.optString("email", ""),
            firstName = json.optString("firstName", ""),
            lastName = json.optString("lastName", ""),
            provider = json.optString("provider", "LOCAL"),
            role = json.optInt("role", 1),
        )
    } catch (_: Exception) {
        null
    }
}

fun extractServerErrorMessage(body: String?, fallback: String): String {
    if (body.isNullOrBlank()) return fallback

    return runCatching {
        val json = JSONObject(body)
        when {
            json.has("message") && !json.isNull("message") -> json.optString("message", fallback)
            json.has("error") && !json.isNull("error") -> json.optString("error", fallback)
            json.has("title") && !json.isNull("title") -> json.optString("title", fallback)
            else -> fallback
        }
    }.getOrDefault(fallback)
}

fun persistAuthSession(context: Context, session: AuthSession) {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val securePrefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    securePrefs.edit().putString("jwt_token", session.token).apply()
    securePrefs.edit().putLong("user_id", session.userId).apply()
}

fun clearAuthSession(context: Context) {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val securePrefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    securePrefs.edit().clear().apply()
}

fun getStoredToken(context: Context): String? {
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val securePrefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    return securePrefs.getString("jwt_token", null)
}

fun openMainActivity(context: Context) {
    val intent = Intent(context, MainActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
}

fun openLoginActivity(context: Context) {
    val intent = Intent(context, GoogleSignInActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
}