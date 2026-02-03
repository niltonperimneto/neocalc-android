package com.neocalc.app.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uniffi.neocalc_backend.Calculator

class CalculatorViewModel : ViewModel() {


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



    // Settings State
    private val _showFractions = MutableStateFlow(false)
    val showFractions: StateFlow<Boolean> = _showFractions.asStateFlow()

    fun setFractionDisplay(enabled: Boolean) {
        viewModelScope.launch {
            _showFractions.value = enabled
            // Update all active sessions settings (or just current, but usually settings are global)
            _sessions.value.forEach { session ->
                 try {
                     session.calculator.setFractionDisplay(enabled)
                 } catch (e: Exception) {
                     android.util.Log.e("CalculatorViewModel", "Error setting fraction display", e)
                 }
            }
        }
    }

    // Expose current mode dynamically via stable StateFlow
    private val _mode = MutableStateFlow(CalculatorMode.STANDARD)
    val mode: StateFlow<CalculatorMode> = _mode.asStateFlow()

    private var modeCollectionJob: kotlinx.coroutines.Job? = null

    init {

        addNewSession()
    }

    fun addNewSession() {
        val newSession = Session(
            id = java.util.UUID.randomUUID().toString(),
            name = "Calc ${(_sessions.value.size + 1)}",
            calculator = Calculator()
        )
        // Apply current settings to new session
        try {
            newSession.calculator.setFractionDisplay(_showFractions.value)
        } catch (e: Exception) { /* ignore */ }

        _sessions.value = _sessions.value + newSession
        switchToSession(newSession)
    }

    fun switchToSession(session: Session) {
        _currentSession.value = session
        
        // Sync display and history and mode
        viewModelScope.launch {
            _displayValue.value = session.calculator.getBuffer()
            _history.value = session.calculator.getHistory()
        }
        
        // Sync Mode: Cancel previous collector and start new one
        modeCollectionJob?.cancel()
        modeCollectionJob = viewModelScope.launch {
            session.mode.collect { newMode ->
                _mode.value = newMode
            }
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

    /**
     * Cycle through modes (for swipe gestures).
     * @param forward If true, cycle forward; if false, cycle backward.
     */
    fun cycleMode(forward: Boolean) {
        val modes = CalculatorMode.entries
        val current = _mode.value
        val currentIndex = modes.indexOf(current)
        val newIndex = if (forward) {
            (currentIndex + 1) % modes.size
        } else {
            (currentIndex - 1 + modes.size) % modes.size
        }
        setMode(modes[newIndex])
    }

    /**
     * Insert a history item value into the current buffer.
     */
    fun insertHistoryItem(item: String) {
        viewModelScope.launch {
            try {
                _currentSession.value?.let { session ->
                    // Extract the result from history item (format: "expression = result")
                    val result = item.substringAfterLast("=").trim()
                    _displayValue.value = session.calculator.input(result)
                }
            } catch (e: Exception) {
                android.util.Log.e("CalculatorViewModel", "Error inserting history item", e)
            }
        }
    }

    fun input(text: String) {
        viewModelScope.launch {
            try {
                _currentSession.value?.let { session ->
                     _displayValue.value = session.calculator.input(text)
                }
            } catch (e: Exception) {
                android.util.Log.e("CalculatorViewModel", "Error executing input", e)
            }
        }
    }

    fun backspace() {
        viewModelScope.launch {
            try {
                 _currentSession.value?.let { session ->
                    _displayValue.value = session.calculator.backspace()
                 }
            } catch (e: Exception) {
                android.util.Log.e("CalculatorViewModel", "Error executing backspace", e)
            }
        }
    }

    fun clear() {
        viewModelScope.launch {
            try {
                 _currentSession.value?.let { session ->
                     _displayValue.value = session.calculator.clear()
                 }
            } catch (e: Exception) {
                android.util.Log.e("CalculatorViewModel", "Error executing clear", e)
            }
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
               android.util.Log.e("CalculatorViewModel", "Error executing evaluate", e)
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
            } catch (e: Exception) {
                android.util.Log.e("CalculatorViewModel", "Error executing hex conversion", e)
            }
        }
    }

    fun convertToBin() {
        viewModelScope.launch {
            try {
                 _currentSession.value?.let { session ->
                    _displayValue.value = session.calculator.convertToBin()
                 }
            } catch (e: Exception) {
                android.util.Log.e("CalculatorViewModel", "Error executing bin conversion", e)
            }
        }
    }

    private fun updateHistory() {
         viewModelScope.launch {
            try {
                 _currentSession.value?.let { session ->
                    _history.value = session.calculator.getHistory()
                 }
            } catch (e: Exception) {
                android.util.Log.e("CalculatorViewModel", "Error updating history", e)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Cleanup Rust resources
        _sessions.value.forEach { it.calculator.destroy() }
    }
}
