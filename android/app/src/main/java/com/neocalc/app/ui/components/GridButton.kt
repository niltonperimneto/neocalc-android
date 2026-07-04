package com.neocalc.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.neocalc.app.ui.style.KeyStyle
import com.neocalc.app.ui.style.Spacing

enum class ButtonType {
    NUMBER, OPERATOR, FUNCTION, EQUALS, DESTRUCTIVE, NONE
}

/**
 * Calculator key in the Material 3 expressive style: rests as a pill and
 * springs toward a rounded square while pressed.
 */
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
    // Tonal hierarchy: neutral digits, tinted functions, vivid operators,
    // primary equals — color conveys role, like the system calculator.
    val containerColor = when (type) {
        ButtonType.NUMBER -> MaterialTheme.colorScheme.surfaceContainerHigh
        ButtonType.OPERATOR -> MaterialTheme.colorScheme.tertiaryContainer
        ButtonType.FUNCTION -> MaterialTheme.colorScheme.secondaryContainer
        ButtonType.EQUALS -> MaterialTheme.colorScheme.primary
        ButtonType.DESTRUCTIVE -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val contentColor = when (type) {
        ButtonType.NUMBER -> MaterialTheme.colorScheme.onSurface
        ButtonType.OPERATOR -> MaterialTheme.colorScheme.onTertiaryContainer
        ButtonType.FUNCTION -> MaterialTheme.colorScheme.onSecondaryContainer
        ButtonType.EQUALS -> MaterialTheme.colorScheme.onPrimary
        ButtonType.DESTRUCTIVE -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (pressed) KeyStyle.cornerPressed else KeyStyle.cornerRest,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "keyCornerMorph"
    )

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.KeyboardTap)
            onClick()
        },
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(corner),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .padding(Spacing.buttonGap)
            .fillMaxSize()
            .then(if (useAspectRatio) Modifier.aspectRatio(KeyStyle.aspect) else Modifier)
            .semantics { role = Role.Button }
    ) {
        if (imageVector != null) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription ?: text,
                modifier = Modifier.fillMaxSize(0.4f)
            )
        } else {
            // Scale label size down as labels get longer (sin, ANS, 1/x, ...)
            val displayText = text ?: ""
            val textStyle = when {
                displayText.length > 4 -> MaterialTheme.typography.titleSmall
                displayText.length > 2 -> MaterialTheme.typography.titleMedium
                else -> MaterialTheme.typography.headlineSmall
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
