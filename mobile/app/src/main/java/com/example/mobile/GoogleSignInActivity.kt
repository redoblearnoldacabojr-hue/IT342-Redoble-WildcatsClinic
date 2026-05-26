package com.example.mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import com.google.firebase.messaging.FirebaseMessaging

class GoogleSignInActivity : AppCompatActivity() {
    private val TAG = "GoogleSignInActivity"
    private lateinit var client: OkHttpClient

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                exchangeTokenWithBackend(idToken)
            }
        } catch (e: ApiException) {
            Log.w(TAG, "Google sign in failed", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_signin)

        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        client = OkHttpClient.Builder().addInterceptor(logging).build()

        val serverClientId = getString(R.string.server_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(serverClientId)
            .build()

        val signInClient = GoogleSignIn.getClient(this, gso)

        findViewById<Button>(R.id.btn_google_signin).setOnClickListener {
            val intent = signInClient.signInIntent
            startForResult.launch(intent)
        }
    }

    private fun exchangeTokenWithBackend(idToken: String) {
        // Replace with your backend URL
        val backendUrl = "http://10.0.2.2:8080/api/auth/google"
        val json = "{\"idToken\":\"$idToken\"}"
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder().url(backendUrl).post(body).build()

        Thread {
            try {
                val resp = client.newCall(req).execute()
                if (!resp.isSuccessful) {
                    Log.e(TAG, "Backend exchange failed: ${resp.code}")
                    return@Thread
                }
                val respBody = resp.body?.string()
                if (respBody != null) {
                    try {
                        val json = JSONObject(respBody)
                        val token = json.optString("token", null)
                        val userId = json.optLong("userId", -1L)

                        // store securely using EncryptedSharedPreferences
                        val masterKey = MasterKey.Builder(this@GoogleSignInActivity)
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                            .build()

                        val securePrefs = EncryptedSharedPreferences.create(
                            this@GoogleSignInActivity,
                            "secure_prefs",
                            masterKey,
                            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                        )

                        token?.let {
                            securePrefs.edit().putString("jwt_token", it).apply()
                        }
                        if (userId >= 0) {
                            securePrefs.edit().putLong("user_id", userId).apply()
                        }

                        Log.i(TAG, "Stored auth token securely")

                        // After storing JWT, attempt to obtain FCM token and register it with backend
                        try {
                            val jwtStored = securePrefs.getString("jwt_token", null)
                            if (jwtStored != null) {
                                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val fcmToken = task.result
                                        if (!fcmToken.isNullOrBlank()) {
                                            val deviceJson = JSONObject()
                                            deviceJson.put("token", fcmToken)
                                            deviceJson.put("platform", "android")
                                            val deviceBody = deviceJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                                            val deviceReq = Request.Builder()
                                                .url("http://10.0.2.2:8080/api/devices")
                                                .post(deviceBody)
                                                .addHeader("Authorization", "Bearer $jwtStored")
                                                .build()

                                            Thread {
                                                try {
                                                    val deviceResp = client.newCall(deviceReq).execute()
                                                    if (deviceResp.isSuccessful) {
                                                        Log.i(TAG, "Registered device token with backend")
                                                    } else {
                                                        Log.e(TAG, "Failed to register device token: ${'$'}{deviceResp.code}")
                                                    }
                                                } catch (ex: Exception) {
                                                    Log.e(TAG, "Error registering device token", ex)
                                                }
                                            }.start()
                                        }
                                    } else {
                                        Log.w(TAG, "Unable to fetch FCM token", task.exception)
                                    }
                                }
                            }
                        } catch (ex: Exception) {
                            Log.e(TAG, "Failed obtaining or registering FCM token", ex)
                        }
                    } catch (ex: Exception) {
                        Log.e(TAG, "Failed parsing auth response", ex)
                    }
                } else {
                    Log.e(TAG, "Empty response body from backend exchange")
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Exchange error", ex)
            }
        }.start()
    }
}
