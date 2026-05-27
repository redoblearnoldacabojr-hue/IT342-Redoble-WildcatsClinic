package com.example.mobile

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.graphics.Paint
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.MasterKey
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import com.google.firebase.messaging.FirebaseMessaging
import com.google.android.material.textfield.TextInputLayout

class GoogleSignInActivity : AppCompatActivity() {
    private val TAG = "GoogleSignInActivity"
    private lateinit var client: OkHttpClient
    private lateinit var signInClient: GoogleSignInClient

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
        // If a JWT is already stored, skip signin and open main
        val existing = getStoredToken(this)
        if (!existing.isNullOrBlank()) {
            openMainActivity(this)
            finish()
            return
        }

        setContentView(R.layout.activity_google_signin)

        // Edge-to-edge content so UI fills the whole screen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Apply system bar insets as padding to root content
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val logging = HttpLoggingInterceptor()
        logging.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        client = OkHttpClient.Builder().addInterceptor(logging).build()

        val serverClientId = getString(R.string.server_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(serverClientId)
            .build()

        signInClient = GoogleSignIn.getClient(this, gso)

        findViewById<MaterialButton>(R.id.btn_login).setOnClickListener {
            performEmailLogin()
        }

        findViewById<MaterialButton>(R.id.btn_google_signin).setOnClickListener {
            startGoogleFlow()
        }

        findViewById<TextView>(R.id.btn_go_register).setOnClickListener {
            openRegister()
        }

        findViewById<TextView>(R.id.btn_go_register).apply {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
        }

        findViewById<TextView>(R.id.btn_go_register).setOnLongClickListener { true }

        if (intent.getBooleanExtra(EXTRA_AUTOSTART_GOOGLE, false)) {
            startGoogleFlow()
        }
    }

    private fun startGoogleFlow() {
        val intent = signInClient.signInIntent
        startForResult.launch(intent)
    }

    private fun performEmailLogin() {
        val emailLayout = findViewById<TextInputLayout>(R.id.layout_login_email)
        val passwordLayout = findViewById<TextInputLayout>(R.id.layout_login_password)
        val email = findViewById<TextInputEditText>(R.id.input_email).text?.toString()?.trim().orEmpty()
        val password = findViewById<TextInputEditText>(R.id.input_password).text?.toString().orEmpty()
        val statusText = findViewById<TextView>(R.id.login_status_text)

        emailLayout.error = null
        passwordLayout.error = null
        statusText.visibility = View.GONE

        if (email.isBlank() || password.isBlank()) {
            if (email.isBlank()) {
                emailLayout.error = "Email is required"
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Enter a valid email"
            }
            if (password.isBlank()) {
                passwordLayout.error = "Password is required"
            }
            statusText.visibility = View.VISIBLE
            statusText.text = "Please fix the highlighted fields."
            Log.w(TAG, "Email and password validation failed")
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Enter a valid email"
            statusText.visibility = View.VISIBLE
            statusText.text = "Please fix the highlighted fields."
            return
        }

        val backendBaseUrl = BuildConfig.BACKEND_URL
        val loginUrl = "$backendBaseUrl/api/auth/login"
        val json = JSONObject()
            .put("email", email)
            .put("password", password)
            .toString()
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder().url(loginUrl).post(body).build()

        Thread {
            try {
                val resp = client.newCall(req).execute()
                val respBody = resp.body?.string()
                if (!resp.isSuccessful || respBody.isNullOrBlank()) {
                    val errorMessage = extractServerErrorMessage(respBody, "Invalid credentials or server error.")
                    runOnUiThread {
                        statusText.visibility = View.VISIBLE
                        statusText.text = errorMessage
                    }
                    Log.e(TAG, "Login failed: ${resp.code}")
                    return@Thread
                }

                val session = parseAuthSession(respBody)
                if (session == null) {
                    runOnUiThread {
                        statusText.visibility = View.VISIBLE
                        statusText.text = "Unable to start your session."
                    }
                    Log.e(TAG, "Login response missing token")
                    return@Thread
                }

                persistAuthSession(this@GoogleSignInActivity, session)
                openMainActivity(this@GoogleSignInActivity)
            } catch (ex: Exception) {
                Log.e(TAG, "Login error", ex)
            }
        }.start()
    }

    private fun openRegister() {
        startActivity(android.content.Intent(this, RegisterActivity::class.java))
    }

    private fun exchangeTokenWithBackend(idToken: String) {
        val backendBaseUrl = BuildConfig.BACKEND_URL
        val backendUrl = "$backendBaseUrl/api/auth/google"
        val statusText = findViewById<TextView>(R.id.login_status_text)
        val json = JSONObject()
            .put("idToken", idToken)
            .toString()
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder().url(backendUrl).post(body).build()

        Thread {
            try {
                val resp = client.newCall(req).execute()
                if (!resp.isSuccessful) {
                    val errorBody = resp.body?.string()
                    val errorMessage = extractServerErrorMessage(errorBody, "Google sign-in failed. Please try again.")
                    runOnUiThread {
                        statusText.visibility = View.VISIBLE
                        statusText.text = errorMessage
                    }
                    Log.e(TAG, "Backend exchange failed: ${resp.code}")
                    return@Thread
                }
                val respBody = resp.body?.string()
                if (respBody != null) {
                    try {
                        val session = parseAuthSession(respBody)
                        if (session != null) {
                            persistAuthSession(this@GoogleSignInActivity, session)
                            Log.i(TAG, "Stored auth token securely")
                            openMainActivity(this@GoogleSignInActivity)
                        } else {
                            runOnUiThread {
                                statusText.visibility = View.VISIBLE
                                statusText.text = "Google sign-in response was incomplete."
                            }
                            Log.e(TAG, "Empty response body from backend exchange")
                            return@Thread
                        }

                        // After storing JWT, attempt to obtain FCM token and register it with backend
                        try {
                            val masterKey = MasterKey.Builder(this@GoogleSignInActivity)
                                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                                .build()

                            val securePrefs = androidx.security.crypto.EncryptedSharedPreferences.create(
                                this@GoogleSignInActivity,
                                "secure_prefs",
                                masterKey,
                                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                            )

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
                                                .url("$backendBaseUrl/api/devices")
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
                    runOnUiThread {
                        statusText.visibility = View.VISIBLE
                        statusText.text = "Google sign-in response was empty."
                    }
                    Log.e(TAG, "Empty response body from backend exchange")
                }
            } catch (ex: Exception) {
                runOnUiThread {
                    statusText.visibility = View.VISIBLE
                    statusText.text = "Google sign-in failed. Check your connection and try again."
                }
                Log.e(TAG, "Exchange error", ex)
            }
        }.start()
    }

    companion object {
        const val EXTRA_AUTOSTART_GOOGLE = "extra_autostart_google"
    }
}
