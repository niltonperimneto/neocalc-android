package com.neocalc.app.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader

data class ThemeInfo(
    val name: String,
    val colorScheme: ColorScheme
)

object ThemeManager {
    private val _currentTheme = MutableStateFlow<ThemeInfo?>(null)
    val currentTheme: StateFlow<ThemeInfo?> = _currentTheme.asStateFlow()

    private val _availableThemes = MutableStateFlow<List<String>>(emptyList())
    val availableThemes: StateFlow<List<String>> = _availableThemes.asStateFlow()

    // Default Fallback (Material)
    private val DefaultScheme = darkColorScheme(
        primary = Color(0xFFD0BCFF),
        secondary = Color(0xFFCCC2DC),
        tertiary = Color(0xFFEFB8C8)
    )

    fun initialize(context: Context) {
        val assetManager = context.assets
        try {
            val themes = assetManager.list("themes")
                ?.filter { it.endsWith(".css") }
                ?.map { it.removeSuffix(".css") }
                ?.sorted() ?: emptyList()
            
            _availableThemes.value = themes
            
            // Load default theme if exists, else first
            if (themes.contains("dracula")) {
                loadTheme(context, "dracula")
            } else if (themes.isNotEmpty()) {
                loadTheme(context, themes.first())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadTheme(context: Context, themeName: String) {
        try {
            val cssContent = context.assets.open("themes/$themeName.css").use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }
            val parsedColors = parseCssColors(cssContent)
            val scheme = mapToColorScheme(parsedColors)
            _currentTheme.value = ThemeInfo(themeName, scheme)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun parseCssColors(css: String): Map<String, Long> {
        val colorMap = mutableMapOf<String, Long>()
        // Regex to find @define-color name #RRGGBB; or #RGB;
        // Simple parser assuming hex codes
        val regex = Regex("@define-color\\s+([\\w_]+)\\s+(#[A-Fa-f0-9]{3,8});")
        regex.findAll(css).forEach { match ->
            val name = match.groupValues[1]
            val hex = match.groupValues[2]
            colorMap[name] = parseHexColor(hex)
        }
        return colorMap
    }

    private fun parseHexColor(hex: String): Long {
        var cleanHex = hex.removePrefix("#")
        if (cleanHex.length == 3) {
            cleanHex = cleanHex.map { "$it$it" }.joinToString("")
        }
        if (cleanHex.length == 6) {
            cleanHex = "FF$cleanHex" // Add Alpha
        }
        return try {
            cleanHex.toLong(16)
        } catch (e: Exception) {
            0xFF000000
        }
    }

    private fun mapToColorScheme(colors: Map<String, Long>): ColorScheme {
        // Default to dark scheme baseline
        val base = darkColorScheme()

        // Extract GTK colors
        // Note: GTK themes use window_bg_color, accent_bg_color etc.
        
        val windowBg = colors["window_bg_color"]?.let { Color(it) } ?: base.background
        val windowFg = colors["window_fg_color"]?.let { Color(it) } ?: base.onBackground
        
        val headerBg = colors["headerbar_bg_color"]?.let { Color(it) } ?: base.surface
        val headerFg = colors["headerbar_fg_color"]?.let { Color(it) } ?: base.onSurface
        
        val accentBg = colors["accent_bg_color"]?.let { Color(it) } ?: base.primary
        val accentFg = colors["accent_fg_color"]?.let { Color(it) } ?: base.onPrimary
        
        val destructiveBg = colors["destructive_bg_color"]?.let { Color(it) } ?: base.error
        val destructiveFg = colors["destructive_fg_color"]?.let { Color(it) } ?: base.onError

        // Secondary Accent / Warning Color matching
        // Many themes use warning_bg_color (Orange) or success_bg_color (Green)
        // We will try to map Operators to 'warning_bg_color' (Orange-ish) which creates a secondary accent.
        val warningBg = colors["warning_bg_color"]?.let { Color(it) } 
            ?: colors["warning_color"]?.let { Color(it) }
            ?: accentBg // Fallback if no warning color

        // If warningBg is the same as accentBg, maybe we can shift it or use inverse? 
        // But for now, let's assume themes have it or we fallback to accent.
        
        val warningFg = colors["warning_fg_color"]?.let { Color(it) } 
             ?: base.onTertiaryContainer
        
        // Card / Button colors
        val cardBg = colors["card_bg_color"]?.let { Color(it) } ?: headerBg
        val cardFg = colors["card_fg_color"]?.let { Color(it) } ?: headerFg

        return base.copy(
            primary = accentBg,
            onPrimary = accentFg,
            primaryContainer = accentBg.copy(alpha = 0.8f),
            onPrimaryContainer = accentFg,
            
            secondary = cardBg, 
            secondaryContainer = cardBg, 
            onSecondaryContainer = cardFg,
            
            tertiary = warningBg,
            tertiaryContainer = warningBg, // Solid color for better visibility
            onTertiaryContainer = warningFg,
            
            background = windowBg,
            onBackground = windowFg,
            surface = windowBg, // Scaffolds often match window
            onSurface = windowFg,
            
            surfaceVariant = headerBg, // Function buttons
            onSurfaceVariant = headerFg,
            
            error = destructiveBg,
            onError = destructiveFg,
            errorContainer = destructiveBg,
            onErrorContainer = destructiveFg
        )
    }
}
