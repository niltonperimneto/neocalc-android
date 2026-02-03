package com.neocalc.app.ui.style

import androidx.compose.ui.unit.dp

/**
 * Standard spacing scale for consistent UI layout.
 * Based on Material Design 8dp grid system.
 */
object Spacing {
    val xs = 4.dp   // Extra small
    val sm = 8.dp   // Small
    val md = 16.dp  // Medium (default)
    val lg = 24.dp  // Large
    val xl = 32.dp  // Extra large
    
    // Specific use cases
    val buttonGap = 2.dp
    val contentPadding = 16.dp
    val sectionGap = 24.dp
}

/**
 * Layout weight constants for consistent proportions.
 */
object LayoutWeights {
    const val displayPortrait = 0.3f
    const val gridPortrait = 0.7f
    const val displayLandscape = 0.4f
    const val gridLandscape = 0.6f
}
