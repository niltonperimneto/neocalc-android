package com.neocalc.app.ui.grids

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.neocalc.app.core.CalculatorViewModel
import com.neocalc.app.ui.components.ButtonType
import com.neocalc.app.ui.components.GridButton

@Composable
fun StandardGrid(
    viewModel: CalculatorViewModel,
    modifier: Modifier = Modifier
) {
    GridContainer(modifier) {
        // Missing Standard Functions Row
        Row(Modifier.fillMaxWidth()) {
            GridButton("%", { viewModel.input("%") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("ANS", { viewModel.insertLastResult() }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("x²", { viewModel.input("^2") }, ButtonType.FUNCTION, Modifier.weight(1f))
            GridButton("√", { viewModel.input("sqrt(") }, ButtonType.FUNCTION, Modifier.weight(1f))
        }

        Numpad(
            viewModel = viewModel,
            // Slot 1 (Next to 3): Power ^
            slot1 = {
                GridButton("^", { viewModel.input("^") }, ButtonType.OPERATOR, Modifier.weight(1f))
            },
            // Slot 2 (Next to Dot): Equals =
            slot2 = {
                GridButton("=", { viewModel.evaluate() }, ButtonType.EQUALS, Modifier.weight(1f))
            }
        )
    }
}
