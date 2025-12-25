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
fun FinancialGrid(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // TVM Functions
        Row(Modifier.fillMaxWidth()) {
            GridButton("PV", { viewModel.input("pv(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("FV", { viewModel.input("fv(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("PMT", { viewModel.input("pmt(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("Rate", { viewModel.input("rate(") }, ButtonType.FUNCTION, Modifier.weight(1f))
        }

        Row(Modifier.fillMaxWidth()) {
            GridButton("NPER", { viewModel.input("nper(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("NPV", { viewModel.input("npv(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("IRR", { viewModel.input("irr(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("%", { viewModel.input("%") }, ButtonType.FUNCTION, Modifier.weight(1f))
        }

        // Stats
        Row(Modifier.fillMaxWidth()) {
            GridButton("Mean", { viewModel.input("mean(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("Std", { viewModel.input("std(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("Var", { viewModel.input("var(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("Median", { viewModel.input("median(") }, ButtonType.FUNCTION, Modifier.weight(1f)) // Fixed "Med" key
        }

        // Standard Block
        Row(Modifier.fillMaxWidth()) {
            GridButton("C", { viewModel.clear() }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("⌫", { viewModel.backspace() }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("/", { viewModel.input("/") }, ButtonType.OPERATOR, Modifier.weight(1f))
            GridButton("*", { viewModel.input("*") }, ButtonType.OPERATOR, Modifier.weight(1f))
        }

        // Standard Numpad ...
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
            GridButton("^", { viewModel.input("^") }, ButtonType.OPERATOR, Modifier.weight(1f)) 
        }
    }
}
