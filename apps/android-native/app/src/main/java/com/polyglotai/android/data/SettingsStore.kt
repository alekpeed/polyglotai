package com.polyglotai.android.data

import android.content.Context

/** How the app picks light vs dark. SYSTEM follows the device setting. */
enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

/** Local, device-scoped preferences: the daily review goal and the theme choice. */
class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("polyglotai_settings", Context.MODE_PRIVATE)

    var dailyGoal: Int
        get() = prefs.getInt(KEY_GOAL, DEFAULT_GOAL).coerceIn(MIN_GOAL, MAX_GOAL)
        set(value) = prefs.edit().putInt(KEY_GOAL, value.coerceIn(MIN_GOAL, MAX_GOAL)).apply()

    var themeMode: ThemeMode
        get() = runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!) }
            .getOrDefault(ThemeMode.SYSTEM)
        set(value) = prefs.edit().putString(KEY_THEME, value.name).apply()

    companion object {
        const val DEFAULT_GOAL = 20
        const val MIN_GOAL = 5
        const val MAX_GOAL = 100
        private const val KEY_GOAL = "daily_goal"
        private const val KEY_THEME = "theme_mode"
    }
}
