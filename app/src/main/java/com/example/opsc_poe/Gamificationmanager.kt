package com.example.opsc_poe

import android.content.Context
import android.content.SharedPreferences
import android.util.Log // Imported Android Log utility
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages game-design elements for the platform, including persistence calculations
 * for continuous login streaks, expense milestone accumulators, and trophy assets allocation.
 * Values are managed via private [SharedPreferences] instances.
 */
object GamificationManager {

    private const val TAG = "GamificationManager"

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
     * Evaluates a user login sequence to determine daily continuity.
     * Computes consecutive logins, persists results, and evaluates streak-based milestone rewards.
     * * @return The updated streak integer count.
     */
    fun recordLogin(ctx: Context): Int {
        Log.d(TAG, "recordLogin: Initializing login tracking validation.")
        val p       = prefs(ctx)
        val today   = dateFormat.format(Date())
        val last    = p.getString(KEY_LAST_LOGIN, "") ?: ""
        val streak  = p.getInt(KEY_STREAK, 0)

        val newStreak = when {
            last == today -> {
                Log.d(TAG, "recordLogin: Duplicate login detected for today ($today). Maintaining current streak: $streak")
                streak
            }
            last == yesterday() -> {
                val incrementalStreak = streak + 1
                Log.d(TAG, "recordLogin: Consecutive day verified. Incrementing streak: $streak -> $incrementalStreak")
                incrementalStreak
            }
            else -> {
                Log.d(TAG, "recordLogin: Missing sequence link or broken consecutive loop. Last recorded login: '$last'. Resetting streak count to 1.")
                1
            }
        }

        p.edit()
            .putString(KEY_LAST_LOGIN, today)
            .putInt(KEY_STREAK, newStreak)
            .apply()

        // Evaluate login streak badge assignments explicitly
        checkStreakBadges(ctx, newStreak)

        return newStreak
    }

    fun getStreak(ctx: Context): Int = prefs(ctx).getInt(KEY_STREAK, 0)

    /**
     * Helper calculation generating an ISO tracking string corresponding to the previous day.
     */
    private fun yesterday(): String {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.DATE, -1)
        return dateFormat.format(cal.time)
    }

    /**
     * Validates active streaks against explicit minimum values to unlock achievement medals.
     */
    private fun checkStreakBadges(ctx: Context, streakCount: Int) {
        val existing = getUnlockedBadgeIds(ctx)
        Log.d(TAG, "checkStreakBadges: Checking streak eligibility for count ($streakCount). Active allocations: $existing")

        if (streakCount >= 3 && !existing.contains(Badge.STREAK_3.id)) {
            Log.d(TAG, "checkStreakBadges: Threshold met. Unlocking Badge: ${Badge.STREAK_3.title}")
            unlockBadge(ctx, Badge.STREAK_3)
        }
        if (streakCount >= 7 && !existing.contains(Badge.STREAK_7.id)) {
            Log.d(TAG, "checkStreakBadges: Threshold met. Unlocking Badge: ${Badge.STREAK_7.title}")
            unlockBadge(ctx, Badge.STREAK_7)
        }
    }

    // ── Expense milestones ────────────────────────────────────────

    /**
     * Updates structural database entry count parameters whenever transactional components save.
     * Evaluates volume thresholds against global definitions to identify new unlocks.
     * * @return A List containing newly awarded [Badge] objects, empty if none were earned.
     */
    fun recordExpense(ctx: Context): List<Badge> {
        val p     = prefs(ctx)
        val count = p.getInt(KEY_TOTAL_EXP, 0) + 1

        Log.d(TAG, "recordExpense: Transaction logged. Incremented aggregate operational totals to: $count")
        p.edit().putInt(KEY_TOTAL_EXP, count).apply()

        val existing = getUnlockedBadgeIds(ctx)
        val newBadges = mutableListOf<Badge>()

        // Check each available enum value against newly calculated counts
        Badge.values().forEach { badge ->
            if (badge.requiredCount != null && count >= badge.requiredCount && !existing.contains(badge.id)) {
                Log.d(TAG, "recordExpense: Milestone verified for badge '${badge.title}'. Appending to output award lists.")
                unlockBadge(ctx, badge)
                newBadges.add(badge)
            }
        }
        return newBadges
    }

    fun getExpenseCount(ctx: Context): Int = prefs(ctx).getInt(KEY_TOTAL_EXP, 0)

    // ── Badges ────────────────────────────────────────────────────

    /**
     * Appends a targeted badge unique string token identifier into your persistent shared tracking strings.
     */
    fun unlockBadge(ctx: Context, badge: Badge) {
        val current = getUnlockedBadgeIds(ctx).toMutableSet()
        if (current.add(badge.id)) {
            val updatedStringRepresentation = current.joinToString(",")
            Log.d(TAG, "unlockBadge: Persisting updated CSV dataset map: [$updatedStringRepresentation]")
            prefs(ctx).edit().putString(KEY_BADGES, updatedStringRepresentation).apply()
        } else {
            Log.e(TAG, "unlockBadge warning: Attempted to add duplicate target reference token into system file records: '${badge.id}'")
        }
    }

    /**
     * Parsed verification wrapper returning clear string ID tokens extracted from preference strings.
     */
    fun getUnlockedBadgeIds(ctx: Context): Set<String> {
        val raw = prefs(ctx).getString(KEY_BADGES, "") ?: ""
        return if (raw.isEmpty()) {
            emptySet()
        } else {
            raw.split(",").toSet()
        }
    }

    /**
     * Aggregates active badge ID string tokens and maps them to concrete enum types.
     */
    fun getUnlockedBadges(ctx: Context): List<Badge> {
        val ids = getUnlockedBadgeIds(ctx)
        val filteredList = Badge.values().filter { ids.contains(it.id) }
        Log.d(TAG, "getUnlockedBadges: Resolved ${filteredList.size} matching system awards.")
        return filteredList
    }
}

/**
 * All available badges in the app.
 * [requiredCount] indicates the expense total count required to trigger.
 * Storing a value of null specifies manually handled procedural flags (e.g., calendar tracking hooks).
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