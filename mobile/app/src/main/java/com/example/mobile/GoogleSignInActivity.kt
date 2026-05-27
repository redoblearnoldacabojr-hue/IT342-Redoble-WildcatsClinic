package com.example.mobile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.graphics.Paint
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import com.google.android.material.textfield.TextInputLayout

class GoogleSignInActivity : AppCompatActivity() {
    private val TAG = "GoogleSignInActivity"
    private lateinit var client: OkHttpClient
    private lateinit var loadingOverlay: View
    private lateinit var loadingText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_signin)

        loadingOverlay = findViewById(R.id.loading_overlay)
        loadingText = findViewById(R.id.loading_text)

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

        val existingToken = getStoredToken(this)
        if (!existingToken.isNullOrBlank()) {
            validateStoredSession(existingToken)
            return
        }

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

        val statusText = findViewById<TextView>(R.id.login_status_text)

        intent.getStringExtra(EXTRA_AUTH_ERROR)?.takeIf { it.isNotBlank() }?.let { errorMessage ->
            statusText.visibility = View.VISIBLE
            statusText.text = errorMessage
        }

        if (intent.getBooleanExtra(EXTRA_AUTOSTART_GOOGLE, false)) {
            startGoogleFlow()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun startGoogleFlow() {
        setLoading(true, "Opening Google sign-in in your browser...")

        val authUri = Uri.parse("${BuildConfig.BACKEND_URL}/api/auth/google/start")
            .buildUpon()
            .appendQueryParameter("redirect_uri", GOOGLE_OAUTH_REDIRECT_URI)
            .build()

        val intent = Intent(Intent.ACTION_VIEW, authUri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            setLoading(false)
            findViewById<TextView>(R.id.login_status_text).apply {
                visibility = View.VISIBLE
                text = "No browser app was found to continue Google sign-in."
            }
        }
    }

    private fun setLoading(show: Boolean, message: String = "Processing...") {
        loadingText.text = message
        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        findViewById<MaterialButton>(R.id.btn_login).isEnabled = !show
        findViewById<MaterialButton>(R.id.btn_google_signin).isEnabled = !show
        findViewById<TextView>(R.id.btn_go_register).isEnabled = !show
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

        setLoading(true, "Signing in...")

        Thread {
            try {
                val resp = client.newCall(req).execute()
                val respBody = resp.body?.string()
                if (!resp.isSuccessful || respBody.isNullOrBlank()) {
                    val errorMessage = extractServerErrorMessage(respBody, "Invalid credentials or server error.")
                    runOnUiThread {
                        setLoading(false)
                        statusText.visibility = View.VISIBLE
                        statusText.text = errorMessage
                    }
                    Log.e(TAG, "Login failed: ${resp.code}")
                    return@Thread
                }

                val session = parseAuthSession(respBody)
                if (session == null) {
                    runOnUiThread {
                        setLoading(false)
                        statusText.visibility = View.VISIBLE
                        statusText.text = "Unable to start your session."
                    }
                    Log.e(TAG, "Login response missing token")
                    return@Thread
                }

                if (!completeAuthenticatedSession(this@GoogleSignInActivity, session)) {
                    runOnUiThread {
                        setLoading(false)
                        clearAuthSession(this@GoogleSignInActivity)
                        statusText.visibility = View.VISIBLE
                        statusText.text = "Staff and admin accounts must use the web app on a desktop or laptop."
                    }
                    Log.w(TAG, "Blocked mobile access for role ${session.role}")
                    return@Thread
                }

                runOnUiThread {
                    setLoading(false)
                    openMainActivity(this@GoogleSignInActivity)
                    finish()
                }
            } catch (ex: Exception) {
                runOnUiThread {
                    setLoading(false)
                    statusText.visibility = View.VISIBLE
                    statusText.text = "Login failed: ${ex.message ?: "unknown error"}"
                }
                Log.e(TAG, "Login error", ex)
            }
        }.start()
    }

    private fun openRegister() {
        startActivity(android.content.Intent(this, RegisterActivity::class.java))
    }

    private fun validateStoredSession(token: String) {
        val statusText = findViewById<TextView>(R.id.login_status_text)
        val cachedRole = getStoredRole(this)

        if (cachedRole != null) {
            if (canUseMobileApp(cachedRole)) {
                openMainActivity(this)
            } else {
                clearAuthSession(this)
                statusText.visibility = View.VISIBLE
                statusText.text = "Staff and admin accounts must use the web app on a desktop or laptop."
            }
            return
        }

        Thread {
            try {
                val request = Request.Builder()
                    .url(BuildConfig.BACKEND_URL + "/api/auth/me")
                    .get()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                val response = client.newCall(request).execute()
                val body = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    runOnUiThread {
                        setLoading(false)
                        clearAuthSession(this)
                        statusText.visibility = View.VISIBLE
                        statusText.text = extractServerErrorMessage(body, "Session expired. Please sign in again.")
                    }
                    return@Thread
                }

                val session = runCatching {
                    val json = JSONObject(body)
                    AuthSession(
                        token = token,
                        userId = json.optLong("userId", json.optLong("id", -1L)),
                        email = json.optString("email", ""),
                        firstName = json.optString("firstName", ""),
                        lastName = json.optString("lastName", ""),
                        provider = json.optString("provider", "LOCAL"),
                        role = json.optInt("role", 1)
                    )
                }.getOrNull()

                runOnUiThread {
                    if (session == null) {
                        setLoading(false)
                        clearAuthSession(this)
                        statusText.visibility = View.VISIBLE
                        statusText.text = "Unable to verify your session. Please sign in again."
                        return@runOnUiThread
                    }

                    if (canUseMobileApp(session.role)) {
                        persistAuthSession(this, session)
                        openMainActivity(this)
                    } else {
                        setLoading(false)
                        clearAuthSession(this)
                        statusText.visibility = View.VISIBLE
                        statusText.text = "Staff and admin accounts must use the web app on a desktop or laptop."
                    }
                }
            } catch (_: Exception) {
                runOnUiThread {
                    setLoading(false)
                    clearAuthSession(this)
                    statusText.visibility = View.VISIBLE
                    statusText.text = "Unable to verify your session. Please sign in again."
                }
            }
        }.start()
    }

    companion object {
        const val EXTRA_AUTOSTART_GOOGLE = "extra_autostart_google"
        const val EXTRA_AUTH_ERROR = "extra_auth_error"
        const val GOOGLE_OAUTH_REDIRECT_URI = "wildcatclinic://auth/google"
    }
}
