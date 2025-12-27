package com.neocalc.app.ui.grids

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.neocalc.app.core.CalculatorViewModel
import com.neocalc.app.ui.components.ButtonType
import com.neocalc.app.ui.components.GridButton
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize

@Composable
fun Numpad(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier,
    // Slot for Row 4, Col 4 (Next to 3)
    slot1: @Composable RowScope.() -> Unit = {
         GridButton("=", { viewModel.evaluate() }, ButtonType.EQUALS, Modifier.weight(1f))
    },
    // Slot for Row 5, Col 3 (Next to Dot)
    slot2: @Composable RowScope.() -> Unit = { } 
) {
    // Row 1: Clear, /, *, Backspace (GTK Standard Layout)
    Row(Modifier.fillMaxWidth()) {
        GridButton("C", { viewModel.clear() }, ButtonType.DESTRUCTIVE, Modifier.weight(1f))
        GridButton("÷", { viewModel.input("/") }, ButtonType.OPERATOR, Modifier.weight(1f))
        GridButton("×", { viewModel.input("*") }, ButtonType.OPERATOR, Modifier.weight(1f))
        GridButton(
            onClick = { viewModel.backspace() },
            type = ButtonType.DESTRUCTIVE,
            modifier = Modifier.weight(1f),
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "Backspace"
        )
    }

    // Row 2: 7, 8, 9, -
    Row(Modifier.fillMaxWidth()) {
        GridButton("7", { viewModel.input("7") }, ButtonType.NUMBER, Modifier.weight(1f))
        GridButton("8", { viewModel.input("8") }, ButtonType.NUMBER, Modifier.weight(1f))
        GridButton("9", { viewModel.input("9") }, ButtonType.NUMBER, Modifier.weight(1f))
        GridButton("−", { viewModel.input("-") }, ButtonType.OPERATOR, Modifier.weight(1f))
    }

    // Row 3: 4, 5, 6, +
    Row(Modifier.fillMaxWidth()) {
        GridButton("4", { viewModel.input("4") }, ButtonType.NUMBER, Modifier.weight(1f))
        GridButton("5", { viewModel.input("5") }, ButtonType.NUMBER, Modifier.weight(1f))
        GridButton("6", { viewModel.input("6") }, ButtonType.NUMBER, Modifier.weight(1f))
        GridButton("+", { viewModel.input("+") }, ButtonType.OPERATOR, Modifier.weight(1f))
    }

    // Row 4: 1, 2, 3, [Slot1]
    Row(Modifier.fillMaxWidth()) {
        GridButton("1", { viewModel.input("1") }, ButtonType.NUMBER, Modifier.weight(1f))
        GridButton("2", { viewModel.input("2") }, ButtonType.NUMBER, Modifier.weight(1f))
        GridButton("3", { viewModel.input("3") }, ButtonType.NUMBER, Modifier.weight(1f))
        slot1()
    }

    // Row 5: 0, ., [Slot2]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max), // Force equal height
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        GridButton("0", { viewModel.input("0") }, ButtonType.NUMBER, Modifier.weight(2f), useAspectRatio = false) // Span 2, no forced AR
        GridButton(".", { viewModel.input(".") }, ButtonType.NUMBER, Modifier.weight(1f))
        slot2()
    }
}
