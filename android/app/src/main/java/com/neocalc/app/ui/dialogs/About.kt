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
import coil.ImageLoader
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.neocalc.app.BuildConfig

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
                val imageLoader = androidx.compose.runtime.remember(context) {
                    coil.ImageLoader.Builder(context)
                        .components {
                            add(coil.decode.SvgDecoder.Factory())
                        }
                        .build()
                }

                coil.compose.AsyncImage(
                    model = "file:///android_asset/logo.svg",
                    contentDescription = "NeoCalc Logo",
                    imageLoader = imageLoader,
                    modifier = Modifier.height(100.dp).fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(text = "Version: ${com.neocalc.app.BuildConfig.VERSION_NAME}")
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
                
                // Update Checker
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                var updateStatus by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.neocalc.app.utils.UpdateStatus>(com.neocalc.app.utils.UpdateStatus.Idle) }

                Spacer(modifier = Modifier.height(16.dp))

                when (val status = updateStatus) {
                    is com.neocalc.app.utils.UpdateStatus.Idle -> {
                        TextButton(onClick = {
                            updateStatus = com.neocalc.app.utils.UpdateStatus.Loading
                            scope.launch {
                                updateStatus = com.neocalc.app.utils.UpdateManager.checkForUpdates(com.neocalc.app.BuildConfig.VERSION_NAME)
                            }
                        }) {
                            Text("Check for Updates")
                        }
                    }
                    is com.neocalc.app.utils.UpdateStatus.Loading -> {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    is com.neocalc.app.utils.UpdateStatus.UpToDate -> {
                        Text("You are up to date! ✅", color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                        TextButton(onClick = { updateStatus = com.neocalc.app.utils.UpdateStatus.Idle }) {
                            Text("Check Again")
                        }
                    }
                    is com.neocalc.app.utils.UpdateStatus.Available -> {
                        Text("New Version Available: ${status.version}", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                        TextButton(onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(status.downloadUrl))
                            context.startActivity(intent)
                        }) {
                            Text("Download Latest Release")
                        }
                    }
                    is com.neocalc.app.utils.UpdateStatus.Error -> {
                        Text("Error: ${status.message}", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                        TextButton(onClick = { updateStatus = com.neocalc.app.utils.UpdateStatus.Idle }) {
                            Text("Retry")
                        }
                    }
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
