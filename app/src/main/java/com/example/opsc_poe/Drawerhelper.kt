package com.example.opsc_poe

import android.content.Intent
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

/**
 * Wires up both the Navigation Drawer and the BottomNavigationView.
 *
 * Call once in each Activity's onCreate() AFTER setContentView():
 *
 *   setupNavigation(this, R.id.btnHome)      // Home.kt
 *   setupNavigation(this, R.id.btnExpInc)    // Expense.kt
 *   setupNavigation(this, R.id.btnProfile)   // Profile.kt
 */
fun setupNavigation(activity: AppCompatActivity, currentItemId: Int) {

    val drawerLayout  = activity.findViewById<DrawerLayout>(R.id.drawerLayout)
    val toolbar       = activity.findViewById<MaterialToolbar>(R.id.toolbar)
    val navView       = activity.findViewById<NavigationView>(R.id.navigationView)
    val bottomNav     = activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView)

    // ── Drawer toggle (hamburger icon) ─────────────────────────────
    val toggle = ActionBarDrawerToggle(
        activity, drawerLayout, toolbar,
        R.string.navigation_drawer_open,
        R.string.navigation_drawer_close
    )
    drawerLayout.addDrawerListener(toggle)
    toggle.syncState()

    // ── Side drawer item clicks ────────────────────────────────────
    navView.setNavigationItemSelectedListener { menuItem: MenuItem ->
        drawerLayout.closeDrawers()
        when (menuItem.itemId) {
            R.id.nav_home    -> if (activity !is Home)    activity.startActivity(Intent(activity, Home::class.java))
            R.id.nav_expense -> if (activity !is Expense) activity.startActivity(Intent(activity, Expense::class.java))
            R.id.nav_profile -> if (activity !is Profile) activity.startActivity(Intent(activity, Profile::class.java))
        }
        true
    }

    // ── Bottom nav: highlight current tab ─────────────────────────
    bottomNav.selectedItemId = currentItemId

    // ── Bottom nav item clicks ─────────────────────────────────────
    bottomNav.setOnItemSelectedListener { item ->
        when (item.itemId) {
            R.id.btnHome    -> { if (activity !is Home)    activity.startActivity(Intent(activity, Home::class.java));    true }
            R.id.btnExpInc  -> { if (activity !is Expense) activity.startActivity(Intent(activity, Expense::class.java)); true }
            R.id.btnProfile -> { if (activity !is Profile) activity.startActivity(Intent(activity, Profile::class.java)); true }
            else -> false
        }
    }
}