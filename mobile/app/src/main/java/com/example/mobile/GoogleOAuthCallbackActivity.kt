package com.example.mobile

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class GoogleOAuthCallbackActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent?.data
        val errorMessage = extractOAuthErrorMessage(data)
        if (!errorMessage.isNullOrBlank()) {
            openLoginActivity(this, "Google sign-in was canceled or denied.")
            finish()
            return
        }

        val session = data?.let(::parseAuthSession)
        if (session == null) {
            openLoginActivity(this, "Google sign-in response was incomplete.")
            finish()
            return
        }

        if (!completeAuthenticatedSession(this, session)) {
            openLoginActivity(this, "Staff and admin accounts must use the web app on a desktop or laptop.")
            finish()
            return
        }

        finish()
    }
}
