package com.example.mobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.button.MaterialButton
import androidx.fragment.app.Fragment
import com.example.mobile.api.ClinicApi
import java.util.Calendar

class DashboardFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        val token = getStoredToken(requireContext())
        val patientStats = view.findViewById<LinearLayout>(R.id.dashboard_stats_patient)
        val staffStats = view.findViewById<LinearLayout>(R.id.dashboard_stats_staff)
        val nextAppointmentValue = view.findViewById<TextView>(R.id.tv_next_appointment_value)
        val recordsValue = view.findViewById<TextView>(R.id.tv_records_value)
        val notificationsValue = view.findViewById<TextView>(R.id.tv_notifications_value)
        val doctorsValue = view.findViewById<TextView>(R.id.tv_doctors_value)
        val nextVisitTitle = view.findViewById<TextView>(R.id.tv_next_visit_label)
        val nextVisitValue = view.findViewById<TextView>(R.id.tv_next_visit_value)
        val nextVisitNote = view.findViewById<TextView>(R.id.tv_next_visit_note)

        if (token.isNullOrBlank()) {
            nextAppointmentValue.text = "Not signed in"
            recordsValue.text = "0"
            notificationsValue.text = "0"
            doctorsValue.text = "0"
            patientStats.isVisible = true
            staffStats.isVisible = false
            nextVisitTitle.text = "Sign in required"
            nextVisitValue.text = "Open the app again after logging in"
            nextVisitNote.text = "Your data is loaded from the Render server after authentication."
        } else {
            loadLiveDashboard(view, token)
        }

        view.findViewById<MaterialButton>(R.id.btn_dashboard_appointments).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.content_frame, AppointmentsFragment())
                .commit()
        }

        view.findViewById<MaterialButton>(R.id.btn_dashboard_records).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.content_frame, RecordsFragment())
                .commit()
        }

        return view
    }

    private fun loadLiveDashboard(view: View, token: String) {
        val patientStats = view.findViewById<LinearLayout>(R.id.dashboard_stats_patient)
        val staffStats = view.findViewById<LinearLayout>(R.id.dashboard_stats_staff)
        val nextAppointmentValue = view.findViewById<TextView>(R.id.tv_next_appointment_value)
        val recordsValue = view.findViewById<TextView>(R.id.tv_records_value)
        val notificationsValue = view.findViewById<TextView>(R.id.tv_notifications_value)
        val doctorsValue = view.findViewById<TextView>(R.id.tv_doctors_value)
        val nextVisitTitle = view.findViewById<TextView>(R.id.tv_next_visit_label)
        val nextVisitValue = view.findViewById<TextView>(R.id.tv_next_visit_value)
        val nextVisitNote = view.findViewById<TextView>(R.id.tv_next_visit_note)

        Thread {
            try {
                val profile = ClinicApi.fetchProfile(token)
                val appointments = ClinicApi.fetchAppointments(token)
                val records = ClinicApi.fetchRecords(token)
                val notifications = runCatching { ClinicApi.fetchNotifications(token) }.getOrDefault(emptyList())
                val doctors = if (profile.role >= 2) runCatching { ClinicApi.fetchDoctors(token) }.getOrDefault(emptyList()) else emptyList()

                val now = Calendar.getInstance()
                val nextAppointment = appointments
                    .mapNotNull { entry ->
                        val calendar = entry.dateCalendar()
                        calendar.set(Calendar.HOUR_OF_DAY, entry.timeHour())
                        calendar.set(Calendar.MINUTE, 0)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        if (calendar.before(now)) null else entry to calendar
                    }
                    .sortedBy { it.second.timeInMillis }
                    .firstOrNull()
                    ?.first

                val unreadNotifications = notifications.count { !it.isRead }
                val doctorsInCount = doctors.count { it.doctorIn || it.available }

                activity?.runOnUiThread {
                    val isStaff = profile.role >= 2
                    patientStats.isVisible = !isStaff
                    staffStats.isVisible = isStaff

                    if (isStaff) {
                        view.findViewById<TextView>(R.id.tv_next_appointment_title).text = "Pending Appointments"
                        nextAppointmentValue.text = appointments.count { appointment ->
                            appointment.status.equals("PROCESSING", ignoreCase = true) ||
                                appointment.status.equals("APPROVED", ignoreCase = true)
                        }.toString()

                        view.findViewById<TextView>(R.id.tv_records_title).text = "Completed Appointments"
                        recordsValue.text = appointments.count { appointment ->
                            appointment.status.equals("COMPLETED", ignoreCase = true)
                        }.toString()

                        notificationsValue.text = unreadNotifications.toString()
                        doctorsValue.text = doctorsInCount.toString()

                        nextVisitTitle.text = "Live clinic data"
                        nextVisitValue.text = if (appointments.isEmpty()) "No appointments yet" else "${appointments.size} appointment(s) loaded"
                        nextVisitNote.text = "Appointments, records, notifications, and doctors are now loaded from ${BuildConfig.BACKEND_URL}."
                    } else {
                        view.findViewById<TextView>(R.id.tv_next_appointment_title).text = "Next Appointment"
                        nextAppointmentValue.text = nextAppointment?.let { "${it.date} ${it.time}" } ?: "No upcoming"
                        view.findViewById<TextView>(R.id.tv_records_title).text = "Number of Visits"
                        recordsValue.text = records.size.toString()
                        notificationsValue.text = unreadNotifications.toString()
                        doctorsValue.text = "0"

                        nextVisitTitle.text = "Your clinic summary"
                        nextVisitValue.text = if (records.isEmpty()) "No records yet" else "${records.size} record(s) loaded"
                        nextVisitNote.text = "Patient data now comes from the same backend as the web app."
                    }
                }
            } catch (ex: Exception) {
                activity?.runOnUiThread {
                    patientStats.isVisible = true
                    staffStats.isVisible = false
                    nextAppointmentValue.text = "Error"
                    recordsValue.text = "--"
                    notificationsValue.text = "--"
                    doctorsValue.text = "--"
                    nextVisitTitle.text = "Unable to load dashboard"
                    nextVisitValue.text = "Check your connection and sign in again."
                    nextVisitNote.text = ex.message ?: "Failed to connect to the backend."
                }
            }
        }.start()
    }
}