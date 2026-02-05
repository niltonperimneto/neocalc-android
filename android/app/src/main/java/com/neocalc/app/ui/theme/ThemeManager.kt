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

    // Default Fallback
    private val DefaultScheme = darkColorScheme(
        primary = Color(0xFFD0BCFF),
        secondary = Color(0xFFCCC2DC),
        tertiary = Color(0xFFEFB8C8)
    )

    suspend fun initialize(context: Context) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val assetManager = context.assets
            try {
                val assetThemes = assetManager.list("themes")
                    ?.filter { it.endsWith(".css") }
                    ?.map { it.removeSuffix(".css") }
                    ?.sorted() ?: emptyList()

                // Allow user to import themes to filesDir/themes
                val localThemesDir = java.io.File(context.filesDir, "themes")
                if (!localThemesDir.exists()) localThemesDir.mkdirs()

                val localThemes = localThemesDir.listFiles()
                    ?.filter { it.name.endsWith(".css") }
                    ?.map { it.name.removeSuffix(".css") }
                    ?.sorted() ?: emptyList()

                // Merge unique
                val allThemes = (assetThemes + localThemes).distinct().sorted()

                _availableThemes.value = allThemes

                // Load default theme if exists, else first
                // Note: Recursive calls to loadTheme inside withContext are fine since loadTheme is also suspend/withContext
                if (allThemes.contains("material")) {
                    loadTheme(context, "material")
                } else if (allThemes.contains("dracula")) {
                    loadTheme(context, "dracula")
                } else if (allThemes.isNotEmpty()) {
                    loadTheme(context, allThemes.first())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun loadTheme(context: Context, themeName: String) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Try Loading from Local Files first (User overrides)
                val localFile = java.io.File(context.filesDir, "themes/$themeName.css")
                val cssContent = if (localFile.exists()) {
                    localFile.readText()
                } else {
                    // Fallback to Assets
                    context.assets.open("themes/$themeName.css").use { stream ->
                        BufferedReader(InputStreamReader(stream)).readText()
                    }
                }

                val parsedColors = uniffi.neocalc_backend.parseThemeCss(cssContent)
                val scheme = mapToColorScheme(parsedColors)
                _currentTheme.value = ThemeInfo(themeName, scheme)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun importTheme(context: Context, uri: android.net.Uri) {
         kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
             try {
                 val contentResolver = context.contentResolver
                 val fileName = getFileName(context, uri) ?: "imported_theme.css"
                 // Ensure .css extension
                 val safeName = if (fileName.endsWith(".css")) fileName else "$fileName.css"

                 val themesDir = java.io.File(context.filesDir, "themes")
                 if (!themesDir.exists()) themesDir.mkdirs()

                 val destFile = java.io.File(themesDir, safeName)

                 contentResolver.openInputStream(uri)?.use { input ->
                     java.io.FileOutputStream(destFile).use { output ->
                         input.copyTo(output)
                     }
                 }

                 // Reload list
                 initialize(context)

                 // Auto-select imported theme
                 loadTheme(context, safeName.removeSuffix(".css"))

             } catch (e: Exception) {
                 e.printStackTrace()
             }
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

    private fun mapToColorScheme(colors: Map<String, Long>): ColorScheme {
        // Default to dark scheme baseline
        val base = darkColorScheme()

        // Map GTK colors to Material scheme
        
        val windowBg = colors["window_bg_color"]?.let { Color(it) } ?: base.background
        val windowFg = colors["window_fg_color"]?.let { Color(it) } ?: base.onBackground
        
        val headerBg = colors["headerbar_bg_color"]?.let { Color(it) } ?: base.surface
        val headerFg = colors["headerbar_fg_color"]?.let { Color(it) } ?: base.onSurface
        
        val accentBg = colors["accent_bg_color"]?.let { Color(it) } ?: base.primary
        val accentFg = colors["accent_fg_color"]?.let { Color(it) } ?: base.onPrimary
        
        val destructiveBg = colors["destructive_bg_color"]?.let { Color(it) } ?: base.error
        val destructiveFg = colors["destructive_fg_color"]?.let { Color(it) } ?: base.onError

        // Use warning color for Tertiary role
        
        val warningFg = colors["warning_fg_color"]?.let { Color(it) } 
             ?: base.onTertiaryContainer
             
        val warningBg = colors["warning_bg_color"]?.let { Color(it) } 
             ?: base.tertiaryContainer
        
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
