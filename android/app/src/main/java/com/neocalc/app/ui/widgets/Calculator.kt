package com.neocalc.app.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.animation.*
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
    
    // Observe current session mode
    val currentMode by viewModel.mode.collectAsState()
    val displayValue by viewModel.displayValue.collectAsState()
    
    var showAbout by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    
    val showFractions by viewModel.showFractions.collectAsState()

    if (showSettings) {
        com.neocalc.app.ui.dialogs.SettingsDialog(
            showFractions = showFractions,
            onToggleFractions = { viewModel.setFractionDisplay(it) },
            onDismiss = { showSettings = false }
        )
    }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (showAbout) {
        AboutDialog { showAbout = false }
    }

    // Bottom Sheet State
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState()
    var showModeSheet by remember { mutableStateOf(false) }

    if (showModeSheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showModeSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            // Sheet Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Select Mode",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                
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
                             viewModel.setMode(mode)
                             scope.launch { sheetState.hide() }.invokeOnCompletion { 
                                 if (!sheetState.isVisible) {
                                     showModeSheet = false
                                 }
                             }
                         },
                         modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                         shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                     )
                }
            }
        }
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
                        // Close button in badge slot
                        badge = {
                            if (sessionList.size > 1) {
                                IconButton(
                                    onClick = { viewModel.removeSession(session) }
                                ) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                        contentDescription = "Close Session",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
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
                // Header
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    // Theme Switcher (Brush Icon)
                    IconButton(onClick = { viewModel.showThemeDialog.value = true }) {
                        Icon(Icons.Default.Brush, contentDescription = "Themes")
                    }
                    
                    // Settings (Settings Icon)
                    IconButton(onClick = { showSettings = true }) {
                         Icon(androidx.compose.material.icons.filled.Settings, contentDescription = "Settings")
                    }

                    // About (Info Icon)
                    IconButton(onClick = { showAbout = true }) {
                        Icon(Icons.Filled.Info, contentDescription = "About")
                    }
                }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(innerPadding)
                .padding(bottom = 16.dp) // Safety margin for screen corners
                .fillMaxSize()
        ) {
            val isLandscape = maxWidth > maxHeight
            
            val history by viewModel.history.collectAsState()
            
            // Shared Grid Content
            val gridContent: @Composable (Modifier) -> Unit = { modifier ->
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    contentAlignment = androidx.compose.ui.Alignment.BottomCenter // Pin to bottom
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = currentMode,
                        transitionSpec = {
                            (androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { height -> height })
                                .togetherWith(androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { height -> -height })
                        },
                        label = "Grid Transition"
                    ) { targetMode ->
                         when (targetMode) {
                            CalculatorMode.STANDARD -> StandardGrid(viewModel)
                            CalculatorMode.SCIENTIFIC -> ScientificGrid(viewModel)
                            CalculatorMode.PROGRAMMING -> ProgrammingGrid(viewModel)
                            CalculatorMode.FINANCIAL -> FinancialGrid(viewModel)
                        }
                    }
                }
            }

            if (isLandscape) {
                // LANDSCAPE: Row Layout
                androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxSize()) {
                    // Left: Display (40%)
                    Display(
                        displayText = displayValue,
                        currentMode = currentMode,
                        onModeClick = { showModeSheet = true },
                        history = history,
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxSize()
                    )
                    
                    // Right: Grid (60%)
                    gridContent(Modifier.weight(0.6f))
                }
            } else {
                // PORTRAIT: Column Layout
                Column(modifier = Modifier.fillMaxSize()) {
                    Display(
                        displayText = displayValue,
                        currentMode = currentMode,
                        onModeClick = { showModeSheet = true },
                        history = history,
                        modifier = Modifier.weight(0.3f) // Display takes 30%
                    )
                    
                    // Grid takes remaining space 70%
                    gridContent(Modifier.weight(0.7f))
                }
            }
        }
    }
}
}


