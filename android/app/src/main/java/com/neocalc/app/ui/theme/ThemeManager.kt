package com.neocalc.app.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.neocalc.app.core.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

data class ThemeInfo(
    val name: String,
    val colorScheme: ColorScheme,
    /** True when the theme's background is dark (drives status-bar icons etc.). */
    val isDark: Boolean
)

/** Small color sample of a theme, used for picker previews. */
data class ThemeSwatch(
    val background: Color,
    val card: Color,
    val accent: Color,
    val destructive: Color
)

object ThemeManager {
    private const val DEFAULT_THEME = "material"

    private val _currentTheme = MutableStateFlow<ThemeInfo?>(null)
    val currentTheme: StateFlow<ThemeInfo?> = _currentTheme.asStateFlow()

    private val _availableThemes = MutableStateFlow<List<String>>(emptyList())
    val availableThemes: StateFlow<List<String>> = _availableThemes.asStateFlow()

    private val _swatches = MutableStateFlow<Map<String, ThemeSwatch>>(emptyMap())
    val swatches: StateFlow<Map<String, ThemeSwatch>> = _swatches.asStateFlow()

    /** True when the user selected Material You dynamic color instead of a CSS theme. */
    private val _dynamicSelected = MutableStateFlow(false)
    val dynamicSelected: StateFlow<Boolean> = _dynamicSelected.asStateFlow()

    private val _darkMode = MutableStateFlow(AppPreferences.DarkMode.SYSTEM)
    val darkMode: StateFlow<AppPreferences.DarkMode> = _darkMode.asStateFlow()

