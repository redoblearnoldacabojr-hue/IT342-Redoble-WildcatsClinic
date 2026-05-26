package com.example.mobile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class AppointmentsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_appointments, container, false)
        val btnNew = view.findViewById<Button>(R.id.btn_new_appointment)
        btnNew.setOnClickListener {
            // For now, open GoogleSignInActivity to authenticate or exchange tokens
            context?.let { ctx ->
                GoogleSignInActivity().apply { }
                // In a real app, navigate to booking screen
            }
        }
        return view
    }
}
