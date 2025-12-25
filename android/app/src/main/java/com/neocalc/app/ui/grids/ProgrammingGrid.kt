package com.neocalc.app.ui.grids

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.neocalc.app.core.CalculatorViewModel
import com.neocalc.app.ui.components.GridButton
import com.neocalc.app.ui.style.ButtonType

@Composable
fun ProgrammingGrid(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Hex/Bin/Oct Controls
        Row(Modifier.fillMaxWidth()) {
            GridButton("Hex", { viewModel.convertToHex() }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("Bin", { viewModel.convertToBin() }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("mod", { viewModel.input("%") }, ButtonType.FUNCTION, Modifier.weight(1f))
            // Bitwise NOT
            GridButton("NOT", { viewModel.input("bnot(") }, ButtonType.FUNCTION, Modifier.weight(1f))
        }

        // Bitwise Ops
        Row(Modifier.fillMaxWidth()) {
            GridButton("AND", { viewModel.input("band(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("OR", { viewModel.input("bor(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("XOR", { viewModel.input("bxor(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("LSH", { viewModel.input("lsh(") }, ButtonType.FUNCTION, Modifier.weight(1f))
        }
        
        Row(Modifier.fillMaxWidth()) {
            GridButton("A", { viewModel.input("A") }, ButtonType.NUMBER, Modifier.weight(1f)) // Colored as number or hex?
            GridButton("B", { viewModel.input("B") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("C", { viewModel.input("C") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("RSH", { viewModel.input("rsh(") }, ButtonType.FUNCTION, Modifier.weight(1f))
        }

        Row(Modifier.fillMaxWidth()) {
            GridButton("D", { viewModel.input("D") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("E", { viewModel.input("E") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("F", { viewModel.input("F") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("0x", { viewModel.input("0x") }, ButtonType.NUMBER, Modifier.weight(1f))
        }

        // Standard Block
        Row(Modifier.fillMaxWidth()) {
            GridButton("C", { viewModel.clear() }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("⌫", { viewModel.backspace() }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("/", { viewModel.input("/") }, ButtonType.OPERATOR, Modifier.weight(1f))
            GridButton("*", { viewModel.input("*") }, ButtonType.OPERATOR, Modifier.weight(1f))
        }

        Row(Modifier.fillMaxWidth()) {
            GridButton("7", { viewModel.input("7") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("8", { viewModel.input("8") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("9", { viewModel.input("9") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("-", { viewModel.input("-") }, ButtonType.OPERATOR, Modifier.weight(1f))
        }

        Row(Modifier.fillMaxWidth()) {
            GridButton("4", { viewModel.input("4") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("5", { viewModel.input("5") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("6", { viewModel.input("6") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("+", { viewModel.input("+") }, ButtonType.OPERATOR, Modifier.weight(1f))
        }

        Row(Modifier.fillMaxWidth()) {
            GridButton("1", { viewModel.input("1") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("2", { viewModel.input("2") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("3", { viewModel.input("3") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("=", { viewModel.evaluate() }, ButtonType.EQUALS, Modifier.weight(1f))
        }

        Row(Modifier.fillMaxWidth()) {
            GridButton("0", { viewModel.input("0") }, ButtonType.NUMBER, Modifier.weight(2f)) // Span 2
            GridButton(".", { viewModel.input(".") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("0b", { viewModel.input("0b") }, ButtonType.NUMBER, Modifier.weight(1f))
        }
    }
}
