package com.example.mobile.api

import com.example.mobile.AppointmentEntry
import com.example.mobile.BuildConfig
import com.example.mobile.parseAppointments
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject

object ClinicApi {
    private val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor()
        logging.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        OkHttpClient.Builder().addInterceptor(logging).build()
    }

    private fun request(path: String, token: String?, method: String = "GET", bodyJson: String? = null): String {
        val body = bodyJson?.toRequestBody("application/json; charset=utf-8".toMediaType())
        val requestBuilder = Request.Builder()
            .url(BuildConfig.BACKEND_URL + path)
            .method(method, if (method == "GET" || method == "DELETE") null else body)

        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            val message = runCatching {
                val json = JSONObject(responseBody)
                json.optString("message", responseBody)
            }.getOrDefault(responseBody.ifBlank { "Request failed" })
            throw IllegalStateException(message.ifBlank { "Request failed" })
        }

        return responseBody
    }

    fun fetchProfile(token: String): UserProfile {
        val body = request("/api/auth/me", token)
        val json = JSONObject(body)
        return UserProfile(
            userId = json.optLong("userId", json.optLong("id", -1L)),
            email = json.optString("email", ""),
            firstName = json.optString("firstName", ""),
            lastName = json.optString("lastName", ""),
            provider = json.optString("provider", "LOCAL"),
            role = json.optInt("role", 1)
        )
    }

    fun fetchAppointments(token: String): List<AppointmentEntry> {
        val body = request("/api/appointments", token)
        return parseAppointments(body)
    }

    fun fetchRecords(token: String): List<ClinicRecord> {
        val body = request("/api/records", token)
        val items = JSONArray(body)
        val records = mutableListOf<ClinicRecord>()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            records.add(
                ClinicRecord(
                    id = item.optLong("id", -1L),
                    title = item.optString("title", ""),
                    summary = item.optString("summary", ""),
                    date = item.optString("date", ""),
                    doctorName = item.optString("doctorName").takeIf { it.isNotBlank() },
                    remarks = item.optString("remarks").takeIf { it.isNotBlank() },
                    results = item.optString("results").takeIf { it.isNotBlank() }
                )
            )
        }
        return records
    }

    fun fetchDoctors(token: String): List<ClinicDoctor> {
        val body = request("/api/doctors", token)
        val items = JSONArray(body)
        val doctors = mutableListOf<ClinicDoctor>()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            doctors.add(
                ClinicDoctor(
                    id = item.optLong("id", -1L),
                    name = item.optString("name", ""),
                    available = item.optBoolean("available", false),
                    doctorIn = item.optBoolean("doctorIn", item.optBoolean("available", false))
                )
            )
        }
        return doctors
    }

    fun fetchNotifications(token: String): List<ClinicNotification> {
        val body = request("/api/notifications", token)
        val items = JSONArray(body)
        val notifications = mutableListOf<ClinicNotification>()
        for (index in 0 until items.length()) {
            val item = items.optJSONObject(index) ?: continue
            notifications.add(
                ClinicNotification(
                    id = item.optLong("id", -1L),
                    isRead = item.optBoolean("isRead", false)
                )
            )
        }
        return notifications
    }
}

data class UserProfile(
    val userId: Long,
    val email: String,
    val firstName: String,
    val lastName: String,
    val provider: String,
    val role: Int,
)

data class ClinicRecord(
    val id: Long,
    val title: String,
    val summary: String,
    val date: String,
    val doctorName: String?,
    val remarks: String?,
    val results: String?,
)

data class ClinicDoctor(
    val id: Long,
    val name: String,
    val available: Boolean,
    val doctorIn: Boolean,
)

data class ClinicNotification(
    val id: Long,
    val isRead: Boolean,
)
