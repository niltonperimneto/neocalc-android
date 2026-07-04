package com.neocalc.app.core

import android.content.Context
import android.content.SharedPreferences

/**
 * Lightweight SharedPreferences-backed settings store.
 *
 * Holds UI-side preferences that the Rust core either cannot expose (there is
 * no getter for show_fractions) or does not model (per-session calculator mode).
 */
object AppPreferences {
    private const val PREFS_NAME = "neocalc_prefs"
    private const val KEY_SHOW_FRACTIONS = "show_fractions"
    private const val KEY_SESSION_MODE_PREFIX = "session_mode_"
    private const val KEY_THEME_NAME = "theme_name"
    private const val KEY_DARK_MODE = "dark_mode"

    /** Sentinel theme name selecting Material You dynamic color (Android 12+). */
    const val THEME_DYNAMIC = "dynamic"

    enum class DarkMode { SYSTEM, LIGHT, DARK }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Selected theme name, [THEME_DYNAMIC], or null (never chosen — use default). */
    fun getThemeName(context: Context): String? =
        prefs(context).getString(KEY_THEME_NAME, null)

    fun setThemeName(context: Context, name: String) {
        prefs(context).edit().putString(KEY_THEME_NAME, name).apply()
    }

    fun getDarkMode(context: Context): DarkMode {
        val stored = prefs(context).getString(KEY_DARK_MODE, null)
        return stored?.let { s -> DarkMode.entries.find { it.name == s } } ?: DarkMode.SYSTEM
    }

    fun setDarkMode(context: Context, mode: DarkMode) {
        prefs(context).edit().putString(KEY_DARK_MODE, mode.name).apply()
    }

    fun getShowFractions(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_FRACTIONS, false)

    fun setShowFractions(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_FRACTIONS, value).apply()
    }

    fun getSessionMode(context: Context, sessionId: String): CalculatorMode {
        val name = prefs(context).getString(KEY_SESSION_MODE_PREFIX + sessionId, null)
        return name?.let { stored -> CalculatorMode.entries.find { it.name == stored } }
            ?: CalculatorMode.STANDARD
    }

    fun setSessionMode(context: Context, sessionId: String, mode: CalculatorMode) {
        prefs(context).edit().putString(KEY_SESSION_MODE_PREFIX + sessionId, mode.name).apply()
    }

    fun clearSessionMode(context: Context, sessionId: String) {
        prefs(context).edit().remove(KEY_SESSION_MODE_PREFIX + sessionId).apply()
    }
}
