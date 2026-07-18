package com.polyglotai.android.data

import android.content.Context
import com.polyglotai.android.ui.theme.AppTheme
import com.polyglotai.android.ui.theme.Pack
import com.polyglotai.android.ui.theme.PackVariant
import com.polyglotai.android.ui.theme.variantsForPack

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

    /** The chosen palette variant for a pack world, or that world's default (first option) when the
     *  learner hasn't picked one. Worlds with a single look always resolve to DEFAULT. Stored per
     *  world so, e.g., a Russian choice survives switching languages and back. */
    fun variantFor(pack: Pack): PackVariant {
        val options = variantsForPack(pack)
        if (options.isEmpty()) return PackVariant.DEFAULT
        val stored = prefs.getString(KEY_VARIANT_PREFIX + pack.name, null)
        return options.firstOrNull { it.variant.name == stored }?.variant ?: options.first().variant
    }

    fun setVariant(pack: Pack, variant: PackVariant) {
        prefs.edit().putString(KEY_VARIANT_PREFIX + pack.name, variant.name).apply()
    }

    companion object {
        const val DEFAULT_GOAL = 20
        const val MIN_GOAL = 5
        const val MAX_GOAL = 100
        private const val KEY_GOAL = "daily_goal"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_ONBOARDED = "onboarded"
        private const val KEY_VARIANT_PREFIX = "pack_variant_"
    }
}
