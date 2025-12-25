package com.neocalc.app.ui.style

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class CalcStyle(
    val backgroundColor: Color,
    val textColor: Color,
    val cornerRadius: Dp = 16.dp,
    val elevation: Dp = 2.dp
)

object StyleManager {
    // Define your "CSS Classes" here
    
    val NUMBER_BUTTON = CalcStyle(
        backgroundColor = Color(0xFFE0E0E0), // Light Gray
        textColor = Color.Black
    )
    
    val OPERATOR_BUTTON = CalcStyle(
        backgroundColor = Color(0xFFFF9800), // Orange
        textColor = Color.White,
        cornerRadius = 50.dp // Circular/Pill
    )
    
    val FUNCTION_BUTTON = CalcStyle(
         backgroundColor = Color(0xFF9E9E9E), // Darker Gray
         textColor = Color.Black
    )
    
    val EQUALS_BUTTON = CalcStyle(
        backgroundColor = Color(0xFF4CAF50), // Green
        textColor = Color.White,
        cornerRadius = 12.dp
    )
    
    // Dark Mode variants could be handled by checking system theme in the composable
    // and passing the right StyleManager object, or having `fun getStyle(isDark: Boolean)`
    
    fun getStyle(type: ButtonType): CalcStyle {
        return when (type) {
            ButtonType.NUMBER -> NUMBER_BUTTON
            ButtonType.OPERATOR -> OPERATOR_BUTTON
            ButtonType.FUNCTION -> FUNCTION_BUTTON
            ButtonType.EQUALS -> EQUALS_BUTTON
            else -> NUMBER_BUTTON
        }
    }
}

enum class ButtonType {
    NUMBER, OPERATOR, FUNCTION, EQUALS, NONE
}
