package com.example.opsc_poe

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages all gamification: login streaks, expense milestones, badges.
 * Stored in SharedPreferences so it persists across sessions.
 */
object GamificationManager {

    private const val PREFS = "spend_smart_gamification"
    private const val KEY_LAST_LOGIN  = "last_login_date"
    private const val KEY_STREAK      = "login_streak"
    private const val KEY_TOTAL_EXP   = "total_expense_count"   // total ever saved
    private const val KEY_BADGES      = "unlocked_badges"       // comma-separated badge IDs

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ── Login streak ──────────────────────────────────────────────

    /**
     * Call this every time a user successfully logs in.
     * Returns the new streak count and updates streak badges.
     */
    fun recordLogin(ctx: Context): Int {
        val p       = prefs(ctx)
        val today   = dateFormat.format(Date())
        val last    = p.getString(KEY_LAST_LOGIN, "") ?: ""
        val streak  = p.getInt(KEY_STREAK, 0)

        val newStreak = when {
            last == today        -> streak           // already logged in today
            last == yesterday()  -> streak + 1      // consecutive day
            else                 -> 1               // streak broken
        }

        p.edit()
            .putString(KEY_LAST_LOGIN, today)
            .putInt(KEY_STREAK, newStreak)
            .apply()

        // FIX: Evaluate login streak badge assignments explicitly
        checkStreakBadges(ctx, newStreak)

        return newStreak
    }

    fun getStreak(ctx: Context): Int = prefs(ctx).getInt(KEY_STREAK, 0)

    private fun yesterday(): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DATE, -1)
        return dateFormat.format(cal.time)
    }

    private fun checkStreakBadges(ctx: Context, streakCount: Int) {
        val existing = getUnlockedBadgeIds(ctx)

        if (streakCount >= 3 && !existing.contains(Badge.STREAK_3.id)) {
            unlockBadge(ctx, Badge.STREAK_3)
        }
        if (streakCount >= 7 && !existing.contains(Badge.STREAK_7.id)) {
            unlockBadge(ctx, Badge.STREAK_7)
        }
    }

    // ── Expense milestones ────────────────────────────────────────

    /**
     * Call after every successful transaction save.
     * Returns list of newly unlocked badges (empty if none new).
     */
    fun recordExpense(ctx: Context): List<Badge> {
        val p     = prefs(ctx)
        val count = p.getInt(KEY_TOTAL_EXP, 0) + 1
        p.edit().putInt(KEY_TOTAL_EXP, count).apply()

        val existing = getUnlockedBadgeIds(ctx)
        val newBadges = mutableListOf<Badge>()

        Badge.values().forEach { badge ->
            if (badge.requiredCount != null && count >= badge.requiredCount && !existing.contains(badge.id)) {
                unlockBadge(ctx, badge)
                newBadges.add(badge)
            }
        }
        return newBadges
    }

    fun getExpenseCount(ctx: Context): Int = prefs(ctx).getInt(KEY_TOTAL_EXP, 0)

    // ── Badges ────────────────────────────────────────────────────

    fun unlockBadge(ctx: Context, badge: Badge) {
        val current = getUnlockedBadgeIds(ctx).toMutableSet()
        current.add(badge.id)
        prefs(ctx).edit().putString(KEY_BADGES, current.joinToString(",")).apply()
    }

    fun getUnlockedBadgeIds(ctx: Context): Set<String> {
        val raw = prefs(ctx).getString(KEY_BADGES, "") ?: ""
        return if (raw.isEmpty()) emptySet() else raw.split(",").toSet()
    }

    fun getUnlockedBadges(ctx: Context): List<Badge> {
        val ids = getUnlockedBadgeIds(ctx)
        return Badge.values().filter { ids.contains(it.id) }
    }
}

/**
 * All available badges in the app.
 * requiredCount = expense count needed to unlock (null = manually awarded via streak logic).
 */
enum class Badge(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val requiredCount: Int?
) {
    FIRST_EXPENSE(
        id            = "first_expense",
        title         = "First Step!",
        description   = "Added your first transaction",
        emoji         = "🥇",
        requiredCount = 1
    ),
    FIVE_EXPENSES(
        id            = "five_expenses",
        title         = "Getting Started",
        description   = "Added 5 transactions",
        emoji         = "⭐",
        requiredCount = 5
    ),
    TEN_EXPENSES(
        id            = "ten_expenses",
        title         = "On a Roll!",
        description   = "Added 10 transactions",
        emoji         = "🔥",
        requiredCount = 10
    ),
    TWENTY_FIVE_EXPENSES(
        id            = "twenty_five",
        title         = "Budget Pro",
        description   = "Added 25 transactions",
        emoji         = "💎",
        requiredCount = 25
    ),
    FIFTY_EXPENSES(
        id            = "fifty",
        title         = "Finance Master",
        description   = "Added 50 transactions",
        emoji         = "🏆",
        requiredCount = 50
    ),
    STREAK_3(
        id            = "streak_3",
        title         = "3-Day Streak",
        description   = "Logged in 3 days in a row",
        emoji         = "📅",
        requiredCount = null
    ),
    STREAK_7(
        id            = "streak_7",
        title         = "Week Warrior",
        description   = "Logged in 7 days in a row",
        emoji         = "🗓️",
        requiredCount = null
    )
}