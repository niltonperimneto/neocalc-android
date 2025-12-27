package com.neocalc.app.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.neocalc.app.ui.theme.ThemeManager

@Composable
fun ThemeDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val availableThemes by ThemeManager.availableThemes.collectAsState()
    val currentTheme by ThemeManager.currentTheme.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Theme") },
        text = {
            LazyColumn {
                items(availableThemes) { themeName ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ThemeManager.loadTheme(context, themeName)
                                onDismiss()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme?.name == themeName,
                            onClick = {
                                ThemeManager.loadTheme(context, themeName)
                                onDismiss()
                            }
                        )
                        Text(
                            text = themeName.replace("-", " ").capitalize(),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
