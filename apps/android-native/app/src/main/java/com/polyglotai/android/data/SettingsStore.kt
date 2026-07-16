package com.polyglotai.android.data

import android.content.Context
import com.polyglotai.android.ui.theme.AppTheme

/** Local, device-scoped preferences: the daily review goal and the appearance choice. */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("polyglotai_settings", Context.MODE_PRIVATE)

    var dailyGoal: Int
        get() = prefs.getInt(KEY_GOAL, DEFAULT_GOAL).coerceIn(MIN_GOAL, MAX_GOAL)
        set(value) = prefs.edit().putInt(KEY_GOAL, value.coerceIn(MIN_GOAL, MAX_GOAL)).apply()

    var appTheme: AppTheme
        get() = runCatching { AppTheme.valueOf(prefs.getString(KEY_THEME, AppTheme.SYSTEM.name)!!) }
            .getOrDefault(AppTheme.SYSTEM)
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    /** False until the learner has seen the one-time welcome. */
    var onboarded: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    companion object {
        const val DEFAULT_GOAL = 20
        const val MIN_GOAL = 5
        const val MAX_GOAL = 100
        private const val KEY_GOAL = "daily_goal"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_ONBOARDED = "onboarded"
    }
}
