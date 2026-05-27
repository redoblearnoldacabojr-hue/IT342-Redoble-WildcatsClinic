package com.example.mobile

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.graphics.Paint
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {
    private lateinit var client: OkHttpClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val logging = HttpLoggingInterceptor()
        logging.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        client = OkHttpClient.Builder().addInterceptor(logging).build()

        findViewById<MaterialButton>(R.id.btn_create_account).setOnClickListener {
            performRegister()
        }

        findViewById<MaterialButton>(R.id.btn_google_signup).setOnClickListener {
            startActivity(
                Intent(this, GoogleSignInActivity::class.java)
                    .putExtra(GoogleSignInActivity.EXTRA_AUTOSTART_GOOGLE, true)
            )
        }

        findViewById<TextView>(R.id.btn_go_login).setOnClickListener {
            openLoginActivity(this)
        }

        findViewById<TextView>(R.id.btn_go_login).apply {
            paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
        }
    }

    private fun performRegister() {
        val firstNameLayout = findViewById<TextInputLayout>(R.id.layout_first_name)
        val lastNameLayout = findViewById<TextInputLayout>(R.id.layout_last_name)
        val emailLayout = findViewById<TextInputLayout>(R.id.layout_email)
        val passwordLayout = findViewById<TextInputLayout>(R.id.layout_password)
        val confirmPasswordLayout = findViewById<TextInputLayout>(R.id.layout_confirm_password)
        val firstName = findViewById<TextInputEditText>(R.id.input_first_name).text?.toString()?.trim().orEmpty()
        val lastName = findViewById<TextInputEditText>(R.id.input_last_name).text?.toString()?.trim().orEmpty()
        val email = findViewById<TextInputEditText>(R.id.input_email).text?.toString()?.trim().orEmpty()
        val password = findViewById<TextInputEditText>(R.id.input_password).text?.toString().orEmpty()
        val confirmPassword = findViewById<TextInputEditText>(R.id.input_confirm_password).text?.toString().orEmpty()
        val statusText = findViewById<TextView>(R.id.register_status_text)

        firstNameLayout.error = null
        lastNameLayout.error = null
        emailLayout.error = null
        passwordLayout.error = null
        confirmPasswordLayout.error = null
        statusText.visibility = View.GONE

        var valid = true
        if (firstName.isBlank()) {
            firstNameLayout.error = "First name is required"
            valid = false
        }
        if (lastName.isBlank()) {
            lastNameLayout.error = "Last name is required"
            valid = false
        }
        if (email.isBlank()) {
            emailLayout.error = "Email is required"
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = "Enter a valid email"
            valid = false
        }
        if (password.length < 8) {
            passwordLayout.error = "Use at least 8 characters"
            valid = false
        }
        if (confirmPassword != password) {
            confirmPasswordLayout.error = "Passwords do not match"
            valid = false
        }

        if (!valid) {
            statusText.visibility = View.VISIBLE
            statusText.text = "Please fix the highlighted fields."
            return
        }

        val backendBaseUrl = BuildConfig.BACKEND_URL
        val registerUrl = "$backendBaseUrl/api/auth/register"
        val json = JSONObject()
            .put("firstName", firstName)
            .put("lastName", lastName)
            .put("email", email)
            .put("password", password)
            .toString()
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder().url(registerUrl).post(body).build()

        Thread {
            try {
                val resp = client.newCall(req).execute()
                if (!resp.isSuccessful) {
                    val errorMessage = resp.body?.string().orEmpty().ifBlank { "Registration failed. Please try again." }
                    runOnUiThread {
                        statusText.visibility = View.VISIBLE
                        statusText.text = extractServerErrorMessage(errorMessage, "Registration failed. Please try again.")
                    }
                    return@Thread
                }

                runOnUiThread {
                    statusText.visibility = View.VISIBLE
                    statusText.text = "Registration successful. Redirecting to login..."
                }

                window.decorView.postDelayed({
                    openLoginActivity(this@RegisterActivity)
                }, 1200)
            } catch (ex: Exception) {
                runOnUiThread {
                    statusText.visibility = View.VISIBLE
                    statusText.text = "Registration failed. Please try again."
                }
            }
        }.start()
    }
}