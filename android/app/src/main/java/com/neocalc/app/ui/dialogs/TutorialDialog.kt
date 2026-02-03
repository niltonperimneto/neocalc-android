package com.neocalc.app.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.neocalc.app.ui.style.Spacing

@Composable
fun TutorialDialog(onDismiss: () -> Unit) {
    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        onDismissRequest = onDismiss,
        title = { Text("Help & Tutorial") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Section: Basic Usage
                TutorialSection(
                    title = "Basic Usage",
                    content = "Enter numbers and operators to create expressions. " +
                            "Press '=' to evaluate. Use 'C' to clear and '⌫' to delete."
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                
                // Section: Calculator Modes
                TutorialSection(
                    title = "Calculator Modes",
                    content = "NeoCalc offers 4 modes:\n" +
                            "• Standard: Basic arithmetic\n" +
                            "• Scientific: sin, cos, log, sqrt, etc.\n" +
                            "• Programming: Hex, Binary, Bitwise ops\n" +
                            "• Financial: Interest, loan calculations"
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                
                // Section: Gestures
                TutorialSection(
                    title = "Gestures",
                    content = "• Swipe Left/Right on the keypad to switch between modes.\n" +
                            "• Tap the mode chip at the top to open the mode picker."
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                
                // Section: History
                TutorialSection(
                    title = "History",
                    content = "• Tap a history entry to insert its result into the input.\n" +
                            "• Long-press a history entry to copy it to the clipboard."
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                
                // Section: Sessions
                TutorialSection(
                    title = "Sessions",
                    content = "Manage multiple independent calculator sessions via the drawer (swipe from left edge or tap the drawer icon)."
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
                
                // Section: Themes
                TutorialSection(
                    title = "Themes",
                    content = "Customize the app's appearance in Settings → Appearance. " +
                            "You can import custom CSS-based themes!"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it!")
            }
        }
    )
}

@Composable
private fun TutorialSection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = Spacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = Spacing.xs)
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
