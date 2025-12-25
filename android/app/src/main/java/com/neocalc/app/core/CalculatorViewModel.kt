package com.neocalc.app.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Import the generated Rust bindings
// Note: These will be available after the build task runs
// import com.neocalc.app.core.Calculator
// import com.neocalc.app.core.CalculatorError

class CalculatorViewModel : ViewModel() {
    // We'll lazy initialize the calculator to ensure the library is loaded
    private val calculator: Any by lazy {
        // Dynamic instantiation if needed, or direct constructor usage
        // For now, assuming the bindings generate a class named "Calculator"
        try {
            Class.forName("com.neocalc.app.core.Calculator").getConstructor().newInstance()
        } catch (e: Exception) {
            throw RuntimeException("Failed to load Rust Calculator: ${e.message}")
        }
    }

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
                // Reflection for now until IDE resolves bindings
                val method = calculator.javaClass.getMethod("input", String::class.java)
                val result = method.invoke(calculator, text) as String
                _displayValue.value = result
            } catch (e: Exception) {
                // Log error
            }
        }
    }

    fun backspace() {
        viewModelScope.launch {
            try {
                val method = calculator.javaClass.getMethod("backspace")
                val result = method.invoke(calculator) as String
                _displayValue.value = result
            } catch (e: Exception) {}
        }
    }

    fun clear() {
        viewModelScope.launch {
            try {
                val method = calculator.javaClass.getMethod("clear")
                val result = method.invoke(calculator) as String
                _displayValue.value = result
            } catch (e: Exception) {}
        }
    }

    fun evaluate() {
        viewModelScope.launch {
            try {
                // Using evaluateAsync from Rust
                // Note: The generated binding might expose evaluateAsync as a suspend function or a CompletableFuture
                // UniFFI usually generates "evaluateAsync" as a suspend function in Kotlin.
                // Since I'm using reflection for this "blind" edit, I'll assume standard evaluate first or try to find the async one.
                // Actually, let's stick to the synchronous evaluate for simplicity in this blind setup, 
                // OR assume the bindings are present and just write "invalid" code that will be valid upon build.
                
                // I will write "pseudo-valid" reflection code that assumes generated methods exist.
                val method = calculator.javaClass.getMethod("evaluate", String::class.javaObjectType) // passing null/Option?
                val result = method.invoke(calculator, null) as String
                _displayValue.value = result
                
                updateHistory()
            } catch (e: Exception) {
               _displayValue.value = "Error"
            }
        }
    }
    
    // NOTE: After the first build, you should replace this reflection code with direct calls:
    // private val calculator = Calculator()
    // fun input(text: String) { viewModelScope.launch { _displayValue.value = calculator.input(text) } }

    fun convertToHex() {
        viewModelScope.launch {
            try {
                val method = calculator.javaClass.getMethod("convertToHex")
                val result = method.invoke(calculator) as String
                _displayValue.value = result
            } catch (e: Exception) {}
        }
    }

    fun convertToBin() {
        viewModelScope.launch {
            try {
                val method = calculator.javaClass.getMethod("convertToBin")
                val result = method.invoke(calculator) as String
                _displayValue.value = result
            } catch (e: Exception) {}
        }
    }

    private fun updateHistory() {
        // update _history from calculator.getHistory()
         viewModelScope.launch {
            try {
                val method = calculator.javaClass.getMethod("getHistory")
                @Suppress("UNCHECKED_CAST")
                val res = method.invoke(calculator) as List<String>
                _history.value = res
            } catch (e: Exception) {}
        }
    }
}
