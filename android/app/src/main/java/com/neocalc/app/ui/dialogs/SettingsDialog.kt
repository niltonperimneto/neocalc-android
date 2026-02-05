package com.neocalc.app.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neocalc.app.R
import com.neocalc.app.ui.style.Spacing
import com.neocalc.app.ui.theme.ThemeManager
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun SettingsDialog(
    showFractions: Boolean,
    onToggleFractions: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val availableThemes by ThemeManager.availableThemes.collectAsState()
    val currentTheme by ThemeManager.currentTheme.collectAsState()
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                ThemeManager.importTheme(context, it)
            }
        }
    }

    var showTutorial by remember { mutableStateOf(false) }
    
    if (showTutorial) {
        TutorialDialog(onDismiss = { showTutorial = false })
    }

    AlertDialog(
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            LazyColumn {
                // Section: General
                item {
                    Text(
                        text = stringResource(R.string.settings_section_general),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleFractions(!showFractions) }
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_show_fractions),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = showFractions,
                            onCheckedChange = onToggleFractions
                        )
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.md))
                }

                // Section: Appearance
                item {
                    Text(
                        text = stringResource(R.string.settings_section_appearance),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    )
                }

                items(availableThemes) { themeName ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch {
                                    ThemeManager.loadTheme(context, themeName)
                                }
                            }
                            .padding(vertical = Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme?.name == themeName,
                            onClick = {
                                scope.launch {
                                    ThemeManager.loadTheme(context, themeName)
                                }
                            }
                        )
                        Text(
                            text = themeName.replace("-", " ").replaceFirstChar { 
                                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() 
                            },
                            modifier = Modifier.padding(start = Spacing.sm)
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }

                // Import Theme Button
                item {
                    TextButton(
                        onClick = { launcher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.padding(end = Spacing.xs).size(18.dp)
                        )
                        Text(stringResource(R.string.settings_import_theme))
                    }
                }
                
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.md))
                }
                
                // Section: Help
                item {
                    Text(
                        text = stringResource(R.string.settings_section_help),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    )
                }
                
                item {
                    TextButton(
                        onClick = { showTutorial = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = null,
                            modifier = Modifier.padding(end = Spacing.xs).size(18.dp)
                        )
                        Text(stringResource(R.string.settings_help_tutorial))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_close))
            }
        }
    )
}
