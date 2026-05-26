package com.example.mobile

import java.util.Calendar
import org.json.JSONArray
import org.json.JSONObject

data class AppointmentEntry(
    val id: Long,
    val patientName: String,
    val patientEmail: String?,
    val date: String,
    val time: String,
    val reason: String,
    val status: String,
    val doctorName: String?,
)

fun parseAppointments(body: String): List<AppointmentEntry> {
    return try {
        val array = JSONArray(body)
        buildList(array.length()) {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(item.toAppointmentEntry())
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun JSONObject.toAppointmentEntry(): AppointmentEntry {
    return AppointmentEntry(
        id = optLong("id", -1L),
        patientName = optString("patientName", "Patient"),
        patientEmail = if (has("patientEmail") && !isNull("patientEmail")) optString("patientEmail") else null,
        date = optString("date", ""),
        time = optString("time", ""),
        reason = optString("reason", "Appointment"),
        status = optString("status", "PROCESSING"),
        doctorName = if (has("doctorName") && !isNull("doctorName")) optString("doctorName") else null,
    )
}

fun AppointmentEntry.timeHour(): Int {
    return time.take(2).toIntOrNull() ?: 0
}

fun AppointmentEntry.dateCalendar(): Calendar {
    return try {
        val parts = date.split("-")
        Calendar.getInstance().apply {
            set(Calendar.YEAR, parts[0].toInt())
            set(Calendar.MONTH, parts[1].toInt() - 1)
            set(Calendar.DAY_OF_MONTH, parts[2].toInt())
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    } catch (_: Exception) {
        Calendar.getInstance()
    }
}
