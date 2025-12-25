package com.neocalc.app.ui.windows

import androidx.compose.runtime.Composable
import com.neocalc.app.ui.widgets.Calculator

@Composable
fun MainWindow() {
    // This wrapper represents the main application window logic
    // In GTK this handled window lifecycle, here it wraps the main Widget.
    Calculator()
}
