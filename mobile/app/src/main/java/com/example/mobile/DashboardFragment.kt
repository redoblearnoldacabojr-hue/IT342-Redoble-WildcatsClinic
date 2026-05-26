package com.example.mobile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import androidx.fragment.app.Fragment

class DashboardFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

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
}