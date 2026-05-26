package com.example.mobile

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.Calendar
import java.util.Locale

class AppointmentsFragment : Fragment() {
    private lateinit var client: OkHttpClient

    private var appointments: List<AppointmentEntry> = emptyList()
    private var currentMonth: Calendar = startOfMonth(todayCalendar())
    private var selectedDay: Calendar = todayCalendar()

    private lateinit var monthLabel: TextView
    private lateinit var calendarRowsContainer: LinearLayout
    private lateinit var selectedDayTitle: TextView
    private lateinit var selectedDayStatus: TextView
    private lateinit var selectedDayHoursContainer: LinearLayout
    private lateinit var loadingText: TextView
    private lateinit var errorText: TextView
    private lateinit var bookButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val logging = HttpLoggingInterceptor()
        logging.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        client = OkHttpClient.Builder().addInterceptor(logging).build()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_appointments, container, false)

        monthLabel = view.findViewById(R.id.tv_month_label)
        calendarRowsContainer = view.findViewById(R.id.calendar_rows_container)
        selectedDayTitle = view.findViewById(R.id.tv_selected_day_title)
        selectedDayStatus = view.findViewById(R.id.tv_selected_day_status)
        selectedDayHoursContainer = view.findViewById(R.id.selected_day_hours_container)
        loadingText = view.findViewById(R.id.tv_appointments_loading)
        errorText = view.findViewById(R.id.tv_appointments_error)
        bookButton = view.findViewById(R.id.btn_new_appointment)

        view.findViewById<MaterialButton>(R.id.btn_prev_month).setOnClickListener {
            moveMonth(-1)
        }
        view.findViewById<MaterialButton>(R.id.btn_today).setOnClickListener {
            currentMonth = startOfMonth(todayCalendar())
            selectedDay = todayCalendar()
            renderCalendar()
            renderSelectedDay()
        }
        view.findViewById<MaterialButton>(R.id.btn_next_month).setOnClickListener {
            moveMonth(1)
        }

        bookButton.setOnClickListener {
            openBookingDialog(selectedDay)
        }

        childFragmentManager.setFragmentResultListener(AppointmentBookingDialogFragment.RESULT_KEY, this) { _, bundle ->
            if (bundle.getBoolean(AppointmentBookingDialogFragment.RESULT_SUCCESS, false)) {
                loadAppointments()
            }
        }

        renderCalendar()
        renderSelectedDay()
        loadAppointments()

        return view
    }

    private fun loadAppointments() {
        val token = getStoredToken(requireContext())
        if (token.isNullOrBlank()) {
            appointments = emptyList()
            loadingText.visibility = View.GONE
            errorText.visibility = View.VISIBLE
            errorText.text = "Not signed in."
            renderCalendar()
            renderSelectedDay()
            return
        }

        loadingText.visibility = View.VISIBLE
        errorText.visibility = View.GONE

        Thread {
            try {
                val request = Request.Builder()
                    .url(BuildConfig.BACKEND_URL + "/api/appointments")
                    .get()
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    postUi {
                        appointments = emptyList()
                        loadingText.visibility = View.GONE
                        errorText.visibility = View.VISIBLE
                        errorText.text = "Unable to load appointments right now."
                        renderCalendar()
                        renderSelectedDay()
                    }
                    return@Thread
                }

                val loaded = parseAppointments(body)
                postUi {
                    appointments = loaded
                    loadingText.visibility = View.GONE
                    errorText.visibility = View.GONE
                    renderCalendar()
                    renderSelectedDay()
                }
            } catch (_: Exception) {
                postUi {
                    appointments = emptyList()
                    loadingText.visibility = View.GONE
                    errorText.visibility = View.VISIBLE
                    errorText.text = "Unable to load appointments right now."
                    renderCalendar()
                    renderSelectedDay()
                }
            }
        }.start()
    }

    private fun openBookingDialog(initialDay: Calendar) {
        if (isPastDay(initialDay) && !sameDay(initialDay, todayCalendar())) {
            return
        }

        val defaultHour = if (sameDay(initialDay, todayCalendar())) nextBookableHour(initialDay) else 9
        val dialog = AppointmentBookingDialogFragment.newInstance(
            formatDateKey(initialDay),
            defaultHour.toString().padStart(2, '0') + ":00"
        )
        dialog.show(childFragmentManager, "appointment_booking")
    }

    private fun moveMonth(delta: Int) {
        currentMonth = startOfMonth((currentMonth.clone() as Calendar).apply { add(Calendar.MONTH, delta) })
        selectedDay = (currentMonth.clone() as Calendar)
        renderCalendar()
        renderSelectedDay()
    }

    private fun renderCalendar() {
        monthLabel.text = formatMonthLabel(currentMonth)
        calendarRowsContainer.removeAllViews()

        calendarRowsContainer.addView(buildWeekdayRow())
        buildMonthCells(currentMonth).chunked(7).forEach { week ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            week.forEachIndexed { index, cell ->
                row.addView(createDayCell(cell, index == 6))
            }

            calendarRowsContainer.addView(row)
        }
    }

    private fun renderSelectedDay() {
        selectedDayTitle.text = formatSelectedDayLabel(selectedDay)

        val dayAppointments = appointments.filter { sameDay(it.dateCalendar(), selectedDay) }
        val isPast = isPastDay(selectedDay)
        val canBookSelectedDay = !isPast || hasFutureHoursForDay(selectedDay)

        selectedDayStatus.text = if (isPast && !sameDay(selectedDay, todayCalendar())) {
            "Past day - booking disabled"
        } else {
            "${dayAppointments.size} appointment(s) on this day"
        }

        bookButton.isEnabled = canBookSelectedDay
        bookButton.alpha = if (canBookSelectedDay) 1f else 0.55f

        selectedDayHoursContainer.removeAllViews()
        for (hour in 0 until 24) {
            val booked = dayAppointments.firstOrNull { it.timeHour() == hour }
            val isPastHour = isPastSlot(selectedDay, hour)
            val canBook = booked == null && !isPastHour

            val card = MaterialCardView(requireContext()).apply {
                radius = dp(16).toFloat()
                cardElevation = 0f
                strokeWidth = 1
                strokeColor = getColorCompat(if (booked != null) android.R.color.holo_red_light else R.color.wildcat_border)
                setCardBackgroundColor(
                    getColorCompat(
                        when {
                            booked != null -> android.R.color.white
                            isPastHour -> android.R.color.darker_gray
                            else -> R.color.wildcat_surface
                        }
                    )
                )
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(12) }
                isClickable = canBook
                isFocusable = canBook
                setOnClickListener {
                    if (canBook) {
                        openBookingDialog(selectedDay.withHour(hour))
                    }
                }
            }

            val content = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(dp(18), dp(18), dp(18), dp(18))
            }

            val left = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }

            left.addView(TextView(requireContext()).apply {
                text = formatHour(hour)
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(getColorCompat(R.color.wildcat_text_primary))
            })

            left.addView(TextView(requireContext()).apply {
                text = when {
                    booked != null -> booked.reason
                    isPastHour -> "Past time"
                    else -> "Available"
                }
                setTextColor(getColorCompat(R.color.wildcat_text_secondary))
            })

            content.addView(left)
            content.addView(TextView(requireContext()).apply {
                text = when {
                    booked != null -> "Booked"
                    isPastHour -> "Past"
                    else -> "Book"
                }
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(getColorCompat(R.color.wildcat_burgundy))
            })
            card.addView(content)
            selectedDayHoursContainer.addView(card)
        }
    }

    private fun buildWeekdayRow(): LinearLayout {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 0, 0, dp(12))
        }

        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
            row.addView(TextView(requireContext()).apply {
                text = day
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(getColorCompat(R.color.wildcat_text_secondary))
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
        }

        return row
    }

    private fun createDayCell(date: Calendar?, isLastColumn: Boolean): View {
        if (date == null) {
            return View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(84), 1f).apply {
                    bottomMargin = dp(8)
                    rightMargin = if (isLastColumn) 0 else dp(6)
                }
            }
        }

        val dayAppointments = appointments.filter { sameDay(it.dateCalendar(), date) }
        val isSelected = sameDay(date, selectedDay)
        val isToday = sameDay(date, todayCalendar())

        return MaterialCardView(requireContext()).apply {
            radius = dp(16).toFloat()
            strokeWidth = 1
            strokeColor = getColorCompat(if (isSelected) R.color.wildcat_burgundy else R.color.wildcat_border)
            setCardBackgroundColor(getColorCompat(if (isSelected) R.color.wildcat_burgundy else R.color.wildcat_surface))
            layoutParams = LinearLayout.LayoutParams(0, dp(84), 1f).apply {
                bottomMargin = dp(8)
                rightMargin = if (isLastColumn) 0 else dp(6)
            }
            isClickable = true
            isFocusable = true
            setOnClickListener {
                selectedDay = date.clone() as Calendar
                renderCalendar()
                renderSelectedDay()
            }

            val content = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(10), dp(10), dp(10))
            }

            content.addView(TextView(context).apply {
                text = date.get(Calendar.DAY_OF_MONTH).toString()
                setTypeface(Typeface.DEFAULT_BOLD)
                setTextColor(getColorCompat(if (isSelected) android.R.color.white else R.color.wildcat_text_primary))
            })

            content.addView(TextView(context).apply {
                text = when {
                    isToday -> "Today"
                    dayAppointments.isNotEmpty() -> "${dayAppointments.size} appt"
                    else -> ""
                }
                setTextColor(getColorCompat(if (isSelected) android.R.color.white else R.color.wildcat_text_secondary))
                textSize = 11f
            })

            addView(content)
        }
    }

    private fun nextBookableHour(day: Calendar): Int {
        val now = todayCalendar()
        if (!sameDay(day, now)) return 9

        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val nextHour = currentHour + if (currentMinute > 0) 1 else 0
        return nextHour.coerceAtMost(23)
    }

    private fun hasFutureHoursForDay(day: Calendar): Boolean {
        return (0..23).any { !isPastSlot(day, it) }
    }

    private fun isPastDay(day: Calendar): Boolean {
        val today = todayCalendar()
        return day.isBeforeDay(today)
    }

    private fun isPastSlot(day: Calendar, hour: Int): Boolean {
        val now = todayCalendar()
        if (!sameDay(day, now)) return false

        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        return hour < currentHour || (hour == currentHour && currentMinute > 0)
    }

    private fun sameDay(left: Calendar, right: Calendar): Boolean {
        return left.get(Calendar.YEAR) == right.get(Calendar.YEAR) &&
            left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR)
    }

    private fun Calendar.isBeforeDay(other: Calendar): Boolean {
        return when {
            get(Calendar.YEAR) < other.get(Calendar.YEAR) -> true
            get(Calendar.YEAR) > other.get(Calendar.YEAR) -> false
            get(Calendar.DAY_OF_YEAR) < other.get(Calendar.DAY_OF_YEAR) -> true
            else -> false
        }
    }

    private fun Calendar.withHour(hour: Int): Calendar {
        return (clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun postUi(action: () -> Unit) {
        activity?.runOnUiThread(action)
    }

    private fun buildMonthCells(monthDate: Calendar): List<Calendar?> {
        val cells = mutableListOf<Calendar?>()
        val firstDay = startOfMonth(monthDate)
        val monthDays = monthDate.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstWeekday = firstDay.get(Calendar.DAY_OF_WEEK) - 1

        repeat(firstWeekday) { cells.add(null) }

        for (day in 1..monthDays) {
            cells.add((monthDate.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) })
        }

        while (cells.size % 7 != 0) {
            cells.add(null)
        }

        return cells
    }

    private fun startOfMonth(calendar: Calendar): Calendar {
        return (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun todayCalendar(): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun formatMonthLabel(calendar: Calendar): String {
        val monthName = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
        return "$monthName ${calendar.get(Calendar.YEAR)}"
    }

    private fun formatSelectedDayLabel(calendar: Calendar): String {
        val weekday = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault()) ?: ""
        val month = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault()) ?: ""
        return "$weekday, $month ${calendar.get(Calendar.DAY_OF_MONTH)}, ${calendar.get(Calendar.YEAR)}"
    }

    private fun formatDateKey(calendar: Calendar): String {
        val year = calendar.get(Calendar.YEAR)
        val month = (calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        val day = calendar.get(Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        return "$year-$month-$day"
    }

    private fun formatHour(hour: Int): String {
        return hour.toString().padStart(2, '0') + ":00"
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun getColorCompat(colorRes: Int): Int = ContextCompat.getColor(requireContext(), colorRes)
}
