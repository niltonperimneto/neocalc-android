package com.neocalc.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ColorDerivationTest {

    @Test
    fun luminanceExtremes() {
        assertEquals(0.0, ColorDerivation.luminance(ColorDerivation.BLACK), 1e-9)
        assertEquals(1.0, ColorDerivation.luminance(ColorDerivation.WHITE), 1e-9)
        assertTrue(ColorDerivation.isLight(ColorDerivation.WHITE))
        assertTrue(!ColorDerivation.isLight(ColorDerivation.BLACK))
    }

    @Test
    fun contrastRatioBounds() {
        assertEquals(21.0, ColorDerivation.contrastRatio(ColorDerivation.BLACK, ColorDerivation.WHITE), 1e-6)
        assertEquals(
            ColorDerivation.contrastRatio(0xFF336699L, 0xFFFFFFFFL),
            ColorDerivation.contrastRatio(0xFFFFFFFFL, 0xFF336699L),
            1e-9
        )
        assertEquals(1.0, ColorDerivation.contrastRatio(0xFF808080L, 0xFF808080L), 1e-9)
    }

    @Test
    fun blendEndpointsAndOpacity() {
        val a = 0xFF112233L
        val b = 0xFFEEDDCCL
        assertEquals(a, ColorDerivation.blend(a, b, 0f))
        assertEquals(b, ColorDerivation.blend(a, b, 1f))
        // Alpha always forced opaque
        assertEquals(0xFF000000L, ColorDerivation.blend(0x00000000L, 0x00000000L, 0.5f) and 0xFF000000L)
    }

    @Test
    fun surfaceTiersMoveAwayFromBackground() {
        val darkBg = 0xFF1C1B1FL
        val lightBg = 0xFFFAFAFAL
        assertTrue(
            ColorDerivation.luminance(ColorDerivation.surfaceTier(darkBg, 0.1f)) >
                ColorDerivation.luminance(darkBg)
        )
        assertTrue(
            ColorDerivation.luminance(ColorDerivation.surfaceTier(lightBg, 0.1f)) <
                ColorDerivation.luminance(lightBg)
        )
    }

    @Test
    fun onColorForAlwaysReadable() {
        // Any background: black-or-white pick must reach at least 4.5:1
        // (the worst case, mid-gray, still yields ~4.6:1 against black).
        for (r in 0..255 step 17) {
            for (g in 0..255 step 17) {
                for (b in 0..255 step 17) {
                    val c = 0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
                    val on = ColorDerivation.onColorFor(c)
                    assertTrue(
                        "on-color for ${c.toString(16)} under 4.5:1",
                        ColorDerivation.contrastRatio(c, on) >= 4.5
                    )
                }
            }
        }
    }

    /**
     * Every bundled CSS theme must produce readable derived on-colors for its
     * key surfaces. Reads the shipped assets directly from the module tree.
     */
    @Test
    fun bundledThemesDeriveReadableOnColors() {
        val themesDir = File("src/main/assets/themes")
        assertTrue("themes dir missing: ${themesDir.absolutePath}", themesDir.isDirectory)
        val cssFiles = themesDir.listFiles { f -> f.name.endsWith(".css") }!!
        assertTrue("no bundled themes found", cssFiles.isNotEmpty())

        val defineColor = Regex("""@define-color\s+(\w+)\s+#([0-9a-fA-F]{6,8})\s*;""")
        for (css in cssFiles) {
            val colors = defineColor.findAll(css.readText()).associate { match ->
                val hex = match.groupValues[2]
                val argb = if (hex.length == 6) 0xFF000000L or hex.toLong(16) else hex.toLong(16)
                match.groupValues[1] to argb
            }
            for (key in listOf("window_bg_color", "card_bg_color", "accent_bg_color")) {
                val bg = colors[key] ?: continue
                val on = ColorDerivation.onColorFor(bg)
                assertTrue(
                    "${css.name}: derived on-color for $key below 4.5:1",
                    ColorDerivation.contrastRatio(bg, on) >= 4.5
                )
            }
        }
    }
}
