package com.neocalc.app.core

enum class CalculatorMode(val title: String) {
    STANDARD("Standard"),
    SCIENTIFIC("Scientific"),
    PROGRAMMING("Programming"),
    FINANCIAL("Financial");

    companion object {
        val entries = values().toList()
    }
}
