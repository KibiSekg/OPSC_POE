package com.example.opsc_poe

import android.content.Intent
import android.util.Log // Imported Android Log utility
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

/**
 * Universal navigation configurations utility. Centralizes event listener bindings
 * and routing logic for both the side Drawer layout menu and the Bottom Navigation bar.
 */
fun setupNavigation(activity: AppCompatActivity, currentItemId: Int) {
    // Dynamically generate tag based on the caller activity class name for precise tracking
    val tag = "NavigationHelper:${activity.javaClass.simpleName}"
    Log.d(tag, "setupNavigation: Initializing side drawer and bottom navigation UI binds.")

    val drawerLayout = activity.findViewById<DrawerLayout>(R.id.drawerLayout)
    val toolbar      = activity.findViewById<MaterialToolbar>(R.id.toolbar)
    val navView      = activity.findViewById<NavigationView>(R.id.navigationView)
    val bottomNav    = activity.findViewById<BottomNavigationView>(R.id.bottomNavigationView)

    // Verify critical navigation elements exist in the inflating activity layout tree
    if (drawerLayout == null || toolbar == null || navView == null || bottomNav == null) {
        Log.e(tag, "setupNavigation ERROR: One or more navigation layout components returned null targets. " +
                "Drawer: $drawerLayout, Toolbar: $toolbar, NavView: $navView, BottomNav: $bottomNav")
        return
    }

    // Configure structural Action Bar Drawer open/close toggle synchronization state
    val toggle = ActionBarDrawerToggle(
        activity, drawerLayout, toolbar,
        R.string.navigation_drawer_open,
        R.string.navigation_drawer_close
    )
    drawerLayout.addDrawerListener(toggle)
    toggle.syncState()

    // Bind item selection event routes for the Drawer menu panel
    navView.setNavigationItemSelectedListener { menuItem: MenuItem ->
        Log.d(tag, "Drawer item clicked: ID=${menuItem.itemId}, Title='${menuItem.title}'")
        drawerLayout.closeDrawers()

        when (menuItem.itemId) {
            R.id.nav_home -> {
                if (activity !is Home) {
                    Log.d(tag, "Navigating away -> Starting Home activity context.")
                    activity.startActivity(Intent(activity, Home::class.java))
                } else {
                    Log.d(tag, "Navigation ignored: User is already on Home view canvas.")
                }
            }
            R.id.nav_expense -> {
                if (activity !is Expense) {
                    Log.d(tag, "Navigating away -> Starting Expense activity context.")
                    activity.startActivity(Intent(activity, Expense::class.java))
                } else {
                    Log.d(tag, "Navigation ignored: User is already on Expense view canvas.")
                }
            }
            R.id.nav_analytics -> {
                if (activity !is Analytics) {
                    Log.d(tag, "Navigating away -> Starting Analytics activity context.")
                    activity.startActivity(Intent(activity, Analytics::class.java))
                } else {
                    Log.d(tag, "Navigation ignored: User is already on Analytics view canvas.")
                }
            }
            R.id.nav_profile -> {
                if (activity !is Profile) {
                    Log.d(tag, "Navigating away -> Starting Profile activity context.")
                    activity.startActivity(Intent(activity, Profile::class.java))
                } else {
                    Log.d(tag, "Navigation ignored: User is already on Profile view canvas.")
                }
            }
            else -> {
                Log.e(tag, "Drawer Item Selection Exception: Unexpected resource menu ID token encountered: ${menuItem.itemId}")
            }
        }
        true
    }

    // 1. Clear listener temporarily to avoid initial state assignment loops or tracking recursive bugs
    bottomNav.setOnItemSelectedListener(null)

    // 2. Set the active item ID state securely to reflect current tracking positions
    Log.d(tag, "Synchronizing current active Bottom Navigation index position to ID: $currentItemId")
    bottomNav.selectedItemId = currentItemId

    // 3. Bind conditional routing action intercept loops cleanly to the Bottom bar item slots
    bottomNav.setOnItemSelectedListener { item ->
        Log.d(tag, "Bottom navigation bar item clicked: ID=${item.itemId}, Title='${item.title}'")
        when (item.itemId) {
            R.id.btnHome -> {
                if (activity !is Home) {
                    Log.d(tag, "Navigating away -> Launching Home via bottom menu selection.")
                    activity.startActivity(Intent(activity, Home::class.java))
                }
                true
            }
            R.id.btnExpInc -> {
                if (activity !is Expense) {
                    Log.d(tag, "Navigating away -> Launching Expense via bottom menu selection.")
                    activity.startActivity(Intent(activity, Expense::class.java))
                }
                true
            }
            R.id.btnAnalytics -> {
                if (activity !is Analytics) {
                    Log.d(tag, "Navigating away -> Launching Analytics via bottom menu selection.")
                    activity.startActivity(Intent(activity, Analytics::class.java))
                }
                true
            }
            R.id.btnProfile -> {
                if (activity !is Profile) {
                    Log.d(tag, "Navigating away -> Launching Profile via bottom menu selection.")
                    activity.startActivity(Intent(activity, Profile::class.java))
                }
                true
            }
            else -> {
                Log.e(tag, "Bottom Nav Item Selection Exception: Unmapped configuration ID slot encountered: ${item.itemId}")
                false
            }
        }
    }
}