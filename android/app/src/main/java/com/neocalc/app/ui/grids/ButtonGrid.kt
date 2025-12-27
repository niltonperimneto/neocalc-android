package com.neocalc.app.ui.grids

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.neocalc.app.core.CalculatorViewModel
import com.neocalc.app.ui.components.GridButton
import com.neocalc.app.ui.style.ButtonType

@Composable
fun ButtonGrid(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        
        // Row 1: Clear, Backspace
        Row(Modifier.weight(1f)) {
            GridButton("C", { viewModel.clear() }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("⌫", { viewModel.backspace() }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("/", { viewModel.input("/") }, ButtonType.OPERATOR, Modifier.weight(1f))
            GridButton("*", { viewModel.input("*") }, ButtonType.OPERATOR, Modifier.weight(1f))
        }

        // Row 2: 7, 8, 9, -
        Row(Modifier.weight(1f)) {
            GridButton("7", { viewModel.input("7") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("8", { viewModel.input("8") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("9", { viewModel.input("9") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("-", { viewModel.input("-") }, ButtonType.OPERATOR, Modifier.weight(1f))
        }

        // Row 3: 4, 5, 6, +
        Row(Modifier.weight(1f)) {
            GridButton("4", { viewModel.input("4") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("5", { viewModel.input("5") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("6", { viewModel.input("6") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("+", { viewModel.input("+") }, ButtonType.OPERATOR, Modifier.weight(1f))
        }

        // Row 4: 1, 2, 3, ^
        Row(Modifier.weight(1f)) {
            GridButton("1", { viewModel.input("1") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("2", { viewModel.input("2") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("3", { viewModel.input("3") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("^", { viewModel.input("^") }, ButtonType.OPERATOR, Modifier.weight(1f))
        }

        // Row 5: 0, ., =
        Row(Modifier.weight(1f)) {
            GridButton("0", { viewModel.input("0") }, ButtonType.NUMBER, Modifier.weight(2f)) // Span 2
            GridButton(".", { viewModel.input(".") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("=", { viewModel.evaluate() }, ButtonType.EQUALS, Modifier.weight(1f))
        }
    }
}
