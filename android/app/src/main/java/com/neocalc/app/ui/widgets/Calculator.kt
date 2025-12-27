package com.neocalc.app.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neocalc.app.core.CalculatorViewModel
import com.neocalc.app.ui.components.Display
import com.neocalc.app.ui.grids.ButtonGrid
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.neocalc.app.core.CalculatorMode
import com.neocalc.app.ui.dialogs.AboutDialog
import com.neocalc.app.ui.grids.ScientificGrid
import com.neocalc.app.ui.grids.ProgrammingGrid
import com.neocalc.app.ui.grids.FinancialGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Calculator(
    viewModel: CalculatorViewModel = viewModel()
) {
    val displayValue by viewModel.displayValue.collectAsState()
    val currentMode by viewModel.mode.collectAsState()
    val modes = CalculatorMode.values()
    var showAbout by remember { mutableStateOf(false) }

    if (showAbout) {
        AboutDialog { showAbout = false }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = { Text("NeoCalc") },
                    actions = {
                        IconButton(onClick = { showAbout = true }) {
                            Icon(Icons.Filled.Info, contentDescription = "About")
                        }
                    }
                )
                ScrollableTabRow(
                    selectedTabIndex = currentMode.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    modes.forEach { mode ->
                        Tab(
                            selected = currentMode == mode,
                            onClick = { viewModel.setMode(mode) },
                            text = { Text(text = mode.title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Display(
                displayText = displayValue,
                modifier = Modifier.weight(0.3f)
            )
            
            // Grid container
            Box(Modifier.weight(0.7f)) {
                when (currentMode) {
                    CalculatorMode.STANDARD -> ButtonGrid(viewModel)
                    CalculatorMode.SCIENTIFIC -> ScientificGrid(viewModel)
                    CalculatorMode.PROGRAMMING -> ProgrammingGrid(viewModel)
                    CalculatorMode.FINANCIAL -> FinancialGrid(viewModel)
                }
            }
        }
    }
}

