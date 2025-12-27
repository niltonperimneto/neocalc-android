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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.aspectRatio


enum class ButtonType {
    NUMBER, OPERATOR, FUNCTION, EQUALS, DESTRUCTIVE, NONE
}
@Composable
fun GridButton(
    text: String? = null,
    onClick: () -> Unit,
    type: ButtonType = ButtonType.NUMBER,
    modifier: Modifier = Modifier,
    imageVector: ImageVector? = null,
    contentDescription: String? = null,
    useAspectRatio: Boolean = true
) {
    // Map ButtonType to M3 Color Roles
    val containerColor = when (type) {
        ButtonType.NUMBER -> MaterialTheme.colorScheme.secondaryContainer
        ButtonType.OPERATOR -> MaterialTheme.colorScheme.tertiaryContainer
        ButtonType.FUNCTION -> MaterialTheme.colorScheme.surfaceVariant
        ButtonType.EQUALS -> MaterialTheme.colorScheme.primary
        ButtonType.DESTRUCTIVE -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val contentColor = when (type) {
        ButtonType.NUMBER -> MaterialTheme.colorScheme.onSecondaryContainer
        ButtonType.OPERATOR -> MaterialTheme.colorScheme.onTertiaryContainer
        ButtonType.FUNCTION -> MaterialTheme.colorScheme.onSurfaceVariant
        ButtonType.EQUALS -> MaterialTheme.colorScheme.onPrimary
        ButtonType.DESTRUCTIVE -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        // Rounded shape for modern look
        shape = androidx.compose.foundation.shape.RoundedCornerShape(15), 
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
        modifier = modifier
            .padding(2.dp) // Compact padding
            .fillMaxSize()
            .then(if (useAspectRatio) Modifier.aspectRatio(1.6f) else Modifier) 
    ) {
        if (imageVector != null) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription ?: text,
                modifier = Modifier.fillMaxSize(0.5f)
            )
        } else {
            // Text Scaling Logic
            val displayText = text ?: ""
            val fontSize = when {
                displayText.length > 6 -> 10.sp
                displayText.length > 4 -> 12.sp
                displayText.length > 2 -> 16.sp
                else -> 20.sp
            }
            
            Text(
                text = displayText,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = fontSize),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
