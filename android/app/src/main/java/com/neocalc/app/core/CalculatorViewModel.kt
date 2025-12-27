package com.neocalc.app.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uniffi.neocalc_backend.Calculator

class CalculatorViewModel : ViewModel() {

    // List of active sessions
    data class Session(
        val id: String,
        var name: String,
        val calculator: Calculator,
        val mode: MutableStateFlow<CalculatorMode> = MutableStateFlow(CalculatorMode.STANDARD)
    )

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private val _currentSession = MutableStateFlow<Session?>(null)
    val currentSession: StateFlow<Session?> = _currentSession.asStateFlow()

    private val _displayValue = MutableStateFlow("0")
    val displayValue: StateFlow<String> = _displayValue.asStateFlow()

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    // UI State for Dialogs
    val showThemeDialog = MutableStateFlow(false)

    // Expose current mode dynamically based on session
    val mode: StateFlow<CalculatorMode> get() = _currentSession.value?.mode?.asStateFlow() ?: MutableStateFlow(CalculatorMode.STANDARD).asStateFlow()

    init {
        // Create initial session
        addNewSession()
    }

    fun addNewSession() {
        val newSession = Session(
            id = java.util.UUID.randomUUID().toString(),
            name = "Calc ${(_sessions.value.size + 1)}",
            calculator = Calculator()
        )
        _sessions.value = _sessions.value + newSession
        switchToSession(newSession)
    }

    fun switchToSession(session: Session) {
        _currentSession.value = session
        // Sync display and history from the new session's calculator state
        viewModelScope.launch {
            _displayValue.value = session.calculator.getBuffer()
            _history.value = session.calculator.getHistory()
        }
    }
    
    fun removeSession(session: Session) {
        if (_sessions.value.size <= 1) return // Keep at least one
        
        // Destroy rust object
        session.calculator.destroy()
        
        val newList = _sessions.value.toMutableList()
        newList.remove(session)
        _sessions.value = newList
        
        if (_currentSession.value == session) {
            switchToSession(newList.first())
        }
    }

    fun setMode(newMode: CalculatorMode) {
        _currentSession.value?.mode?.value = newMode
    }

    fun input(text: String) {
        viewModelScope.launch {
            try {
                _currentSession.value?.let { session ->
                     _displayValue.value = session.calculator.input(text)
                }
            } catch (e: Exception) {
                // Log error or handle
            }
        }
    }

    fun backspace() {
        viewModelScope.launch {
            try {
                 _currentSession.value?.let { session ->
                    _displayValue.value = session.calculator.backspace()
                 }
            } catch (e: Exception) {}
        }
    }

    fun clear() {
        viewModelScope.launch {
            try {
                 _currentSession.value?.let { session ->
                     _displayValue.value = session.calculator.clear()
                 }
            } catch (e: Exception) {}
        }
    }

    fun evaluate() {
        viewModelScope.launch {
            try {
                // Evaluate current state
                 _currentSession.value?.let { session ->
                    _displayValue.value = session.calculator.evaluate(null)
                    updateHistory()
                 }
            } catch (e: Exception) {
               _displayValue.value = "Error"
            }
        }
    }

    fun convertToHex() {
        viewModelScope.launch {
            try {
                 _currentSession.value?.let { session ->
                    _displayValue.value = session.calculator.convertToHex()
                 }
            } catch (e: Exception) {}
        }
    }

    fun convertToBin() {
        viewModelScope.launch {
            try {
                 _currentSession.value?.let { session ->
                    _displayValue.value = session.calculator.convertToBin()
                 }
            } catch (e: Exception) {}
        }
    }

    private fun updateHistory() {
         viewModelScope.launch {
            try {
                 _currentSession.value?.let { session ->
                    _history.value = session.calculator.getHistory()
                 }
            } catch (e: Exception) {}
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Ensure we close/destroy all Rust objects
        _sessions.value.forEach { it.calculator.destroy() }
    }
}
