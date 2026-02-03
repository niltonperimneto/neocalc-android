package com.neocalc.app.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neocalc.app.core.CalculatorMode
import com.neocalc.app.core.CalculatorViewModel
import com.neocalc.app.ui.components.Display
import com.neocalc.app.ui.dialogs.AboutDialog
import com.neocalc.app.ui.dialogs.SettingsDialog
import com.neocalc.app.ui.grids.FinancialGrid
import com.neocalc.app.ui.grids.ProgrammingGrid
import com.neocalc.app.ui.grids.ScientificGrid
import com.neocalc.app.ui.grids.StandardGrid
import com.neocalc.app.ui.style.LayoutWeights
import com.neocalc.app.ui.style.Spacing
import kotlinx.coroutines.launch
import kotlin.math.abs

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
        SettingsDialog(
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
    val sheetState = rememberModalBottomSheetState()
    var showModeSheet by remember { mutableStateOf(false) }

    if (showModeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showModeSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.xl)
            ) {
                Text(
                    "Select Mode",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(Spacing.md)
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
                         modifier = Modifier.padding(horizontal = Spacing.sm + Spacing.xs, vertical = Spacing.xs),
                         shape = RoundedCornerShape(Spacing.sm + Spacing.xs)
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
                Text("Sessions", modifier = Modifier.padding(Spacing.md), style = MaterialTheme.typography.titleMedium)
                
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
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Session",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(15),
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
                    shape = RoundedCornerShape(15),
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {


        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { }, // Empty title for calculator
                    navigationIcon = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAbout = true }) {
                            Icon(Icons.Filled.Info, contentDescription = "About")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(innerPadding)
                .padding(bottom = Spacing.md) // Safety margin for screen corners
                .fillMaxSize()
        ) {
            val isLandscape = maxWidth > maxHeight
            
            val history by viewModel.history.collectAsState()
            
            // Shared Grid Content with swipe gesture for mode switching
            val gridContent: @Composable (Modifier) -> Unit = { modifier ->
                var dragDelta by remember { mutableStateOf(0f) }
                
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.sm)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    // Threshold for swipe detection (100px)
                                    if (abs(dragDelta) > 100) {
                                        viewModel.cycleMode(forward = dragDelta < 0) // Left = forward
                                    }
                                    dragDelta = 0f
                                },
                                onDragCancel = { dragDelta = 0f }
                            ) { _, delta ->
                                dragDelta += delta
                            }
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    AnimatedContent(
                        targetState = currentMode,
                        transitionSpec = {
                            val enterSpec = tween<Float>(300, easing = FastOutSlowInEasing)
                            val exitSpec = tween<Float>(200, easing = FastOutSlowInEasing)
                            (fadeIn(animationSpec = enterSpec) + slideInVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) { height -> height })
                                .togetherWith(fadeOut(animationSpec = exitSpec) + slideOutVertically(animationSpec = tween(200, easing = FastOutSlowInEasing)) { height -> -height })
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
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left: Display
                    Display(
                        displayText = displayValue,
                        currentMode = currentMode,
                        onModeClick = { showModeSheet = true },
                        history = history,
                        onHistoryItemClick = { viewModel.insertHistoryItem(it) },
                        modifier = Modifier
                            .weight(LayoutWeights.displayLandscape)
                            .fillMaxSize()
                    )
                    
                    // Right: Grid
                    gridContent(Modifier.weight(LayoutWeights.gridLandscape))
                }
            } else {
                // PORTRAIT: Column Layout
                Column(modifier = Modifier.fillMaxSize()) {
                    Display(
                        displayText = displayValue,
                        currentMode = currentMode,
                        onModeClick = { showModeSheet = true },
                        history = history,
                        onHistoryItemClick = { viewModel.insertHistoryItem(it) },
                        modifier = Modifier.weight(LayoutWeights.displayPortrait)
                    )
                    
                    // Grid takes remaining space
                    gridContent(Modifier.weight(LayoutWeights.gridPortrait))
                }
            }
        }
    }
}
}


