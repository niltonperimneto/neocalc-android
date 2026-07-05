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
import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description

import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.neocalc.app.BuildConfig

@Composable
fun AboutDialog(
    onDismissRequest: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var updateStatus by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.neocalc.app.utils.UpdateStatus>(com.neocalc.app.utils.UpdateStatus.Idle) }
    
    // Logo Loader
    val imageLoader = androidx.compose.runtime.remember(context) {
        coil3.ImageLoader.Builder(context)
            .components {
                add(coil3.svg.SvgDecoder.Factory())
            }
            .build()
    }

    AlertDialog(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        titleContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
        textContentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
        onDismissRequest = onDismissRequest,
        icon = {
             coil3.compose.AsyncImage(
                model = "file:///android_asset/logo.svg",
                contentDescription = "NeoCalc Logo",
                imageLoader = imageLoader,
                modifier = Modifier.size(64.dp)
            )
        },
        title = {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                Text(text = "NeoCalc", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = "v${com.neocalc.app.BuildConfig.VERSION_NAME}",
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.secondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 1. Update Check Row
                AboutActionRow(
                    icon = when (updateStatus) {
                        is com.neocalc.app.utils.UpdateStatus.Loading -> null // Spinner
                        is com.neocalc.app.utils.UpdateStatus.Downloading -> null // Spinner
                        is com.neocalc.app.utils.UpdateStatus.Available -> androidx.compose.material.icons.Icons.Default.CloudDownload
                        is com.neocalc.app.utils.UpdateStatus.Downloaded -> androidx.compose.material.icons.Icons.Default.InstallMobile
                        is com.neocalc.app.utils.UpdateStatus.UpToDate -> androidx.compose.material.icons.Icons.Default.CheckCircle
                        is com.neocalc.app.utils.UpdateStatus.Error -> androidx.compose.material.icons.Icons.Default.Warning
                        is com.neocalc.app.utils.UpdateStatus.ChecksumFailed -> androidx.compose.material.icons.Icons.Default.Warning
                        else -> androidx.compose.material.icons.Icons.Default.SystemUpdate
                    },
                    isLoading = updateStatus is com.neocalc.app.utils.UpdateStatus.Loading || updateStatus is com.neocalc.app.utils.UpdateStatus.Downloading,
                    title = "Updates",
                    subtitle = when (val s = updateStatus) {
                        is com.neocalc.app.utils.UpdateStatus.Idle -> "Check for updates"
                        is com.neocalc.app.utils.UpdateStatus.Loading -> "Checking..."
                        is com.neocalc.app.utils.UpdateStatus.Available -> "Tap to download ${s.version}"
                        is com.neocalc.app.utils.UpdateStatus.Downloading ->
                            if (s.progress < 0) "Downloading..." else "Downloading... ${s.progress}%"
                        is com.neocalc.app.utils.UpdateStatus.Downloaded -> "Tap to install ${s.version}"
                        is com.neocalc.app.utils.UpdateStatus.UpToDate -> "Up to date"
                        is com.neocalc.app.utils.UpdateStatus.Error -> s.message
                        is com.neocalc.app.utils.UpdateStatus.ChecksumFailed -> "Security check failed! Download aborted."
                    },
                    iconTint = when(updateStatus) {
                         is com.neocalc.app.utils.UpdateStatus.Available -> androidx.compose.material3.MaterialTheme.colorScheme.primary
                         is com.neocalc.app.utils.UpdateStatus.Downloaded -> androidx.compose.material3.MaterialTheme.colorScheme.primary
                         is com.neocalc.app.utils.UpdateStatus.UpToDate -> androidx.compose.material3.MaterialTheme.colorScheme.tertiary
                         is com.neocalc.app.utils.UpdateStatus.Error -> androidx.compose.material3.MaterialTheme.colorScheme.error
                         is com.neocalc.app.utils.UpdateStatus.ChecksumFailed -> androidx.compose.material3.MaterialTheme.colorScheme.error
                         else -> androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    onClick = {
                        when (val s = updateStatus) {
                             is com.neocalc.app.utils.UpdateStatus.Available -> {
                                 // Start download
                                 scope.launch {
                                     updateStatus = com.neocalc.app.utils.UpdateStatus.Downloading(-1, s.version)
                                     updateStatus = com.neocalc.app.utils.UpdateManager.downloadApk(
                                         context = context,
                                         downloadUrl = s.downloadUrl,
                                         version = s.version,
                                         expectedChecksum = s.checksum,
                                         onProgress = { progress ->
                                             // Late-posted callbacks must not overwrite the final status
                                             if (updateStatus is com.neocalc.app.utils.UpdateStatus.Downloading) {
                                                 updateStatus = com.neocalc.app.utils.UpdateStatus.Downloading(progress, s.version)
                                             }
                                         }
                                     )
                                 }
                             }
                             is com.neocalc.app.utils.UpdateStatus.Downloaded -> {
                                 // Install APK
                                 com.neocalc.app.utils.UpdateManager.installApk(context, s.apkFile)
                             }
                             is com.neocalc.app.utils.UpdateStatus.Downloading -> {
                                 // Already downloading, do nothing
                             }
                             else -> {
                                 updateStatus = com.neocalc.app.utils.UpdateStatus.Loading
                                 scope.launch {
                                    updateStatus = com.neocalc.app.utils.UpdateManager.checkForUpdates(com.neocalc.app.BuildConfig.VERSION_NAME)
                                 }
                             }
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                // 2. GitHub Row
                AboutActionRow(
                    icon = androidx.compose.material.icons.Icons.Default.Code, // Or a GitHub-like path if available, Code is good generic
                    title = "Source Code",
                    subtitle = "github.com/niltonperimneto/neocalc",
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/niltonperimneto/neocalc-android"))
                        context.startActivity(intent)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 3. License Row
                AboutActionRow(
                    icon = androidx.compose.material.icons.Icons.Default.Description,
                    title = "License",
                    subtitle = "GNU General Public License v3.0",
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.gnu.org/licenses/gpl-3.0.html"))
                        context.startActivity(intent)
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Footer
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    Text(
                        text = "Developed by Nilton Perim Neto",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "© 2025",
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f)
                    )
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

@Composable
fun AboutActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isLoading: Boolean = false,
    title: String,
    subtitle: String,
    iconTint: androidx.compose.ui.graphics.Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        color = androidx.compose.ui.graphics.Color.Transparent,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
    ) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            // Icon Slot
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )
                } else if (icon != null) {
                    androidx.compose.material3.Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (!isLoading) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.OpenInNew, // Or ChevronRight
                    contentDescription = null,
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.outline.copy(alpha=0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
