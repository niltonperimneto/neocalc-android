package com.neocalc.app.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neocalc.app.ui.style.ButtonType
import com.neocalc.app.ui.style.StyleManager

@Composable
fun GridButton(
    text: String,
    onClick: () -> Unit,
    type: ButtonType = ButtonType.NUMBER,
    modifier: Modifier = Modifier
) {
    val style = StyleManager.getStyle(type)

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = style.backgroundColor,
            contentColor = style.textColor
        ),
        shape = RoundedCornerShape(style.cornerRadius),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = style.elevation),
        modifier = modifier
            .padding(4.dp)
            .fillMaxSize()
    ) {
        Text(
            text = text,
            fontSize = 24.sp
        )
    }
}
