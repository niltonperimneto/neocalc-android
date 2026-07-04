package com.neocalc.app.ui.dialogs

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neocalc.app.R
import com.neocalc.app.core.AppPreferences
import com.neocalc.app.ui.style.Spacing
import com.neocalc.app.ui.theme.ThemeManager
import com.neocalc.app.ui.theme.ThemeSwatch
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    showFractions: Boolean,
    onToggleFractions: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val availableThemes by ThemeManager.availableThemes.collectAsState()
    val currentTheme by ThemeManager.currentTheme.collectAsState()
    val dynamicSelected by ThemeManager.dynamicSelected.collectAsState()
    val swatches by ThemeManager.swatches.collectAsState()
    val darkMode by ThemeManager.darkMode.collectAsState()
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
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            LazyColumn {
                // Section: General
                item {
                    SectionHeader(stringResource(R.string.settings_section_general))
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
                    SectionHeader(stringResource(R.string.settings_section_appearance))
                }

                // Dark mode: System / Light / Dark
                item {
                    val options = listOf(
                        AppPreferences.DarkMode.SYSTEM to stringResource(R.string.settings_dark_system),
                        AppPreferences.DarkMode.LIGHT to stringResource(R.string.settings_dark_light),
                        AppPreferences.DarkMode.DARK to stringResource(R.string.settings_dark_dark)
                    )
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = Spacing.md)
                    ) {
                        options.forEachIndexed { index, (mode, label) ->
                            SegmentedButton(
                                selected = darkMode == mode,
                                onClick = { ThemeManager.setDarkMode(context, mode) },
                                shape = SegmentedButtonDefaults.itemShape(index, options.size)
                            ) {
                                Text(label, maxLines = 1)
                            }
                        }
                    }
                }

                // Dynamic color (Material You), Android 12+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    item {
                        ThemeRow(
                            label = stringResource(R.string.settings_theme_dynamic),
                            swatch = null,
                            selected = dynamicSelected,
                            onClick = { ThemeManager.selectDynamic(context) }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }

                items(availableThemes) { themeName ->
                    ThemeRow(
                        label = themeName.replace("-", " ").replaceFirstChar {
                            if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                        },
                        swatch = swatches[themeName],
                        selected = !dynamicSelected && currentTheme?.name == themeName,
                        onClick = {
                            scope.launch {
                                ThemeManager.selectTheme(context, themeName)
                            }
                        }
                    )
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
                    SectionHeader(stringResource(R.string.settings_section_help))
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

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = Spacing.sm)
    )
}

/** Theme picker row: color-swatch preview, name, and a check when selected. */
@Composable
private fun ThemeRow(
    label: String,
    swatch: ThemeSwatch?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (swatch != null) {
            SwatchPreview(swatch)
        } else {
            // Dynamic (Material You): sample the live dynamic scheme
            SwatchPreview(
                ThemeSwatch(
                    background = MaterialTheme.colorScheme.surfaceContainerLowest,
                    card = MaterialTheme.colorScheme.secondaryContainer,
                    accent = MaterialTheme.colorScheme.primary,
                    destructive = MaterialTheme.colorScheme.tertiary
                )
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = Spacing.md)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Four overlapping color dots sampling a theme's palette. */
@Composable
private fun SwatchPreview(swatch: ThemeSwatch) {
    val colors = listOf(swatch.background, swatch.card, swatch.accent, swatch.destructive)
    Row {
        colors.forEachIndexed { index, color ->
            Box(
                modifier = Modifier
                    .offset(x = (-6 * index).dp)
                    .size(20.dp)
                    .background(color, CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
        }
    }
}
