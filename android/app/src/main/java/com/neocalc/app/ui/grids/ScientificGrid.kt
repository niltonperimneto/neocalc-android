package com.neocalc.app.ui.grids

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.neocalc.app.core.CalculatorViewModel
import com.neocalc.app.ui.components.GridButton
import com.neocalc.app.ui.style.ButtonType

@Composable
fun ScientificGrid(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    // Scrollable column to fit all scientific keys on smaller screens
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        val rowModifier = Modifier.weight(1f, fill = false) 
        // Note: We use fill=false for weight in a scrollable column or explicit height
        // But for a Scrollable Column, 'weight' doesn't work well directly on children if parent is unbound.
        // We actually want a Fixed height approach or just default wrap content. 
        // For simpler "Grid" feel in a fixed space, we usually use weight. 
        // But since this might be TALLER than screen, 'verticalScroll' is right.
        // We will just let rows wrap individually.

        // Constants & Basic Functions
        Row(Modifier.fillMaxWidth()) {
            GridButton("(", { viewModel.input("(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton(")", { viewModel.input(")") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("π", { viewModel.input("pi") }, ButtonType.FUNCTION, Modifier.weight(1f)) 
            GridButton("e", { viewModel.input("e") }, ButtonType.FUNCTION, Modifier.weight(1f))
        }

        Row(Modifier.fillMaxWidth()) {
            GridButton("sin", { viewModel.input("sin(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("cos", { viewModel.input("cos(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("tan", { viewModel.input("tan(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("√", { viewModel.input("sqrt(") }, ButtonType.FUNCTION, Modifier.weight(1f))
        }

        Row(Modifier.fillMaxWidth()) {
            GridButton("asin", { viewModel.input("asin(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("acos", { viewModel.input("acos(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("atan", { viewModel.input("atan(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("^", { viewModel.input("^") }, ButtonType.OPERATOR, Modifier.weight(1f))
        }

        Row(Modifier.fillMaxWidth()) {
            GridButton("ln", { viewModel.input("ln(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("log", { viewModel.input("log(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("abs", { viewModel.input("abs(") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("im", { viewModel.input("im(") }, ButtonType.FUNCTION, Modifier.weight(1f))
        }
        
        // Standard Numpad + Ops
        // Row: ClearRow
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
            // Empty slot or extra fun? let's fill with Ans/History if possible or just spacer.
            // Let's put a spacer or just stretch "=" from above? No, simple grid.
            // Let's put 'i' (imaginary unit)
            GridButton("i", { viewModel.input("i") }, ButtonType.NUMBER, Modifier.weight(1f))
        }
    }
}
