package com.neocalc.app.ui.util

import uniffi.neocalc_backend.NumberLocale
import uniffi.neocalc_backend.formatForDisplay as rustFormatForDisplay
import java.util.Locale

/**
 * Utility for formatting numbers for display with locale-aware separators.
 * Delegates to Rust backend for consistent formatting.
 */
object NumberFormatter {

    /**
     * Get the appropriate NumberLocale based on the current system locale.
     */
    private fun getNumberLocale(): NumberLocale {
        val locale = Locale.getDefault()
        return when (locale.language) {
            "pt", "es", "de", "fr", "it" -> NumberLocale.PT_BR // European/Latin format
            else -> NumberLocale.EN_US // US/UK format
        }
    }

    /**
     * Format a string as a number with thousand separators if it's a valid number.
     * Uses the system locale to determine separator style.
     * Returns the original string if parsing fails.
     */
    fun format(input: String): String {
        return rustFormatForDisplay(input, getNumberLocale())
    }

    /**
     * Format with explicit locale override.
     */
    fun format(input: String, locale: NumberLocale): String {
        return rustFormatForDisplay(input, locale)
    }
}
