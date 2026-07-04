package com.neocalc.app.ui.theme

import kotlin.math.pow

/**
 * Pure color math on 0xAARRGGBB Longs used to derive a full Material tonal
 * scheme from the handful of colors a GTK CSS theme defines.
 *
 * Deliberately free of androidx types so it can be unit-tested on the JVM.
 */
object ColorDerivation {

    const val WHITE = 0xFFFFFFFFL
    const val BLACK = 0xFF000000L

    private fun red(c: Long): Int = ((c shr 16) and 0xFF).toInt()
    private fun green(c: Long): Int = ((c shr 8) and 0xFF).toInt()
    private fun blue(c: Long): Int = (c and 0xFF).toInt()

    private fun channelLuminance(channel: Int): Double {
        val s = channel / 255.0
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    /** WCAG relative luminance in [0, 1]. */
    fun luminance(c: Long): Double =
        0.2126 * channelLuminance(red(c)) +
            0.7152 * channelLuminance(green(c)) +
            0.0722 * channelLuminance(blue(c))

    fun isLight(c: Long): Boolean = luminance(c) > 0.5

    /** WCAG contrast ratio between two colors, in [1, 21]. */
    fun contrastRatio(a: Long, b: Long): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    /** Per-channel linear interpolation from [a] to [b]; t in [0, 1]. Alpha is forced opaque. */
    fun blend(a: Long, b: Long, t: Float): Long {
        val clamped = t.coerceIn(0f, 1f)
        fun lerp(x: Int, y: Int): Long = (x + ((y - x) * clamped)).toLong().coerceIn(0, 255)
        return 0xFF000000L or
            (lerp(red(a), red(b)) shl 16) or
            (lerp(green(a), green(b)) shl 8) or
            lerp(blue(a), blue(b))
    }

    fun lighten(c: Long, t: Float): Long = blend(c, WHITE, t)

    fun darken(c: Long, t: Float): Long = blend(c, BLACK, t)

    /** Black or white, whichever contrasts more against [bg]. */
    fun onColorFor(bg: Long): Long =
        if (contrastRatio(bg, WHITE) >= contrastRatio(bg, BLACK)) WHITE else BLACK

    /**
     * A slightly elevated/receded surface tier: lightened on dark themes,
     * darkened on light themes, so tiers always move away from the background.
     */
    fun surfaceTier(background: Long, t: Float): Long =
        if (isLight(background)) darken(background, t) else lighten(background, t)
}
