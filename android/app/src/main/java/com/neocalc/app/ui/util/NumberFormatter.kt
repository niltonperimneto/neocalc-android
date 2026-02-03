package com.neocalc.app.ui.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Utility for formatting numbers for display with locale-aware separators.
 */
object NumberFormatter {
    private val decimalFormat = DecimalFormat("#,###.########", DecimalFormatSymbols(Locale.US))

    /**
     * Format a string as a number with thousand separators if it's a valid number.
     * Returns the original string if parsing fails.
     */
    fun format(input: String): String {
        // Don't format if it looks like an expression (contains operators)
        if (input.contains(Regex("[+\\-*/^()=]")) || input.contains(" ")) {
            return input
        }
        
        return try {
            val number = input.toDoubleOrNull()
            when {
                number == null -> input
                number.isNaN() || number.isInfinite() -> input
                number == number.toLong().toDouble() -> {
                    // It's a whole number
                    DecimalFormat("#,###").format(number.toLong())
                }
                else -> decimalFormat.format(number)
            }
        } catch (e: Exception) {
            input
        }
    }
}
