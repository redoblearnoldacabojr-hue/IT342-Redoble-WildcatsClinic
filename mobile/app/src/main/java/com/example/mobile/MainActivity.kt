package com.example.mobile

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.fragment.app.commit
import android.view.MenuItem
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val storedRole = getStoredRole(this)
        if (storedRole != null && !canUseMobileApp(storedRole)) {
            clearAuthSession(this)
            openLoginActivity(this)
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val drawer = findViewById<DrawerLayout>(R.id.main)
        val toggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.app_name, R.string.app_name)
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        toggle.drawerArrowDrawable.color = ContextCompat.getColor(this, android.R.color.white)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        val nav = findViewById<NavigationView>(R.id.navigation_view)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val header = nav.getHeaderView(0)
        val nameView = header.findViewById<TextView>(R.id.drawer_user_name)
        val emailView = header.findViewById<TextView>(R.id.drawer_user_email)
        val roleView = header.findViewById<TextView>(R.id.drawer_user_role)

        nameView.text = getStoredDisplayName(this) ?: "WildcatsClinic User"
        emailView.text = getStoredEmail(this) ?: ""
        roleView.text = when (getStoredRole(this) ?: 1) {
            1 -> "User"
            2 -> "Staff"
            3 -> "Admin"
            else -> "User"
        }

        fun showDashboard() {
            supportFragmentManager.commit { replace(R.id.content_frame, DashboardFragment()) }
        }

        fun showAppointments() {
            supportFragmentManager.commit { replace(R.id.content_frame, AppointmentsFragment()) }
        }

        fun showRecords() {
            supportFragmentManager.commit { replace(R.id.content_frame, RecordsFragment()) }
        }

        nav.setNavigationItemSelectedListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> showDashboard()
                R.id.nav_appointments -> showAppointments()
                R.id.nav_records -> showRecords()
                R.id.nav_signout -> {
                    clearAuthSession(this)
                    openLoginActivity(this)
                }
            }
            drawer.closeDrawers()
            true
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_bottom_dashboard -> {
                    showDashboard()
                    true
                }
                R.id.nav_bottom_appointments -> {
                    showAppointments()
                    true
                }
                R.id.nav_bottom_records -> {
                    showRecords()
                    true
                }
                R.id.nav_bottom_menu -> {
                    drawer.openDrawer(androidx.core.view.GravityCompat.START)
                    false
                }
                else -> false
            }
        }

        // default fragment
        if (savedInstanceState == null) {
            showDashboard()
            bottomNav.selectedItemId = R.id.nav_bottom_dashboard
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar)) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(v.paddingLeft, statusBars.top, v.paddingRight, v.paddingBottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { v, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, navigationBars.bottom)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.content_frame)) { v, insets ->
            v.setPadding(0, 0, 0, 0)
            insets
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == android.R.id.home) {
            findViewById<DrawerLayout>(R.id.main).openDrawer(androidx.core.view.GravityCompat.START)
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}