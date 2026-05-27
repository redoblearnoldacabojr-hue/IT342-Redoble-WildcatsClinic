package com.example.mobile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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

fun parseAuthSession(uri: Uri): AuthSession? {
    val token = uri.getQueryParameter("token")?.takeIf { it.isNotBlank() } ?: return null
    val userId = uri.getQueryParameter("userId")?.toLongOrNull() ?: -1L
    val email = uri.getQueryParameter("email").orEmpty()
    val firstName = uri.getQueryParameter("firstName").orEmpty()
    val lastName = uri.getQueryParameter("lastName").orEmpty()
    val provider = uri.getQueryParameter("provider") ?: "GOOGLE"
    val role = uri.getQueryParameter("role")?.toIntOrNull() ?: 1

    return AuthSession(
        token = token,
        userId = userId,
        email = email,
        firstName = firstName,
        lastName = lastName,
        provider = provider,
        role = role,
    )
}

fun extractOAuthErrorMessage(uri: Uri?): String? {
    if (uri == null) return null

    val error = uri.getQueryParameter("error")?.takeIf { it.isNotBlank() }
    val errorDescription = uri.getQueryParameter("error_description")?.takeIf { it.isNotBlank() }
    val description = errorDescription ?: error
    return description?.replace('+', ' ')
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

    securePrefs.edit()
        .putString("jwt_token", session.token)
        .putLong("user_id", session.userId)
        .putInt("user_role", session.role)
        .putString("user_email", session.email)
        .putString("user_first_name", session.firstName)
        .putString("user_last_name", session.lastName)
        .putString("user_provider", session.provider)
        .commit()
}

fun completeAuthenticatedSession(context: Context, session: AuthSession): Boolean {
    if (!canUseMobileApp(session.role)) {
        clearAuthSession(context)
        return false
    }

    persistAuthSession(context, session)
    runCatching { registerDeviceToken(context, session.token) }
        .onFailure { ex -> Log.w("AuthUiUtils", "Skipping device registration after sign-in", ex) }
    return true
}

private fun registerDeviceToken(context: Context, jwtToken: String) {
    val backendBaseUrl = BuildConfig.BACKEND_URL

    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (!task.isSuccessful) {
            Log.w("AuthUiUtils", "Unable to fetch FCM token", task.exception)
            return@addOnCompleteListener
        }

        val fcmToken = task.result
        if (fcmToken.isNullOrBlank()) {
            return@addOnCompleteListener
        }

        val client = OkHttpClient()
        val deviceJson = JSONObject()
            .put("token", fcmToken)
            .put("platform", "android")
            .toString()
        val deviceBody = deviceJson.toRequestBody("application/json; charset=utf-8".toMediaType())
        val deviceRequest = Request.Builder()
            .url("$backendBaseUrl/api/devices")
            .post(deviceBody)
            .addHeader("Authorization", "Bearer $jwtToken")
            .build()

        Thread {
            try {
                client.newCall(deviceRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.i("AuthUiUtils", "Registered device token with backend")
                    } else {
                        Log.e("AuthUiUtils", "Failed to register device token: ${'$'}{response.code}")
                    }
                }
            } catch (ex: Exception) {
                Log.e("AuthUiUtils", "Error registering device token", ex)
            }
        }.start()
    }
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

fun getStoredRole(context: Context): Int? {
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

    return if (securePrefs.contains("user_role")) securePrefs.getInt("user_role", 1) else null
}

fun getStoredEmail(context: Context): String? {
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

    return securePrefs.getString("user_email", null)
}

fun getStoredDisplayName(context: Context): String? {
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

    val firstName = securePrefs.getString("user_first_name", "") ?: ""
    val lastName = securePrefs.getString("user_last_name", "") ?: ""
    val fullName = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").trim()
    return fullName.ifBlank { null }
}

fun canUseMobileApp(role: Int?): Boolean {
    return role == null || role <= 1
}

fun openMainActivity(context: Context) {
    val intent = Intent(context, MainActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    context.startActivity(intent)
}

fun openLoginActivity(context: Context, errorMessage: String? = null) {
    val intent = Intent(context, GoogleSignInActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    if (!errorMessage.isNullOrBlank()) {
        intent.putExtra(GoogleSignInActivity.EXTRA_AUTH_ERROR, errorMessage)
    }
    context.startActivity(intent)
}