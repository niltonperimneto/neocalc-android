package com.neocalc.app.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
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
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
        textContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
        onDismissRequest = onDismiss,
        title = { Text("Select Theme") },
        text = {
            LazyColumn {
                items(availableThemes) { themeName ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    ThemeManager.loadTheme(context, themeName)
                                }
                                onDismiss()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme?.name == themeName,
                            onClick = {
                                scope.launch {
                                    ThemeManager.loadTheme(context, themeName)
                                }
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
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                 // Import Button
                 val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
                     contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                 ) { uri: android.net.Uri? ->
                     uri?.let {
                         scope.launch {
                             ThemeManager.importTheme(context, it)
                         }
                     }
                 }
                 
                 androidx.compose.material3.TextButton(onClick = { 
                     launcher.launch("*/*") // Allow picking css or any file (mime types can be tricky)
                 }) {
                     androidx.compose.material3.Icon(
                         imageVector = androidx.compose.material.icons.Icons.Default.Add,
                         contentDescription = null,
                         modifier = Modifier.padding(end = 4.dp).size(16.dp)
                     )
                     Text("Import")
                 }

                 androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))

                 TextButton(onClick = onDismiss) {
                     Text("Close")
                 }
            }
        }
    )
}
