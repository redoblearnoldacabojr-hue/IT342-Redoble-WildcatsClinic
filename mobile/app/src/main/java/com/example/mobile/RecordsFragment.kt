package com.example.mobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.example.mobile.api.ClinicApi
import com.google.android.material.card.MaterialCardView

class RecordsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_records, container, false)
        val loadingText = view.findViewById<TextView>(R.id.tv_records_loading)
        val errorText = view.findViewById<TextView>(R.id.tv_records_error)
        val recordsContainer = view.findViewById<LinearLayout>(R.id.records_container)

        val token = getStoredToken(requireContext())
        if (token.isNullOrBlank()) {
            loadingText.text = "Sign in to load your records."
            errorText.isVisible = false
            return view
        }

        Thread {
            try {
                val records = ClinicApi.fetchRecords(token)
                activity?.runOnUiThread {
                    loadingText.isVisible = false
                    errorText.isVisible = false
                    recordsContainer.removeAllViews()

                    if (records.isEmpty()) {
                        recordsContainer.addView(TextView(requireContext()).apply {
                            text = "No records were returned from the backend yet."
                            setTextColor(getColor(R.color.wildcat_text_secondary))
                            setPadding(0, 0, 0, dp(12))
                        })
                        return@runOnUiThread
                    }

                    records.forEach { record ->
                        val card = MaterialCardView(requireContext()).apply {
                            radius = dp(16).toFloat()
                            cardElevation = 0f
                            strokeWidth = 1
                            strokeColor = getColor(R.color.wildcat_border)
                            setCardBackgroundColor(getColor(R.color.wildcat_surface))
                            layoutParams = LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            ).apply { bottomMargin = dp(12) }
                        }

                        val content = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(dp(16), dp(16), dp(16), dp(16))
                        }

                        content.addView(TextView(requireContext()).apply {
                            text = record.title.ifBlank { "Medical Record" }
                            setTextColor(getColor(R.color.wildcat_text_primary))
                            textSize = 18f
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                        })

                        content.addView(TextView(requireContext()).apply {
                            text = record.date.ifBlank { "Date unavailable" }
                            setTextColor(getColor(R.color.wildcat_burgundy))
                            setPadding(0, dp(4), 0, 0)
                        })

                        content.addView(TextView(requireContext()).apply {
                            text = record.summary.ifBlank { "No summary available." }
                            setTextColor(getColor(R.color.wildcat_text_secondary))
                            setPadding(0, dp(8), 0, 0)
                        })

                        content.addView(TextView(requireContext()).apply {
                            text = listOfNotNull(record.doctorName?.takeIf { it.isNotBlank() }?.let { "Doctor: $it" }, record.results?.takeIf { it.isNotBlank() }?.let { "Results: $it" }, record.remarks?.takeIf { it.isNotBlank() }?.let { "Remarks: $it" }).joinToString("\n")
                            if (text.isBlank()) {
                                text = "No extra details"
                            }
                            setTextColor(getColor(R.color.wildcat_text_muted))
                            setPadding(0, dp(8), 0, 0)
                        })

                        card.addView(content)
                        recordsContainer.addView(card)
                    }
                }
            } catch (ex: Exception) {
                activity?.runOnUiThread {
                    loadingText.isVisible = false
                    errorText.isVisible = true
                    errorText.text = ex.message ?: "Unable to load records right now."
                }
            }
        }.start()

        return view
    }

    private fun getColor(colorRes: Int): Int = androidx.core.content.ContextCompat.getColor(requireContext(), colorRes)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
