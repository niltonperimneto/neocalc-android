package com.neocalc.app.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.neocalc.app.ui.style.Spacing


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

    val haptic = LocalHapticFeedback.current

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // Light click
            onClick()
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        // M3 shape from MaterialTheme
        shape = MaterialTheme.shapes.medium,
        // Removed explicit elevation for tonal M3 approach (color conveys hierarchy)
        modifier = modifier
            .padding(Spacing.buttonGap)
            .fillMaxSize()
            .then(if (useAspectRatio) Modifier.aspectRatio(1.6f) else Modifier)
            .semantics { role = Role.Button }
    ) {
        if (imageVector != null) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription ?: text,
                modifier = Modifier.fillMaxSize(0.5f)
            )
        } else {
            // M3 Typography scale based on text length
            val displayText = text ?: ""
            val textStyle = when {
                displayText.length > 4 -> MaterialTheme.typography.labelSmall
                displayText.length > 2 -> MaterialTheme.typography.labelMedium
                else -> MaterialTheme.typography.labelLarge
            }
            
            Text(
                text = displayText,
                style = textStyle,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

// Compose Previews for Android Studio design tools
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewNumberButton() {
    MaterialTheme {
        GridButton(
            text = "7",
            onClick = {},
            type = ButtonType.NUMBER,
            useAspectRatio = false
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewOperatorButton() {
    MaterialTheme {
        GridButton(
            text = "+",
            onClick = {},
            type = ButtonType.OPERATOR,
            useAspectRatio = false
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewEqualsButton() {
    MaterialTheme {
        GridButton(
            text = "=",
            onClick = {},
            type = ButtonType.EQUALS,
            useAspectRatio = false
        )
    }
}
