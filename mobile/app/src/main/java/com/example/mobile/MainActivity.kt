package com.example.mobile

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.fragment.app.commit
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        val drawer = findViewById<DrawerLayout>(R.id.main)
        val toggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.app_name, R.string.app_name)
        drawer.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        val nav = findViewById<NavigationView>(R.id.navigation_view)
        nav.setNavigationItemSelectedListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    supportFragmentManager.commit { replace(R.id.content_frame, DashboardFragment()) }
                }
                R.id.nav_appointments -> {
                    supportFragmentManager.commit { replace(R.id.content_frame, AppointmentsFragment()) }
                }
                R.id.nav_records -> {
                    supportFragmentManager.commit { replace(R.id.content_frame, RecordsFragment()) }
                }
                R.id.nav_signout -> {
                    clearAuthSession(this)
                    openLoginActivity(this)
                }
            }
            drawer.closeDrawers()
            true
        }

        // default fragment
        if (savedInstanceState == null) {
            supportFragmentManager.commit { replace(R.id.content_frame, DashboardFragment()) }
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
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