package com.neocalc.app.core

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.neocalc.app.R
import uniffi.neocalc_backend.HistoryItem

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application.applicationContext)

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    init {
        // The Rust core persists show_fractions but exposes no getter, so the
        // preference mirror is the source of truth for seeding the toggle.
        val showFractions = AppPreferences.getShowFractions(application.applicationContext)
        sessionManager.setFractionDisplay(showFractions)
        _uiState.update {
            it.copy(showFractions = showFractions, errorMessage = sessionManager.loadWarning())
        }
        syncState()
    }

    private fun syncState() {
        _uiState.update { current ->
            current.copy(
                sessions = sessionManager.sessions,
                currentSession = sessionManager.currentSession,
                displayValue = sessionManager.getBuffer(),
                history = sessionManager.getHistory(),
                currentMode = sessionManager.currentSession?.mode ?: CalculatorMode.STANDARD,
                lastResult = sessionManager.getLastResult()
            )
        }
    }

    fun addNewSession() {
        viewModelScope.launch {
            if (!sessionManager.createSession()) {
                val message = getApplication<Application>().getString(R.string.session_limit_reached)
                _uiState.update { it.copy(errorMessage = message) }
            }
            syncState()
        }
    }

    /** Consume the one-shot error/warning message after the UI has shown it. */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun switchToSession(session: SessionManager.Session) {
        viewModelScope.launch {
            sessionManager.switchTo(session)
            syncState()
        }
    }

    fun removeSession(session: SessionManager.Session) {
        viewModelScope.launch {
            if (sessionManager.removeSession(session)) {
                syncState()
            }
        }
    }

    fun setMode(newMode: CalculatorMode) {
        viewModelScope.launch {
            sessionManager.currentSession?.let { session ->
                sessionManager.setSessionMode(session, newMode)
            }
            _uiState.update { it.copy(currentMode = newMode) }
        }
    }

    fun cycleMode(forward: Boolean) {
        val modes = CalculatorMode.entries
        val current = uiState.value.currentMode
        val currentIndex = modes.indexOf(current)
        val newIndex = if (forward) {
            (currentIndex + 1) % modes.size
        } else {
            (currentIndex - 1 + modes.size) % modes.size
        }
        setMode(modes[newIndex])
    }

    /** Revert the calculator to a history entry: its expression becomes the buffer again. */
    fun restoreHistoryEntry(item: HistoryItem) {
        viewModelScope.launch {
            try {
                val newBuffer = sessionManager.restoreExpression(item.expression)
                _uiState.update { it.copy(displayValue = newBuffer) }
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring history entry", e)
            }
        }
    }

    /** Insert a history entry's result into the current expression. */
    fun insertHistoryResult(item: HistoryItem) {
        if (item.isError) return
        viewModelScope.launch {
            try {
                sessionManager.currentSession?.let { session ->
                    val newBuffer = session.calculator.input(item.result)
                    _uiState.update { it.copy(displayValue = newBuffer) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting history result", e)
            }
        }
    }

    fun input(text: String) {
        viewModelScope.launch {
            try {
                sessionManager.currentSession?.let { session ->
                    val newBuffer = session.calculator.input(text)
                    _uiState.update { it.copy(displayValue = newBuffer) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing input", e)
            }
        }
    }

    fun backspace() {
        viewModelScope.launch {
            try {
                sessionManager.currentSession?.let { session ->
                    val newBuffer = session.calculator.backspace()
                    _uiState.update { it.copy(displayValue = newBuffer) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing backspace", e)
            }
        }
    }

    fun clear() {
        viewModelScope.launch {
            try {
                sessionManager.currentSession?.let { session ->
                    val newBuffer = session.calculator.clear()
                    _uiState.update { it.copy(displayValue = newBuffer) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing clear", e)
            }
        }
    }

    fun evaluate() {
        viewModelScope.launch {
            try {
                sessionManager.currentSession?.let { session ->
                    val result = session.calculator.evaluate()
                    _uiState.update { current ->
                        current.copy(
                            displayValue = result,
                            history = sessionManager.getHistory(),
                            lastResult = sessionManager.getLastResult()
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing evaluate", e)
                _uiState.update { it.copy(displayValue = "Error", errorMessage = e.message) }
            }
        }
    }

    fun convertToHex() {
        viewModelScope.launch {
            try {
                sessionManager.currentSession?.let { session ->
                    val result = session.calculator.convertToHex()
                    _uiState.update { it.copy(displayValue = result) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing hex conversion", e)
            }
        }
    }

    fun convertToBin() {
        viewModelScope.launch {
            try {
                sessionManager.currentSession?.let { session ->
                    val result = session.calculator.convertToBin()
                    _uiState.update { it.copy(displayValue = result) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing bin conversion", e)
            }
        }
    }

    fun convertToOct() {
        viewModelScope.launch {
            try {
                sessionManager.currentSession?.let { session ->
                    val result = session.calculator.convertToOct()
                    _uiState.update { it.copy(displayValue = result) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error executing octal conversion", e)
            }
        }
    }

    fun insertLastResult() {
        viewModelScope.launch {
            try {
                sessionManager.getLastResult()?.let { lastResult ->
                    sessionManager.currentSession?.let { session ->
                        val newBuffer = session.calculator.input(lastResult)
                        _uiState.update { it.copy(displayValue = newBuffer) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting last result", e)
            }
        }
    }

    fun setFractionDisplay(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setFractionDisplay(enabled)
            AppPreferences.setShowFractions(getApplication<Application>().applicationContext, enabled)
            _uiState.update { it.copy(showFractions = enabled) }
        }
    }

    /**
     * Synchronously persist all sessions on a background thread. Safe to call
     * from lifecycle hooks; the Rust backend also debounce-saves continuously.
     */
    fun flush() {
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.flush()?.let { error ->
                Log.e(TAG, "Failed to persist sessions: $error")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // viewModelScope is already cancelled here; flush on a plain thread.
        Thread { sessionManager.cleanup() }.start()
    }

    companion object {
        private const val TAG = "CalculatorViewModel"
    }
}
