package com.neocalc.app.ui.grids

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.neocalc.app.core.CalculatorViewModel
import com.neocalc.app.ui.components.GridButton
import com.neocalc.app.ui.components.ButtonType

@Composable
@Composable
fun FinancialGrid(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    GridContainer(modifier) {
        // Row 1: ( ) % abs
        Row(Modifier.fillMaxWidth()) {
            GridButton("(", { viewModel.input("(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton(")", { viewModel.input(")") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("%", { viewModel.input("%") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("abs", { viewModel.input("abs(") }, ButtonType.FUNCTION, Modifier.weight(1f))
        }

        // Row 2: fv pv pmt nper
        Row(Modifier.fillMaxWidth()) {
            GridButton("fv", { viewModel.input("fv(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("pv", { viewModel.input("pv(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("pmt", { viewModel.input("pmt(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("nper", { viewModel.input("nper(") }, ButtonType.FUNCTION, Modifier.weight(1f))
        }

        // Row 3: rate npv irr ^
        Row(Modifier.fillMaxWidth()) {
            GridButton("rate", { viewModel.input("rate(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("npv", { viewModel.input("npv(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("irr", { viewModel.input("irr(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("^", { viewModel.input("^") }, ButtonType.OPERATOR, Modifier.weight(1f))
        }

        // Row 4: ln log e sqrt
        Row(Modifier.fillMaxWidth()) {
            GridButton("ln", { viewModel.input("ln(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("log", { viewModel.input("log(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("e", { viewModel.input("e") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("√", { viewModel.input("sqrt(") }, ButtonType.FUNCTION, Modifier.weight(1f))
        }

        // Row 5: mean std var median
        Row(Modifier.fillMaxWidth()) {
            GridButton("mean", { viewModel.input("mean(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("std", { viewModel.input("std(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("var", { viewModel.input("var(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("median", { viewModel.input("median(") }, ButtonType.FUNCTION, Modifier.weight(1f))
        }

        // Standard Numpad
        Numpad(
            viewModel = viewModel,
            slot1 = {
                GridButton("e", { viewModel.input("e") }, ButtonType.FUNCTION, Modifier.weight(1f))
            },
            slot2 = {
                GridButton("=", { viewModel.evaluate() }, ButtonType.EQUALS, Modifier.weight(1f))
            }
        )
    }
}
