package com.neocalc.app.ui.grids

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.neocalc.app.core.CalculatorViewModel
import com.neocalc.app.ui.components.ButtonType
import com.neocalc.app.ui.components.GridButton

@Composable
fun ScientificGrid(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    GridContainer(modifier) {
        // Row 1: Basic Math & Grouping
        Row(Modifier.fillMaxWidth()) {
            GridButton("(", { viewModel.input("(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton(")", { viewModel.input(")") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("√", { viewModel.input("sqrt(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("^", { viewModel.input("^") }, ButtonType.OPERATOR, Modifier.weight(1f))
        }

        // Row 2: Constants & Logarithms
        Row(Modifier.fillMaxWidth()) {
            GridButton("ln", { viewModel.input("ln(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("log", { viewModel.input("log(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("π", { viewModel.input("pi") }, ButtonType.FUNCTION, Modifier.weight(1f)) 
            GridButton("e", { viewModel.input("e") }, ButtonType.FUNCTION, Modifier.weight(1f))
        }

        // Row 3: Trigonometry & Abs
        Row(Modifier.fillMaxWidth()) {
            GridButton("sin", { viewModel.input("sin(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("cos", { viewModel.input("cos(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("tan", { viewModel.input("tan(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("abs", { viewModel.input("abs(") }, ButtonType.FUNCTION, Modifier.weight(1f))
        }

        // Row 4: Advanced (Inverse Trig & Complex)
        Row(Modifier.fillMaxWidth()) {
            GridButton("asin", { viewModel.input("asin(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("acos", { viewModel.input("acos(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("atan", { viewModel.input("atan(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("im", { viewModel.input("im(") }, ButtonType.FUNCTION, Modifier.weight(1f))
        }
        
        // Standard Numpad + Ops (using Shared Component)
        Numpad(
            viewModel = viewModel,
            // Slot 1 (Next to 3): Equals =
            slot1 = {
                GridButton("=", { viewModel.evaluate() }, ButtonType.EQUALS, Modifier.weight(1f))
            },
            // Slot 2 (Next to Dot): i (Imaginary)
            slot2 = {
                GridButton("i", { viewModel.input("i") }, ButtonType.NUMBER, Modifier.weight(1f))
            }
        )
    }
}
