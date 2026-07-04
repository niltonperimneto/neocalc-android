package com.neocalc.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neocalc.app.R
import com.neocalc.app.core.CalculatorMode
import com.neocalc.app.ui.style.Spacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Mode selection bottom sheet, styled like the rest of the app: tonal icon
 * circles, a one-line description per mode, and a highlighted selected row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeBottomSheet(
    sheetState: SheetState,
    currentMode: CalculatorMode,
    onModeSelected: (CalculatorMode) -> Unit,
    onDismiss: () -> Unit,
    scope: CoroutineScope
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md)
                .padding(bottom = Spacing.xl)
        ) {
            Text(
                stringResource(R.string.calculator_select_mode),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = Spacing.md)
            )

            CalculatorMode.entries.forEach { mode ->
                val modeIcon = when (mode) {
                    CalculatorMode.STANDARD -> Icons.Default.Calculate
                    CalculatorMode.SCIENTIFIC -> Icons.Default.Science
                    CalculatorMode.PROGRAMMING -> Icons.Default.Code
                    CalculatorMode.FINANCIAL -> Icons.Default.AttachMoney
                }
                val description = when (mode) {
                    CalculatorMode.STANDARD -> stringResource(R.string.mode_desc_standard)
                    CalculatorMode.SCIENTIFIC -> stringResource(R.string.mode_desc_scientific)
                    CalculatorMode.PROGRAMMING -> stringResource(R.string.mode_desc_programming)
                    CalculatorMode.FINANCIAL -> stringResource(R.string.mode_desc_financial)
                }
                val selected = currentMode == mode

                Surface(
                    onClick = {
                        onModeSelected(mode)
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                onDismiss()
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.large,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        androidx.compose.ui.graphics.Color.Transparent
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.sm + Spacing.xs, vertical = Spacing.sm + Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {}
                            Icon(
                                imageVector = modeIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = Spacing.md)
                        ) {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
