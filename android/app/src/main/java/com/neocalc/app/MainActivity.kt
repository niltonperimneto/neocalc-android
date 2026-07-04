package com.neocalc.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.neocalc.app.core.CalculatorViewModel
import com.neocalc.app.ui.style.NeoCalcTheme
import com.neocalc.app.ui.theme.ThemeManager
import com.neocalc.app.ui.windows.MainWindow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Same instance the Compose tree resolves via viewModel().
    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            ThemeManager.initialize(this@MainActivity)
        }
        // Edge-to-edge with visible transparent system bars; the Scaffold
        // consumes the insets. (Previously the bars were hidden entirely.)
        enableEdgeToEdge()

        setContent {
            NeoCalcTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainWindow()
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Guarantee durability beyond the backend's debounced writer.
        viewModel.flush()
    }
}
