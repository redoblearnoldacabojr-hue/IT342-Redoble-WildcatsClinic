package com.example.mobile

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.Calendar

class AppointmentBookingDialogFragment : DialogFragment() {
    private lateinit var client: OkHttpClient
    private lateinit var selectedDate: Calendar
    private lateinit var selectedTime: Calendar

    private lateinit var dateField: android.widget.EditText
    private lateinit var timeField: android.widget.EditText
    private lateinit var reasonPreview: TextView
    private lateinit var bookingPreview: TextView
    private lateinit var errorText: TextView
    private lateinit var reasonOptions: Array<String>
    private lateinit var reasonSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val logging = HttpLoggingInterceptor()
        logging.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        client = OkHttpClient.Builder().addInterceptor(logging).build()

        val initialDate = requireArguments().getString(ARG_DATE).orEmpty()
        val initialTime = requireArguments().getString(ARG_TIME).orEmpty()

        selectedDate = parseDateKey(initialDate) ?: todayCalendar()
        selectedTime = parseTimeKey(initialTime) ?: defaultTimeForDate(selectedDate)
        reasonOptions = arrayOf("General Consultation", "Medical Services", "Dental Service")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_appointment_booking, container, false)

        dateField = view.findViewById(R.id.input_date)
        timeField = view.findViewById(R.id.input_time)
        reasonPreview = view.findViewById(R.id.tv_reason_preview)
        bookingPreview = view.findViewById(R.id.tv_booking_preview)
        errorText = view.findViewById(R.id.tv_booking_error)
        reasonSpinner = view.findViewById(R.id.spinner_reason)

        reasonSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, reasonOptions)
        reasonSpinner.setSelection(0)

        dateField.setText(formatDate(selectedDate))
        timeField.setText(formatTime(selectedTime))
        reasonPreview.text = reasonOptions[0]
        refreshPreview()

        view.findViewById<MaterialButton>(R.id.btn_pick_date).setOnClickListener { showDatePicker() }
        view.findViewById<MaterialButton>(R.id.btn_pick_time).setOnClickListener { showTimePicker() }
        view.findViewById<MaterialButton>(R.id.btn_cancel_booking).setOnClickListener { dismiss() }
        view.findViewById<MaterialButton>(R.id.btn_confirm_booking).setOnClickListener { submitBooking() }

        reasonSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, selectedView: View?, position: Int, id: Long) {
                reasonPreview.text = reasonOptions[position]
                refreshPreview()
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }

        return view
    }

    private fun showDatePicker() {
        val today = todayCalendar()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                selectedDate = calendarOf(year, month, day)
                if (isToday(selectedDate)) {
                    val nextHour = nextAllowedHour()
                    selectedTime = selectedDate.clone() as Calendar
                    selectedTime.set(Calendar.HOUR_OF_DAY, nextHour)
                    selectedTime.set(Calendar.MINUTE, 0)
                } else {
                    selectedTime = defaultTimeForDate(selectedDate)
                }
                dateField.setText(formatDate(selectedDate))
                timeField.setText(formatTime(selectedTime))
                errorText.visibility = View.GONE
                refreshPreview()
            },
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = today.timeInMillis
            show()
        }
    }

    private fun showTimePicker() {
        val initialHour = selectedTime.get(Calendar.HOUR_OF_DAY)
        val initialMinute = selectedTime.get(Calendar.MINUTE)
        TimePickerDialog(
            requireContext(),
            { _, hourOfDay, minute ->
                selectedTime = selectedDate.clone() as Calendar
                selectedTime.set(Calendar.HOUR_OF_DAY, hourOfDay)
                selectedTime.set(Calendar.MINUTE, minute)
                timeField.setText(formatTime(selectedTime))
                errorText.visibility = View.GONE
                refreshPreview()
            },
            initialHour,
            initialMinute,
            true
        ).show()
    }

    private fun submitBooking() {
        val token = getStoredToken(requireContext())
        if (token.isNullOrBlank()) {
            showError("Not signed in.")
            return
        }

        val dateText = dateField.text?.toString().orEmpty()
        val timeText = timeField.text?.toString().orEmpty()
        val selectedReason = reasonSpinner.selectedItem?.toString().orEmpty().ifBlank { reasonOptions[0] }

        val parsedDate = parseDateKey(dateText)
        val parsedTime = parseTimeKey(timeText)
        if (parsedDate == null || parsedTime == null) {
            showError("Please select a valid date and time.")
            return
        }

        if (isPastSelection(parsedDate, parsedTime.get(Calendar.HOUR_OF_DAY), parsedTime.get(Calendar.MINUTE))) {
            showError("Cannot book a previous day or previous time.")
            return
        }

        val payload = "{\"date\":\"$dateText\",\"time\":\"$timeText\",\"reason\":\"${escapeJson(selectedReason)}\"}"
        val request = Request.Builder()
            .url(BuildConfig.BACKEND_URL + "/api/appointments")
            .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .addHeader("Authorization", "Bearer $token")
            .build()

        errorText.visibility = View.GONE

        Thread {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val result = Bundle().apply { putBoolean(RESULT_SUCCESS, true) }
                    parentFragmentManager.setFragmentResult(RESULT_KEY, result)
                    dismissAllowingStateLoss()
                    return@Thread
                }

                val message = parseErrorMessage(responseBody)
                postUi { showError(message) }
            } catch (_: Exception) {
                postUi { showError("Unable to book appointment right now.") }
            }
        }.start()
    }

    private fun refreshPreview() {
        bookingPreview.text = "${formatDate(selectedDate)} at ${formatTime(selectedTime)}"
    }

    private fun showError(message: String) {
        errorText.visibility = View.VISIBLE
        errorText.text = message
    }

    private fun postUi(action: () -> Unit) {
        activity?.runOnUiThread(action)
    }

    private fun isPastSelection(date: Calendar, hour: Int, minute: Int): Boolean {
        val now = todayCalendar()
        if (isBeforeDay(date, now)) return true
        if (!sameDay(date, now)) return false

        val selectedMinutes = hour * 60 + minute
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        return selectedMinutes < currentMinutes
    }

    private fun nextAllowedHour(): Int {
        val now = todayCalendar()
        return (now.get(Calendar.HOUR_OF_DAY) + 1).coerceAtMost(23)
    }

    private fun defaultTimeForDate(date: Calendar): Calendar {
        return (date.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, if (isToday(date)) nextAllowedHour() else 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun isToday(date: Calendar): Boolean = sameDay(date, todayCalendar())

    private fun sameDay(left: Calendar, right: Calendar): Boolean {
        return left.get(Calendar.YEAR) == right.get(Calendar.YEAR) &&
            left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR)
    }

    private fun isBeforeDay(left: Calendar, right: Calendar): Boolean {
        return when {
            left.get(Calendar.YEAR) < right.get(Calendar.YEAR) -> true
            left.get(Calendar.YEAR) > right.get(Calendar.YEAR) -> false
            left.get(Calendar.DAY_OF_YEAR) < right.get(Calendar.DAY_OF_YEAR) -> true
            else -> false
        }
    }

    private fun todayCalendar(): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun parseDateKey(value: String): Calendar? {
        return try {
            val parts = value.split("-")
            if (parts.size != 3) return null
            calendarOf(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        } catch (_: Exception) {
            null
        }
    }

    private fun parseTimeKey(value: String): Calendar? {
        return try {
            val parts = value.split(":")
            if (parts.size < 2) return null
            calendarOf(1970, 0, 1).apply {
                set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                set(Calendar.MINUTE, parts[1].toInt())
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun calendarOf(year: Int, month: Int, day: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun formatDate(calendar: Calendar): String {
        val year = calendar.get(Calendar.YEAR)
        val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        return "$year-$month-$day"
    }

    private fun formatTime(calendar: Calendar): String {
        val hour = calendar.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
        val minute = calendar.get(Calendar.MINUTE).toString().padStart(2, '0')
        return "$hour:$minute"
    }

    private fun parseErrorMessage(body: String): String {
        return try {
            org.json.JSONObject(body).optString("message", "Unable to book appointment.")
        } catch (_: Exception) {
            "Unable to book appointment."
        }
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    companion object {
        const val RESULT_KEY = "appointment_booking_result"
        const val RESULT_SUCCESS = "appointment_booking_success"
        private const val ARG_DATE = "arg_date"
        private const val ARG_TIME = "arg_time"

        fun newInstance(date: String, time: String): AppointmentBookingDialogFragment {
            return AppointmentBookingDialogFragment().apply {
                arguments = android.os.Bundle().apply {
                    putString(ARG_DATE, date)
                    putString(ARG_TIME, time)
                }
            }
        }
    }
}
