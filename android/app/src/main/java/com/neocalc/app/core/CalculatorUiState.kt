package com.neocalc.app.core

import uniffi.neocalc_backend.HistoryItem

/**
 * Represents the complete UI state for the calculator screen.
 * Follows MVI pattern for predictable state management.
 */
data class CalculatorUiState(
    val sessions: List<SessionManager.Session> = emptyList(),
    val currentSession: SessionManager.Session? = null,
    val displayValue: String = "0",
    val history: List<HistoryItem> = emptyList(),
    val currentMode: CalculatorMode = CalculatorMode.STANDARD,
    val showFractions: Boolean = false,
    val lastResult: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
