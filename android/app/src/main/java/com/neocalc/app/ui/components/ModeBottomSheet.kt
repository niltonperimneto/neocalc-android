package com.neocalc.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.neocalc.app.R
import com.neocalc.app.core.CalculatorMode
import com.neocalc.app.ui.style.Spacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Mode selection bottom sheet for switching calculator modes.
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
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.xl)
        ) {
            Text(
                stringResource(R.string.calculator_select_mode),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(Spacing.md)
            )
            HorizontalDivider()
            
            CalculatorMode.entries.forEach { mode ->
                val modeIcon = when (mode) {
                    CalculatorMode.STANDARD -> Icons.Default.Calculate
                    CalculatorMode.SCIENTIFIC -> Icons.Default.Science
                    CalculatorMode.PROGRAMMING -> Icons.Default.Code
                    CalculatorMode.FINANCIAL -> Icons.Default.AttachMoney
                }
                
                NavigationDrawerItem(
                    label = { Text(mode.title) },
                    icon = { Icon(modeIcon, null) },
                    selected = currentMode == mode,
                    onClick = {
                        onModeSelected(mode)
                        scope.launch { sheetState.hide() }.invokeOnCompletion { 
                            if (!sheetState.isVisible) {
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = Spacing.sm + Spacing.xs, vertical = Spacing.xs),
                    shape = RoundedCornerShape(Spacing.sm + Spacing.xs)
                )
            }
        }
    }
}
