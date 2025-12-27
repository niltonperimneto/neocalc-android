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
import com.neocalc.app.ui.components.ButtonType

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
        // Row 1: ( ) bnot mod
        Row(Modifier.fillMaxWidth()) {
            GridButton("(", { viewModel.input("(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton(")", { viewModel.input(")") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("NOT", { viewModel.input("bnot(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("mod", { viewModel.input("%") }, ButtonType.FUNCTION, Modifier.weight(1f))
        }

        // Row 2: band bor bxor A
        Row(Modifier.fillMaxWidth()) {
            GridButton("AND", { viewModel.input("band(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("OR", { viewModel.input("bor(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("XOR", { viewModel.input("bxor(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("A", { viewModel.input("A") }, ButtonType.NUMBER, Modifier.weight(1f))
        }

        // Row 3: lsh rsh rol ror
        Row(Modifier.fillMaxWidth()) {
            GridButton("LSH", { viewModel.input("lsh(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("RSH", { viewModel.input("rsh(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("ROL", { viewModel.input("rol(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("ROR", { viewModel.input("ror(") }, ButtonType.FUNCTION, Modifier.weight(1f))
        }

        // Row 4: Hex Bin B C
        Row(Modifier.fillMaxWidth()) {
            GridButton("Hex", { viewModel.convertToHex() }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("Bin", { viewModel.convertToBin() }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("B", { viewModel.input("B") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("C", { viewModel.input("C") }, ButtonType.NUMBER, Modifier.weight(1f))
        }

        // Row 5: D E F 0x
        Row(Modifier.fillMaxWidth()) {
            GridButton("D", { viewModel.input("D") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("E", { viewModel.input("E") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("F", { viewModel.input("F") }, ButtonType.NUMBER, Modifier.weight(1f))
            GridButton("0x", { viewModel.input("0x") }, ButtonType.NUMBER, Modifier.weight(1f))
        }

        // Standard Numpad
        Numpad(
            viewModel = viewModel,
            slot1 = {
                GridButton("=", { viewModel.evaluate() }, ButtonType.EQUALS, Modifier.weight(1f))
            },
            slot2 = {
                GridButton("0b", { viewModel.input("0b") }, ButtonType.NUMBER, Modifier.weight(1f))
            }
        )
    }
}