    suspend fun initialize(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                _darkMode.value = AppPreferences.getDarkMode(context)

                val assetThemes = context.assets.list("themes")
                    ?.filter { it.endsWith(".css") }
                    ?.map { it.removeSuffix(".css") } ?: emptyList()

                // Allow user to import themes to filesDir/themes
                val localThemesDir = java.io.File(context.filesDir, "themes")
                if (!localThemesDir.exists()) localThemesDir.mkdirs()

                val localThemes = localThemesDir.listFiles()
                    ?.filter { it.name.endsWith(".css") }
                    ?.map { it.name.removeSuffix(".css") } ?: emptyList()

                val allThemes = (assetThemes + localThemes).distinct().sorted()
                _availableThemes.value = allThemes
                _swatches.value = allThemes.mapNotNull { name ->
                    parseThemeColors(context, name)?.let { name to swatchOf(it) }
                }.toMap()

                val saved = AppPreferences.getThemeName(context)
                when {
                    saved == AppPreferences.THEME_DYNAMIC -> {
                        _dynamicSelected.value = true
                        _currentTheme.value = null
                    }
                    saved != null && saved in allThemes -> loadTheme(context, saved)
                    else -> {
                        // First launch or vanished theme: fall back without persisting,
                        // so a later OS/dynamic default change can still win.
                        val fallback = listOf(DEFAULT_THEME, "dracula").firstOrNull { it in allThemes }
                            ?: allThemes.firstOrNull()
                        fallback?.let { loadTheme(context, it) }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** User-initiated theme selection: applies and persists. */
    suspend fun selectTheme(context: Context, themeName: String) {
        AppPreferences.setThemeName(context, themeName)
        _dynamicSelected.value = false
        loadTheme(context, themeName)
    }

    /** User-initiated switch to Material You dynamic color (Android 12+). */
    fun selectDynamic(context: Context) {
        AppPreferences.setThemeName(context, AppPreferences.THEME_DYNAMIC)
        _dynamicSelected.value = true
        _currentTheme.value = null
    }

    fun setDarkMode(context: Context, mode: AppPreferences.DarkMode) {
        AppPreferences.setDarkMode(context, mode)
        _darkMode.value = mode
    }

    /**
     * Name of the light/dark counterpart of [name] following the `foo`/`foo-light`
     * convention, or null when the theme has no counterpart for [wantDark].
     */
    fun pairedName(name: String, wantDark: Boolean): String? {
        val base = name.removeSuffix("-light")
        val target = if (wantDark) base else "$base-light"
        return target.takeIf { it != name && it in _availableThemes.value }
    }

    /**
     * Follow the system/user dark-mode preference by swapping to the theme's
     * counterpart when one exists (e.g. material <-> material-light).
     * Not persisted: the user's explicit selection stays canonical.
     */
    suspend fun applyDarkPreference(context: Context, wantDark: Boolean) {
        val current = _currentTheme.value ?: return
        if (current.isDark == wantDark) return
        pairedName(current.name, wantDark)?.let { loadTheme(context, it) }
    }

    /** Load a theme without persisting the choice. */
    suspend fun loadTheme(context: Context, themeName: String) {
        withContext(Dispatchers.IO) {
            try {
                val parsedColors = parseThemeColors(context, themeName) ?: return@withContext
                val scheme = mapToColorScheme(parsedColors)
                val isDark = !ColorDerivation.isLight(argb(scheme.background))
                _dynamicSelected.value = false
                _currentTheme.value = ThemeInfo(themeName, scheme, isDark)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun importTheme(context: Context, uri: android.net.Uri) {
        withContext(Dispatchers.IO) {
            try {
                val fileName = getFileName(context, uri) ?: "imported_theme.css"
                val safeName = if (fileName.endsWith(".css")) fileName else "$fileName.css"

                val themesDir = java.io.File(context.filesDir, "themes")
                if (!themesDir.exists()) themesDir.mkdirs()

                val destFile = java.io.File(themesDir, safeName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    java.io.FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // Reload list, then apply and persist the imported theme
                initialize(context)
                selectTheme(context, safeName.removeSuffix(".css"))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun parseThemeColors(context: Context, themeName: String): Map<String, Long>? {
        return try {
            // Local user override first, then bundled asset
            val localFile = java.io.File(context.filesDir, "themes/$themeName.css")
            val cssContent = if (localFile.exists()) {
                localFile.readText()
            } else {
                context.assets.open("themes/$themeName.css").use { stream ->
                    BufferedReader(InputStreamReader(stream)).readText()
                }
            }
            uniffi.neocalc_backend.parseThemeCss(cssContent)
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileName(context: Context, uri: android.net.Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

    private fun argb(color: Color): Long = color.value.toLong() ushr 32

    private fun swatchOf(colors: Map<String, Long>): ThemeSwatch {
        val bg = colors["window_bg_color"] ?: 0xFF1C1B1FL
        val card = colors["card_bg_color"] ?: colors["headerbar_bg_color"] ?: bg
        val accent = colors["accent_bg_color"] ?: 0xFFD0BCFFL
        val destructive = colors["destructive_bg_color"] ?: 0xFFB3261EL
        return ThemeSwatch(Color(bg), Color(card), Color(accent), Color(destructive))
    }

    /**
     * Derive a full Material tonal scheme from the handful of GTK
     * `@define-color` keys a theme provides. The baseline (light or dark) is
     * chosen from the window background's luminance, so light CSS themes get
     * light defaults for every unmapped role.
     */
    internal fun mapToColorScheme(colors: Map<String, Long>): ColorScheme {
        val d = ColorDerivation

        val windowBgL = colors["window_bg_color"] ?: 0xFF1C1B1FL
        val dark = !d.isLight(windowBgL)
        val base = if (dark) darkColorScheme() else lightColorScheme()

        val windowFgL = colors["window_fg_color"] ?: d.onColorFor(windowBgL)
        val accentL = colors["accent_bg_color"] ?: argb(base.primary)
        val accentFgL = colors["accent_fg_color"] ?: d.onColorFor(accentL)
        val headerBgL = colors["headerbar_bg_color"] ?: d.surfaceTier(windowBgL, 0.08f)
        val headerFgL = colors["headerbar_fg_color"] ?: d.onColorFor(headerBgL)
        val cardBgL = colors["card_bg_color"] ?: headerBgL
        val cardFgL = colors["card_fg_color"] ?: d.onColorFor(cardBgL)
        val destructiveBgL = colors["destructive_bg_color"] ?: argb(base.error)
        val destructiveFgL = colors["destructive_fg_color"] ?: d.onColorFor(destructiveBgL)
        // Warning maps to the tertiary role; derive a muted accent when absent.
        val warningBgL = colors["warning_bg_color"] ?: d.blend(accentL, cardBgL, 0.5f)
        val warningFgL = colors["warning_fg_color"] ?: d.onColorFor(warningBgL)

        // Containers: accent softened toward the background instead of an alpha hack.
        val primaryContainerL = d.blend(accentL, windowBgL, 0.6f)
        val outlineL = d.blend(windowFgL, windowBgL, 0.5f)
        val outlineVariantL = d.blend(windowFgL, windowBgL, 0.75f)

        fun tier(t: Float) = Color(d.surfaceTier(windowBgL, t))

        return base.copy(
            primary = Color(accentL),
            onPrimary = Color(accentFgL),
            primaryContainer = Color(primaryContainerL),
            onPrimaryContainer = Color(d.onColorFor(primaryContainerL)),

            secondary = Color(cardBgL),
            onSecondary = Color(cardFgL),
            secondaryContainer = Color(cardBgL),
            onSecondaryContainer = Color(cardFgL),

            tertiary = Color(warningBgL),
            onTertiary = Color(warningFgL),
            tertiaryContainer = Color(warningBgL),
            onTertiaryContainer = Color(warningFgL),

            background = Color(windowBgL),
            onBackground = Color(windowFgL),
            surface = Color(windowBgL),
            onSurface = Color(windowFgL),
            surfaceVariant = Color(headerBgL),
            onSurfaceVariant = Color(headerFgL),

            surfaceContainerLowest = tier(0.02f),
            surfaceContainerLow = tier(0.04f),
            surfaceContainer = tier(0.06f),
            surfaceContainerHigh = tier(0.09f),
            surfaceContainerHighest = tier(0.12f),

            outline = Color(outlineL),
            outlineVariant = Color(outlineVariantL),

            inverseSurface = Color(windowFgL),
            inverseOnSurface = Color(windowBgL),

            error = Color(destructiveBgL),
            onError = Color(destructiveFgL),
            errorContainer = Color(destructiveBgL),
            onErrorContainer = Color(destructiveFgL)
        )
    }
}
