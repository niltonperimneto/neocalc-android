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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import coil.ImageLoader

@Composable
fun AboutDialog(
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        onDismissRequest = onDismissRequest,
        title = { Text(text = "About NeoCalc") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                // Logo
                val context = androidx.compose.ui.platform.LocalContext.current
                val imageLoader = coil.ImageLoader.Builder(context)
                    .components {
                        add(coil.decode.SvgDecoder.Factory())
                    }
                    .build()

                coil.compose.AsyncImage(
                    model = "file:///android_asset/logo.svg",
                    contentDescription = "NeoCalc Logo",
                    imageLoader = imageLoader,
                    modifier = Modifier.height(100.dp).fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "Version: 1.0")
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Developer: Nilton Perim Neto", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "© 2025 Nilton Perim Neto")
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "The calculator that judges you.\n(Forked and Broken for educational purposes)")
                Spacer(modifier = Modifier.height(16.dp))
                
                // License Information
                Text(text = "License:", fontWeight = FontWeight.Bold)
                Text(text = "GNU General Public License v3.0")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.gnu.org/licenses/gpl-3.0.html"))
                    context.startActivity(intent)
                }) {
                    Text("View Full License")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // GitHub Link
                TextButton(onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/niltonperimneto/neocalc-android"))
                    context.startActivity(intent)
                }) {
                    Text("Visit GitHub Repository")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Close")
            }
        }
    )
}
