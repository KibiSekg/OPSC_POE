package com.example.opsc_poe

import android.content.Intent
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

fun setupNavigation(activity: AppCompatActivity, currentItemId: Int) {

    val drawerLayout = activity.findViewById<DrawerLayout>(R.id.drawerLayout)
    val toolbar      = activity.findViewById<MaterialToolbar>(R.id.toolbar)
    val navView      = activity.findViewById<NavigationView>(R.id.navigationView)
    val bottomNav    = activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView)

    // Drawer toggle
    val toggle = ActionBarDrawerToggle(
        activity, drawerLayout, toolbar,
        R.string.navigation_drawer_open,
        R.string.navigation_drawer_close
    )
    drawerLayout.addDrawerListener(toggle)
    toggle.syncState()

    // Side drawer clicks
    navView.setNavigationItemSelectedListener { menuItem: MenuItem ->
        drawerLayout.closeDrawers()
        when (menuItem.itemId) {
            R.id.nav_home      -> if (activity !is Home)      activity.startActivity(Intent(activity, Home::class.java))
            R.id.nav_expense   -> if (activity !is Expense)   activity.startActivity(Intent(activity, Expense::class.java))
            R.id.nav_analytics -> if (activity !is Analytics) activity.startActivity(Intent(activity, Analytics::class.java))
            R.id.nav_profile   -> if (activity !is Profile)   activity.startActivity(Intent(activity, Profile::class.java))
        }
        true
    }

    // 1. Clear listener temporarily to avoid initial state assignment tracking bugs
    bottomNav.setOnItemSelectedListener(null)

    // 2. Set the active item ID state securely
    bottomNav.selectedItemId = currentItemId

    // 3. Bind your conditional navigation logic cleanly
    bottomNav.setOnItemSelectedListener { item ->
        when (item.itemId) {
            R.id.btnHome      -> { if (activity !is Home)      activity.startActivity(Intent(activity, Home::class.java));      true }
            R.id.btnExpInc    -> { if (activity !is Expense)   activity.startActivity(Intent(activity, Expense::class.java));   true }
            R.id.btnAnalytics -> { if (activity !is Analytics) activity.startActivity(Intent(activity, Analytics::class.java)); true }
            R.id.btnProfile   -> { if (activity !is Profile)   activity.startActivity(Intent(activity, Profile::class.java));   true }
            else -> false
        }
    }
}