package com.neocalc.app.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AboutDialog(
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "About NeoCalc") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(text = "Version: 1.0")
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Developer: Nilton Perim Neto", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "© 2025 Nilton Perim Neto")
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "The calculator that judges you.\n(Forked and Broken for educational purposes)")
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "Credits:", fontWeight = FontWeight.Bold)
                Text(text = "Original Code: Nilton Perim Neto")
                Text(text = "New Code (Rust): Nilton Perim Neto")
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Purpose:", fontWeight = FontWeight.Bold)
                Text(text = "- To teach innocent kids Python")
                Text(text = "- To try to teach me Rust")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        }
    )
}
