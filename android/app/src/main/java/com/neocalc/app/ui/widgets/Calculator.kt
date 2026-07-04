package com.neocalc.app.ui.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import android.widget.Toast
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neocalc.app.R
import com.neocalc.app.core.CalculatorMode
import com.neocalc.app.core.CalculatorViewModel
import com.neocalc.app.ui.components.Display
import com.neocalc.app.ui.components.ModeBottomSheet
import com.neocalc.app.ui.components.SessionDrawer
import com.neocalc.app.ui.dialogs.AboutDialog
import com.neocalc.app.ui.dialogs.SettingsDialog
import com.neocalc.app.ui.grids.FinancialGrid
import com.neocalc.app.ui.grids.ProgrammingGrid
import com.neocalc.app.ui.grids.ScientificGrid
import com.neocalc.app.ui.grids.StandardGrid
import com.neocalc.app.ui.style.LayoutWeights
import com.neocalc.app.ui.style.Spacing
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Calculator(
    viewModel: CalculatorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // One-shot warnings from the backend (load recovery, session limit, ...)
    val toastContext = LocalContext.current
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            Toast.makeText(toastContext, message, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    var showAbout by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsDialog(
            showFractions = uiState.showFractions,
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
        ModeBottomSheet(
            sheetState = sheetState,
            currentMode = uiState.currentMode,
            onModeSelected = { viewModel.setMode(it) },
            onDismiss = { showModeSheet = false },
            scope = scope
        )
    }

    SessionDrawer(
        drawerState = drawerState,
        sessions = uiState.sessions,
        currentSession = uiState.currentSession,
        onSessionClick = { viewModel.switchToSession(it) },
        onSessionClose = { viewModel.removeSession(it) },
        onAddSession = { viewModel.addNewSession() },
        scope = scope
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                // Blends into the display panel so the top reads as one surface.
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.content_desc_settings),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAbout = true }) {
                            Icon(
                                Icons.Filled.Info,
                                contentDescription = stringResource(R.string.content_desc_about),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            }
        ) { innerPadding ->
            BoxWithConstraints(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(bottom = Spacing.md)
                    .fillMaxSize()
            ) {
                val isLandscape = maxWidth > maxHeight
                
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
                                        if (abs(dragDelta) > 100) {
                                            viewModel.cycleMode(forward = dragDelta < 0)
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
                            targetState = uiState.currentMode,
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
                    Row(modifier = Modifier.fillMaxSize()) {
                        Display(
                            displayText = uiState.displayValue,
                            currentMode = uiState.currentMode,
                            onModeClick = { showModeSheet = true },
                            history = uiState.history,
                            onHistoryRestore = viewModel::restoreHistoryEntry,
                            onHistoryInsertResult = viewModel::insertHistoryResult,
                            modifier = Modifier
                                .weight(LayoutWeights.displayLandscape)
                                .fillMaxSize()
                        )

                        gridContent(Modifier.weight(LayoutWeights.gridLandscape))
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Display(
                            displayText = uiState.displayValue,
                            currentMode = uiState.currentMode,
                            onModeClick = { showModeSheet = true },
                            history = uiState.history,
                            onHistoryRestore = viewModel::restoreHistoryEntry,
                            onHistoryInsertResult = viewModel::insertHistoryResult,
                            modifier = Modifier.weight(LayoutWeights.displayPortrait)
                        )

                        gridContent(Modifier.weight(LayoutWeights.gridPortrait))
                    }
                }
            }
        }
    }
}
