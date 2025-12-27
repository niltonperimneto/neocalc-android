package com.neocalc.app.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import uniffi.neocalc_backend.Calculator

class CalculatorViewModel : ViewModel() {
    // Direct instantiation of the generated Rust wrapper
    private val calculator = Calculator()

    private val _displayValue = MutableStateFlow("0")
    val displayValue: StateFlow<String> = _displayValue.asStateFlow()

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    private val _mode = MutableStateFlow(CalculatorMode.STANDARD)
    val mode: StateFlow<CalculatorMode> = _mode.asStateFlow()

    fun setMode(newMode: CalculatorMode) {
        _mode.value = newMode
    }

    fun input(text: String) {
        viewModelScope.launch {
            try {
                _displayValue.value = calculator.input(text)
            } catch (e: Exception) {
                // Log error or handle
            }
        }
    }

    fun backspace() {
        viewModelScope.launch {
            try {
                _displayValue.value = calculator.backspace()
            } catch (e: Exception) {}
        }
    }

    fun clear() {
        viewModelScope.launch {
            try {
                _displayValue.value = calculator.clear()
            } catch (e: Exception) {}
        }
    }

    fun evaluate() {
        viewModelScope.launch {
            try {
                // Evaluate current state (passing null as per previous logic implied by reflection)
                _displayValue.value = calculator.evaluate(null)
                updateHistory()
            } catch (e: Exception) {
               _displayValue.value = "Error"
            }
        }
    }

    fun convertToHex() {
        viewModelScope.launch {
            try {
                _displayValue.value = calculator.convertToHex()
            } catch (e: Exception) {}
        }
    }

    fun convertToBin() {
        viewModelScope.launch {
            try {
                _displayValue.value = calculator.convertToBin()
            } catch (e: Exception) {}
        }
    }

    private fun updateHistory() {
         viewModelScope.launch {
            try {
                _history.value = calculator.getHistory()
            } catch (e: Exception) {}
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Ensure we close/destroy the Rust object when ViewModel is cleared
        calculator.destroy()
    }
}
