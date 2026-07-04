package com.neocalc.app.ui.style

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.neocalc.app.core.AppPreferences
import com.neocalc.app.ui.theme.ThemeManager

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onPrimary = Color(0xFF381E72),
    onSecondary = Color(0xFF332D41),
    onTertiary = Color(0xFF492532),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6650a4),
    secondary = Color(0xFF625b71),
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun NeoCalcTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val currentThemeInfo by ThemeManager.currentTheme.collectAsState()
    val dynamicSelected by ThemeManager.dynamicSelected.collectAsState()
    val darkModePref by ThemeManager.darkMode.collectAsState()

    val darkTheme = when (darkModePref) {
        AppPreferences.DarkMode.LIGHT -> false
        AppPreferences.DarkMode.DARK -> true
        AppPreferences.DarkMode.SYSTEM -> isSystemInDarkTheme()
    }

    // Follow the dark-mode preference for themes with a light/dark counterpart
    // (e.g. material <-> material-light, catppuccin-mocha stays as-is).
    LaunchedEffect(darkTheme, currentThemeInfo?.name) {
        ThemeManager.applyDarkPreference(context, darkTheme)
    }

    val colorScheme = when {
        // 1. Custom CSS theme selected
        currentThemeInfo != null -> currentThemeInfo!!.colorScheme

        // 2. Material You dynamic color, when selected and available
        dynamicSelected && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        // 3. Fallback (also covers "dynamic" chosen on pre-Android-12 devices)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status-bar icon contrast follows the actual rendered background,
            // which for CSS themes may not match the dark-mode preference.
            val lightBackground = colorScheme.background.luminance() > 0.5f
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                lightBackground
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
