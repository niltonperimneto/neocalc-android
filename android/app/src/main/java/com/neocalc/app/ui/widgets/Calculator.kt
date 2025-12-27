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
import com.neocalc.app.ui.grids.StandardGrid
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
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

import kotlinx.coroutines.launch
import com.neocalc.app.core.CalculatorMode
import com.neocalc.app.ui.dialogs.AboutDialog
import com.neocalc.app.ui.grids.ScientificGrid
import com.neocalc.app.ui.grids.ProgrammingGrid
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.Alignment
import com.neocalc.app.ui.grids.FinancialGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Calculator(
    viewModel: CalculatorViewModel = viewModel()
) {
    val currentSession by viewModel.currentSession.collectAsState()
    val sessionList by viewModel.sessions.collectAsState()
    
    // We observe the mode of the *current* session
    val currentMode by viewModel.mode.collectAsState()
    val displayValue by viewModel.displayValue.collectAsState()
    
    val modes = CalculatorMode.values()
    var showAbout by remember { mutableStateOf(false) }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (showAbout) {
        AboutDialog { showAbout = false }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Text("Sessions", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium)
                
                sessionList.forEach { session ->
                    NavigationDrawerItem(
                        label = { Text(session.name) },
                        selected = session == currentSession,
                        onClick = {
                            viewModel.switchToSession(session)
                            scope.launch { drawerState.close() }
                        },
                        // Match GridButton shape
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(15),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                
                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text("New Calculator") },
                    selected = false,
                    icon = { Icon(Icons.Default.Add, "Add") },
                    onClick = {
                        viewModel.addNewSession()
                        scope.launch { drawerState.close() }
                    },
                    // Match GridButton shape
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(15),
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        val showThemeDialog by viewModel.showThemeDialog.collectAsState()
        if (showThemeDialog) {
            com.neocalc.app.ui.dialogs.ThemeDialog {
                viewModel.showThemeDialog.value = false
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Column {
                    // Thin Header: Theme + Mode Selector + About
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp), // Minimal padding
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                    ) {
                        // Theme Switcher (Brush Icon)
                        IconButton(onClick = { viewModel.showThemeDialog.value = true }) {
                            Icon(Icons.Default.Brush, contentDescription = "Themes")
                        }

                        // Mode Selector (Center)
                        Box {
                             var expanded by remember { mutableStateOf(false) }
                             
                             androidx.compose.material3.Button(
                                 onClick = { expanded = true },
                                 colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                     containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                     contentColor = MaterialTheme.colorScheme.onSurface
                                 ),
                                 contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                             ) {
                                 // Current Mode Icon and Text
                                 val icon = when (currentMode) {
                                     CalculatorMode.STANDARD -> Icons.Default.Calculate
                                     // Fallbacks if Science not found, but trying Science
                                     CalculatorMode.SCIENTIFIC -> Icons.Default.Science
                                     CalculatorMode.PROGRAMMING -> Icons.Default.Code
                                     CalculatorMode.FINANCIAL -> Icons.Default.AttachMoney
                                 }
                                 Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                 Text(currentMode.title, style = MaterialTheme.typography.titleMedium)
                                 Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                             }

                             DropdownMenu(
                                 expanded = expanded,
                                 onDismissRequest = { expanded = false },
                                 offset = androidx.compose.ui.unit.DpOffset(0.dp, 10.dp), // Slight offset for notch/aesthetics
                                 containerColor = MaterialTheme.colorScheme.surface
                             ) {
                                 CalculatorMode.values().forEach { mode ->
                                     val modeIcon = when (mode) {
                                         CalculatorMode.STANDARD -> Icons.Default.Calculate
                                         CalculatorMode.SCIENTIFIC -> Icons.Default.Science
                                         CalculatorMode.PROGRAMMING -> Icons.Default.Code
                                         CalculatorMode.FINANCIAL -> Icons.Default.AttachMoney
                                     }
                                     
                                     DropdownMenuItem(
                                         text = { Text(mode.title) },
                                         leadingIcon = { Icon(modeIcon, contentDescription = null) },
                                         onClick = {
                                             viewModel.setMode(mode)
                                             expanded = false
                                         }
                                     )
                                 }
                             }
                        }

                        // About (Info Icon)
                        IconButton(onClick = { showAbout = true }) {
                            Icon(Icons.Filled.Info, contentDescription = "About")
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
            val history by viewModel.history.collectAsState()
            
            Display(
                displayText = displayValue,
                history = history,
                modifier = Modifier.weight(0.4f)
            )
            
            // Grid container
            Box(Modifier.weight(0.6f)) {
                when (currentMode) {
                    CalculatorMode.STANDARD -> StandardGrid(viewModel)
                    CalculatorMode.SCIENTIFIC -> ScientificGrid(viewModel)
                    CalculatorMode.PROGRAMMING -> ProgrammingGrid(viewModel)
                    CalculatorMode.FINANCIAL -> FinancialGrid(viewModel)
                }
            }
        }
    }
}
}

