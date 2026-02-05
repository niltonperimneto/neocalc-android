package com.neocalc.app.core

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uniffi.neocalc_backend.HistoryItem

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application.applicationContext)

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    // Convenience accessors for backward compatibility during migration
    val sessions: StateFlow<List<SessionManager.Session>> get() = MutableStateFlow(uiState.value.sessions)
    val currentSession: StateFlow<SessionManager.Session?> get() = MutableStateFlow(uiState.value.currentSession)
    val displayValue: StateFlow<String> get() = MutableStateFlow(uiState.value.displayValue)
    val history: StateFlow<List<String>> get() = MutableStateFlow(
        uiState.value.history.map { item -> "${item.expression} = ${item.result}" }
    )
    val mode: StateFlow<CalculatorMode> get() = MutableStateFlow(uiState.value.currentMode)
    val showFractions: StateFlow<Boolean> get() = MutableStateFlow(uiState.value.showFractions)

    init {
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
            sessionManager.createSession()
            syncState()
        }
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
            sessionManager.currentSession?.let {
                it.mode = newMode
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

    fun insertHistoryItem(item: String) {
        viewModelScope.launch {
            try {
                sessionManager.currentSession?.let { session ->
                    val result = item.substringAfterLast("=").trim()
                    val newBuffer = session.calculator.input(result)
                    _uiState.update { it.copy(displayValue = newBuffer) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting history item", e)
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
            _uiState.update { it.copy(showFractions = enabled) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        sessionManager.cleanup()
    }

    companion object {
        private const val TAG = "CalculatorViewModel"
    }
}
